import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message, Collapse } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined, TeamOutlined } from '@ant-design/icons'
import { authApi } from '@/services/auth'
import type { RegisterRequest } from '@/types'
import {
  ERROR_MESSAGE,
  SUCCESS_MESSAGE,
  VALIDATION_RULES,
  ROUTE_PATH,
} from '@/constants/auth'
import './Register.css'

const { Panel } = Collapse

const Register = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const onFinish = async (values: RegisterRequest) => {
    setLoading(true)
    try {
      await authApi.register(values)
      message.success(SUCCESS_MESSAGE.REGISTER_SUCCESS)

      // 注册成功后跳转到登录页
      setTimeout(() => {
        navigate(ROUTE_PATH.LOGIN, {
          state: { username: values.username }
        })
      }, 1500)
    } catch (error: any) {
      const errorMsg = error?.response?.data?.message || ERROR_MESSAGE.REGISTER_FAILED
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="register-container">
      <Card className="register-card" title="注册 GzDoc 账号">
        <Form
          form={form}
          name="register"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
          scrollToFirstError
        >
          <Form.Item
            name="username"
            rules={[
              { required: true, message: ERROR_MESSAGE.USERNAME_REQUIRED },
              {
                pattern: VALIDATION_RULES.USERNAME.PATTERN,
                message: ERROR_MESSAGE.INVALID_USERNAME,
              },
              {
                min: VALIDATION_RULES.USERNAME.MIN_LENGTH,
                max: VALIDATION_RULES.USERNAME.MAX_LENGTH,
                message: ERROR_MESSAGE.INVALID_USERNAME,
              },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名（3-20个字符，字母数字下划线）"
            />
          </Form.Item>

          <Form.Item
            name="email"
            rules={[
              { required: true, message: ERROR_MESSAGE.EMAIL_REQUIRED },
              {
                pattern: VALIDATION_RULES.EMAIL.PATTERN,
                message: ERROR_MESSAGE.INVALID_EMAIL,
              },
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="邮箱" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: ERROR_MESSAGE.PASSWORD_REQUIRED },
              {
                pattern: VALIDATION_RULES.PASSWORD.PATTERN,
                message: ERROR_MESSAGE.INVALID_PASSWORD,
              },
              {
                min: VALIDATION_RULES.PASSWORD.MIN_LENGTH,
                max: VALIDATION_RULES.PASSWORD.MAX_LENGTH,
                message: ERROR_MESSAGE.INVALID_PASSWORD,
              },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码（至少8位，含字母和数字）"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error(ERROR_MESSAGE.PASSWORD_MISMATCH))
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="确认密码"
            />
          </Form.Item>

          <Form.Item name="nickname">
            <Input prefix={<UserOutlined />} placeholder="昵称（可选）" />
          </Form.Item>

          <Collapse ghost>
            <Panel header="高级选项" key="1">
              <Form.Item name="tenantId">
                <Input
                  prefix={<TeamOutlined />}
                  placeholder="租户ID（可选，不填则使用默认租户）"
                  type="number"
                />
              </Form.Item>
            </Panel>
          </Collapse>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              注册
            </Button>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <div style={{ textAlign: 'center' }}>
              已有账号？<Link to={ROUTE_PATH.LOGIN}>立即登录</Link>
            </div>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Register
