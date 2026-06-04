import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message, Collapse } from 'antd'
import { UserOutlined, LockOutlined, TeamOutlined } from '@ant-design/icons'
import { authApi } from '@/services/auth'
import type { LoginRequest } from '@/types'
import {
  setAccessToken,
  setRefreshToken,
  setUserInfo,
} from '@/utils/token'
import {
  ERROR_MESSAGE,
  SUCCESS_MESSAGE,
  ROUTE_PATH,
} from '@/constants/auth'
import './Login.css'

const { Panel } = Collapse

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  // 获取注册页传来的用户名或跳转前的路径
  const state = location.state as { username?: string; from?: { pathname: string } }
  const redirectPath = state?.from?.pathname || ROUTE_PATH.DASHBOARD

  const onFinish = async (values: LoginRequest) => {
    setLoading(true)
    try {
      const response = await authApi.login(values)

      // 使用统一的 token 工具保存认证信息
      setAccessToken(response.accessToken)
      setRefreshToken(response.refreshToken)
      setUserInfo(response.userInfo)

      message.success(SUCCESS_MESSAGE.LOGIN_SUCCESS)

      // 跳转到原目标页面或首页
      navigate(redirectPath, { replace: true })
    } catch (error: any) {
      const errorMsg = error?.response?.data?.message || ERROR_MESSAGE.LOGIN_FAILED
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <Card className="login-card" title="GzDoc 智能文档处理平台">
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
          initialValues={{
            username: state?.username || '',
          }}
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: ERROR_MESSAGE.USERNAME_REQUIRED }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: ERROR_MESSAGE.PASSWORD_REQUIRED }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>

          <Collapse ghost>
            <Panel header="高级选项" key="1">
              <Form.Item name="tenantId">
                <Input
                  prefix={<TeamOutlined />}
                  placeholder="租户ID（可选）"
                  type="number"
                />
              </Form.Item>
            </Panel>
          </Collapse>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <div style={{ textAlign: 'center' }}>
              还没有账号？<Link to={ROUTE_PATH.REGISTER}>立即注册</Link>
            </div>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Login
