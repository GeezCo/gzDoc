import request from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse, RefreshTokenRequest, RefreshTokenResponse } from '@/types'
import { AUTH_API } from '@/constants/auth'

export const authApi = {
  // 登录
  login: (data: LoginRequest) => {
    return request.post<any, LoginResponse>(AUTH_API.LOGIN, data)
  },

  // 注册
  register: (data: RegisterRequest) => {
    return request.post<any, RegisterResponse>(AUTH_API.REGISTER, data)
  },

  // 登出
  logout: () => {
    return request.post(AUTH_API.LOGOUT)
  },

  // 刷新 Token
  refreshToken: (data: RefreshTokenRequest) => {
    return request.post<any, RefreshTokenResponse>(AUTH_API.REFRESH, data)
  },

  // 验证 Token
  validateToken: (token: string) => {
    return request.get<any, boolean>(AUTH_API.VALIDATE, { params: { token } })
  },
}
