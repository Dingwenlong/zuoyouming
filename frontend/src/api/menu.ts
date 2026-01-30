
export interface MenuItem {
  id: number
  parentId: number | null
  name: string
  path: string
  icon?: string
  component?: string
  redirect?: string
  meta?: {
    title: string
    icon?: string
    keepAlive?: boolean
    requiresAuth?: boolean
    roles?: string[]
  }
  children?: MenuItem[]
}

// 模拟后端数据库中的完整菜单列表
const allMenus: MenuItem[] = [
  {
    id: 1,
    parentId: null,
    name: 'Dashboard',
    path: '/dashboard',
    meta: { title: '系统首页', icon: 'DashboardOutlined', roles: ['student', 'librarian', 'admin', 'guest'] }
  },
  {
    id: 2,
    parentId: null,
    name: 'Seat',
    path: '/seat',
    meta: { title: '座位预约', icon: 'DesktopOutlined', roles: ['student', 'librarian', 'admin', 'guest'] }
  },
  {
    id: 6,
    parentId: null,
    name: 'CheckIn',
    path: '/checkin',
    meta: { title: '座位签到', icon: 'EnvironmentOutlined', roles: ['student', 'librarian', 'admin'] }
  },
  {
    id: 4,
    parentId: null,
    name: 'Stats',
    path: '/stats',
    meta: { title: '数据统计', icon: 'BarChartOutlined', roles: ['librarian', 'admin'] }
  },
  {
    id: 3,
    parentId: null,
    name: 'Profile',
    path: '/profile',
    meta: { title: '个人中心', icon: 'UserOutlined', roles: ['student', 'librarian', 'admin'] },
    children: [
      {
        id: 31,
        parentId: 3,
        name: 'ProfileInfo',
        path: '/profile/info',
        meta: { title: '个人信息', roles: ['student', 'librarian', 'admin'] }
      },
      {
        id: 32,
        parentId: 3,
        name: 'ProfileHistory',
        path: '/profile/history',
        meta: { title: '预约/签到记录', roles: ['student', 'librarian', 'admin'] }
      }
    ]
  },
  {
    id: 5,
    parentId: null,
    name: 'System',
    path: '/system',
    meta: { title: '系统管理', icon: 'SettingOutlined', roles: ['admin', 'librarian'] },
    children: [
      {
        id: 51,
        parentId: 5,
        name: 'SystemUser',
        path: '/system/user',
        meta: { title: '用户管理', roles: ['admin'] }
      },
      {
        id: 52,
        parentId: 5,
        name: 'SystemSeat',
        path: '/system/seat',
        meta: { title: '座位管理', roles: ['admin', 'librarian'] }
      },
      {
        id: 53,
        parentId: 5,
        name: 'SystemLog',
        path: '/system/log',
        meta: { title: '系统日志', roles: ['admin'] }
      }
    ]
  }
]

// 模拟后端根据角色过滤菜单
export const getMenus = (role: string): Promise<MenuItem[]> => {
  return new Promise((resolve) => {
    // 简单的递归过滤函数
    const filterMenus = (menus: MenuItem[]): MenuItem[] => {
      return menus
        .filter(menu => {
          // 如果没有定义 roles，默认所有角色可见
          if (!menu.meta?.roles || menu.meta.roles.length === 0) return true
          return menu.meta.roles.includes(role)
        })
        .map(menu => {
          if (menu.children) {
            return { ...menu, children: filterMenus(menu.children) }
          }
          return menu
        })
    }

    const userMenus = filterMenus(allMenus)
    
    setTimeout(() => {
      resolve(userMenus)
    }, 300)
  })
}
