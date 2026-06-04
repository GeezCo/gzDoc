/**
 * 认证相关常量统一管理
 * 修改一处，全局生效
 */

// API 路径常量
export const AUTH_API = {
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  LOGOUT: '/auth/logout',
  REFRESH: '/auth/refresh',
  VALIDATE: '/auth/validate',
} as const

// LocalStorage Key 常量
export const STORAGE_KEY = {
  ACCESS_TOKEN: 'gzdoc_access_token',
  REFRESH_TOKEN: 'gzdoc_refresh_token',
  USER_INFO: 'gzdoc_user_info',
  TENANT_ID: 'gzdoc_tenant_id',
  USER_ID: 'gzdoc_user_id',
} as const

// 路由路径常量
export const ROUTE_PATH = {
  LOGIN: '/login',
  REGISTER: '/register',
  HOME: '/',
  DASHBOARD: '/dashboard',
} as const

// 错误消息常量
export const ERROR_MESSAGE = {
  LOGIN_FAILED: '登录失败，请检查用户名和密码',
  REGISTER_FAILED: '注册失败，请稍后重试',
  TOKEN_EXPIRED: '登录已过期，请重新登录',
  TOKEN_REFRESH_FAILED: '会话已过期，请重新登录',
  UNAUTHORIZED: '未登录或登录已过期',
  FORBIDDEN: '没有权限访问该资源',
  NETWORK_ERROR: '网络错误，请检查网络连接',
  SERVER_ERROR: '服务器错误，请稍后重试',
  INVALID_USERNAME: '用户名只能包含字母、数字和下划线，长度3-20个字符',
  INVALID_PASSWORD: '密码长度至少8位，必须包含字母和数字',
  PASSWORD_MISMATCH: '两次输入的密码不一致',
  INVALID_EMAIL: '请输入有效的邮箱地址',
  USERNAME_REQUIRED: '请输入用户名',
  PASSWORD_REQUIRED: '请输入密码',
  EMAIL_REQUIRED: '请输入邮箱',
} as const

// 成功消息常量
export const SUCCESS_MESSAGE = {
  LOGIN_SUCCESS: '登录成功',
  REGISTER_SUCCESS: '注册成功，即将跳转到登录页',
  LOGOUT_SUCCESS: '已退出登录',
} as const

// Token 配置常量
export const TOKEN_CONFIG = {
  REFRESH_THRESHOLD: 5 * 60 * 1000, // 5分钟内过期时刷新
  MAX_RETRY: 1, // 最大重试次数
} as const

// 表单验证规则常量
export const VALIDATION_RULES = {
  USERNAME: {
    MIN_LENGTH: 3,
    MAX_LENGTH: 20,
    PATTERN: /^[a-zA-Z0-9_]+$/,
  },
  PASSWORD: {
    MIN_LENGTH: 8,
    MAX_LENGTH: 32,
    PATTERN: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]+$/,
  },
  EMAIL: {
    PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  },
} as const
