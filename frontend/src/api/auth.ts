import request from '../utils/request'

export interface LoginParams {
  username: string
  password?: string
}

export interface RegisterParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  role: string
  avatar?: string | null
  phone?: string | null
}

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// Mock implementation
const mockLogin = (data: LoginParams): Promise<LoginResult> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let role = 'student'
      if (data.username.includes('admin')) {
        role = 'admin'
      } else if (data.username.includes('lib')) {
        role = 'librarian'
      }
      
      resolve({
        token: 'mock-token-' + Date.now(),
        userInfo: {
          id: 1,
          username: data.username,
          role: role,
          avatar: 'https://api.dicebear.com/9.x/icons/svg?seed=' + data.username
        }
      })
    }, 500)
  })
}

const mockRegister = (data: RegisterParams): Promise<LoginResult> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        token: 'mock-token-' + Date.now(),
        userInfo: {
          id: Date.now(),
          username: data.username,
          role: 'student',
          avatar: 'https://api.dicebear.com/9.x/icons/svg?seed=' + data.username
        }
      })
    }, 500)
  })
}

export function login(data: LoginParams) {
  if (USE_MOCK) {
    return mockLogin(data)
  }
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function register(data: RegisterParams) {
  if (USE_MOCK) {
    return mockRegister(data)
  }
  return request<LoginResult>({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function guestLogin() {
  // 访客登录始终使用前端模拟数据，不调用后端接口
  return new Promise<LoginResult>((resolve) => {
    setTimeout(() => {
      resolve({
        token: 'guest-token-' + Date.now(),
        userInfo: {
          id: 0,
          username: 'guest',
          role: 'guest',
          avatar: 'https://api.dicebear.com/9.x/icons/svg?seed=guest'
        }
      })
    }, 500)
  })
}

export function getUserInfo() {
  if (USE_MOCK) {
    return Promise.resolve({
      id: 1,
      username: 'mockUser',
      role: 'student'
    })
  }
  return request<UserInfo>({
    url: '/auth/info',
    method: 'get'
  })
}

export function logout() {
  if (USE_MOCK) {
    return Promise.resolve()
  }
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function wechatLogin(code: string) {
  if (USE_MOCK) {
    return new Promise<LoginResult>((resolve) => {
      setTimeout(() => {
        resolve({
          token: 'wechat-token-' + Date.now(),
          userInfo: {
            id: 2,
            username: 'WeChatUser',
            role: 'student',
            avatar: 'https://api.dicebear.com/9.x/icons/svg?seed=WeChat'
          }
        })
      }, 500)
    })
  }
  return request<LoginResult>({
    url: '/auth/wechat-login',
    method: 'post',
    data: { code }
  })
}
