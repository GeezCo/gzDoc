import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'antd'
import type { ApiResponse } from '@/types'
import {
  getAccessToken,
  getRefreshToken,
  getTenantId,
  getUserId,
  setAccessToken,
  setRefreshToken,
  clearAuthData
} from '@/utils/token'
import { AUTH_API, ERROR_MESSAGE, ROUTE_PATH } from '@/constants/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

// Token 刷新相关状态
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: any) => void
}> = []

/**
 * 处理队列中的请求
 */
const processQueue = (error: any = null, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(token!)
    }
  })
  failedQueue = []
}

/**
 * 刷新 Token
 */
const refreshAccessToken = async (): Promise<string> => {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('No refresh token available')
  }

  const response = await axios.post<ApiResponse>(`/api${AUTH_API.REFRESH}`, {
    refreshToken,
  })

  const { code, data } = response.data
  if (code === 200 && data) {
    setAccessToken(data.accessToken)
    if (data.refreshToken) {
      setRefreshToken(data.refreshToken)
    }
    return data.accessToken
  }

  throw new Error('Token refresh failed')
}

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    const tenantId = getTenantId()
    const userId = getUserId()

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId
    }
    if (userId) {
      config.headers['X-User-Id'] = userId
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message: msg, data } = response.data

    if (code === 200) {
      return data
    } else {
      message.error(msg || '请求失败')
      return Promise.reject(new Error(msg || '请求失败'))
    }
  },
  async (error: AxiosError<ApiResponse>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response) {
      const { status, data } = error.response

      // 401 未授权，尝试刷新 Token
      if (status === 401 && originalRequest && !originalRequest._retry) {
        if (isRefreshing) {
          // 正在刷新 Token，将请求加入队列
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject })
          })
            .then((token) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              return axios(originalRequest)
            })
            .catch((err) => {
              return Promise.reject(err)
            })
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
          const newToken = await refreshAccessToken()
          processQueue(null, newToken)

          // 使用新 Token 重试原请求
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return axios(originalRequest)
        } catch (refreshError) {
          // Token 刷新失败，清除认证信息并跳转登录页
          processQueue(refreshError, null)
          clearAuthData()
          message.error(ERROR_MESSAGE.TOKEN_REFRESH_FAILED)
          window.location.href = ROUTE_PATH.LOGIN
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      } else if (status === 403) {
        message.error(ERROR_MESSAGE.FORBIDDEN)
      } else if (status === 404) {
        message.error('请求的资源不存在')
      } else if (status === 500) {
        message.error(data?.message || ERROR_MESSAGE.SERVER_ERROR)
      } else {
        message.error(data?.message || '请求失败')
      }
    } else if (error.request) {
      message.error(ERROR_MESSAGE.NETWORK_ERROR)
    } else {
      message.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default request
