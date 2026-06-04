import { Outlet, useNavigate } from 'react-router-dom'
import { Layout as AntLayout, Menu, Avatar, Dropdown, message } from 'antd'
import {
  DashboardOutlined,
  FileTextOutlined,
  QuestionCircleOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { authApi } from '@/services/auth'
import { getUserInfo, clearAuthData } from '@/utils/token'
import { SUCCESS_MESSAGE, ROUTE_PATH } from '@/constants/auth'
import './Layout.css'

const { Header, Sider, Content } = AntLayout

const Layout = () => {
  const navigate = useNavigate()
  const userInfo = getUserInfo()

  const handleLogout = async () => {
    try {
      await authApi.logout()
      message.success(SUCCESS_MESSAGE.LOGOUT_SUCCESS)
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      clearAuthData()
      navigate(ROUTE_PATH.LOGIN, { replace: true })
    }
  }

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
      onClick: () => navigate('/dashboard'),
    },
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: '文档管理',
      onClick: () => navigate('/documents'),
    },
    {
      key: '/qa',
      icon: <QuestionCircleOutlined />,
      label: '智能问答',
      onClick: () => navigate('/qa'),
    },
  ]

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Header className="layout-header">
        <div className="logo">GzDoc</div>
        <div className="user-info">
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar icon={<UserOutlined />} src={userInfo?.avatar} />
              <span style={{ color: '#fff' }}>{userInfo?.nickname || userInfo?.username}</span>
            </div>
          </Dropdown>
        </div>
      </Header>
      <AntLayout>
        <Sider width={200} theme="light">
          <Menu
            mode="inline"
            defaultSelectedKeys={[window.location.pathname]}
            style={{ height: '100%', borderRight: 0 }}
            items={menuItems}
          />
        </Sider>
        <Content className="layout-content">
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  )
}

export default Layout
