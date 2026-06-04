/**
 * Token 管理工具
 * 统一管理 token 和用户信息的存储
 */

import { STORAGE_KEY } from '@/constants/auth'
import type { User } from '@/types'

/**
 * 获取 Access Token
 */
export const getAccessToken = (): string | null => {
  return localStorage.getItem(STORAGE_KEY.ACCESS_TOKEN)
}

/**
 * 设置 Access Token
 */
export const setAccessToken = (token: string): void => {
  localStorage.setItem(STORAGE_KEY.ACCESS_TOKEN, token)
}

/**
 * 获取 Refresh Token
 */
export const getRefreshToken = (): string | null => {
  return localStorage.getItem(STORAGE_KEY.REFRESH_TOKEN)
}

/**
 * 设置 Refresh Token
 */
export const setRefreshToken = (token: string): void => {
  localStorage.setItem(STORAGE_KEY.REFRESH_TOKEN, token)
}

/**
 * 获取用户信息
 */
export const getUserInfo = (): User | null => {
  const userInfoStr = localStorage.getItem(STORAGE_KEY.USER_INFO)
  if (!userInfoStr) return null

  try {
    return JSON.parse(userInfoStr) as User
  } catch {
    return null
  }
}

/**
 * 设置用户信息
 */
export const setUserInfo = (userInfo: User): void => {
  localStorage.setItem(STORAGE_KEY.USER_INFO, JSON.stringify(userInfo))
  // 同时保存 tenantId 和 userId 以便请求头使用
  localStorage.setItem(STORAGE_KEY.TENANT_ID, userInfo.tenantId.toString())
  localStorage.setItem(STORAGE_KEY.USER_ID, userInfo.id.toString())
}

/**
 * 获取租户 ID
 */
export const getTenantId = (): string | null => {
  return localStorage.getItem(STORAGE_KEY.TENANT_ID)
}

/**
 * 获取用户 ID
 */
export const getUserId = (): string | null => {
  return localStorage.getItem(STORAGE_KEY.USER_ID)
}

/**
 * 清除所有认证数据
 */
export const clearAuthData = (): void => {
  localStorage.removeItem(STORAGE_KEY.ACCESS_TOKEN)
  localStorage.removeItem(STORAGE_KEY.REFRESH_TOKEN)
  localStorage.removeItem(STORAGE_KEY.USER_INFO)
  localStorage.removeItem(STORAGE_KEY.TENANT_ID)
  localStorage.removeItem(STORAGE_KEY.USER_ID)
}

/**
 * 检查是否已登录
 */
export const isAuthenticated = (): boolean => {
  return !!getAccessToken()
}
