import request from '@/utils/request'
import type { LoginRequest, LoginResponse } from '@/types'

export const authApi = {
  // 登录
  login: (data: LoginRequest) => {
    return request.post<any, LoginResponse>('/auth/login', data)
  },

  // 登出
  logout: () => {
    return request.post('/auth/logout')
  },

  // 验证token
  validateToken: (token: string) => {
    return request.get<any, boolean>('/auth/validate', { params: { token } })
  },
}
