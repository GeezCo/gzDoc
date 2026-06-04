import { Navigate, useLocation } from 'react-router-dom'
import { isAuthenticated } from '@/utils/token'
import { ROUTE_PATH } from '@/constants/auth'

interface ProtectedRouteProps {
  children: React.ReactNode
}

/**
 * 路由守卫组件
 * 未登录用户自动重定向到登录页，并记录原目标路径
 */
const ProtectedRoute = ({ children }: ProtectedRouteProps) => {
  const location = useLocation()

  if (!isAuthenticated()) {
    // 未登录，重定向到登录页，并保存当前路径
    return <Navigate to={ROUTE_PATH.LOGIN} state={{ from: location }} replace />
  }

  return <>{children}</>
}

export default ProtectedRoute
