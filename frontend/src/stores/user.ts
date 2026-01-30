import { defineStore } from 'pinia'
import { ref } from 'vue'
import { type LoginParams, type RegisterParams, type UserInfo, login, register, guestLogin, wechatLogin, logout } from '../api/auth'
import { getMenus, type MenuItem } from '../api/menu'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  // 从 localStorage 恢复 userInfo，防止刷新丢失
  const userInfo = ref<UserInfo | null>(
    localStorage.getItem('userInfo') 
      ? JSON.parse(localStorage.getItem('userInfo')!) 
      : null
  )
  const menus = ref<MenuItem[]>([])
  // 新用户标记
  const isNewUser = ref(false)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  // 预约状态管理
  const reservation = ref<{
    seatId: number
    seatNo: string
    deadline: number // 截止时间戳
    status: 'reserved' | 'checked_in' | 'away'
  } | null>(
    localStorage.getItem('reservation') 
      ? JSON.parse(localStorage.getItem('reservation')!) 
      : null
  )

  function setReservation(seatId: number, seatNo: string) {
    const deadline = Date.now() + 15 * 60 * 1000 // 15分钟后截止
    const data = { seatId, seatNo, deadline, status: 'reserved' as const }
    reservation.value = data
    localStorage.setItem('reservation', JSON.stringify(data))
  }

  function setAway() {
    if (!reservation.value) return
    const deadline = Date.now() + 30 * 60 * 1000 // 30分钟暂离时间
    const data = { ...reservation.value, deadline, status: 'away' as const }
    reservation.value = data
    localStorage.setItem('reservation', JSON.stringify(data))
  }

  function checkIn() {
    if (!reservation.value) return
    const data = { ...reservation.value, status: 'checked_in' as const }
    reservation.value = data
    localStorage.setItem('reservation', JSON.stringify(data))
  }

  function clearReservation() {
    reservation.value = null
    localStorage.removeItem('reservation')
  }

  function clearToken() {
    token.value = ''
    userInfo.value = null
    menus.value = []
    isNewUser.value = false
    clearReservation() // 登出时清除预约
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function handleLogin(params: LoginParams) {
    try {
      const res = await login(params)
      
      // 模拟新用户逻辑: 如果用户名包含 'new'，则视为新用户
      const isNew = params.username.includes('new')
      isNewUser.value = isNew
      
      setToken(res.token)
      setUserInfo(res.userInfo)
      
      // 登录成功后获取菜单
      await fetchMenus()
      
      return true
    } catch (error) {
      console.error(error)
      return false
    }
  }

  async function handleRegister(params: RegisterParams) {
    try {
      const res = await register(params)
      
      // 注册用户不标记为新用户，直接进入首页
      isNewUser.value = false
      
      setToken(res.token)
      setUserInfo(res.userInfo)
      
      await fetchMenus()
      
      return true
    } catch (error) {
      console.error(error)
      return false
    }
  }

  async function handleGuestLogin() {
    try {
      const res = await guestLogin()
      
      // 访客直接进入首页，不是新用户
      isNewUser.value = false
      
      setToken(res.token)
      setUserInfo(res.userInfo)
      
      await fetchMenus()
      
      return true
    } catch (error) {
      console.error(error)
      return false
    }
  }

  // 微信登录模拟
  async function handleWeChatLogin(code: string) {
    try {
      const res = await wechatLogin(code)
      
      const isNew = code === 'new_user_code' || Math.random() > 0.5
      isNewUser.value = isNew

      setToken(res.token)
      setUserInfo(res.userInfo)
      await fetchMenus()
      
      return true
    } catch (error) {
      return false
    }
  }

  // 完成绑定
  function completeBinding() {
    isNewUser.value = false
  }

  async function handleLogout() {
    try {
      await logout()
    } finally {
      clearToken()
    }
  }

  async function fetchMenus() {
    if (!userInfo.value?.role) return
    try {
      const res = await getMenus(userInfo.value.role)
      menus.value = res
    } catch (error) {
      console.error('Failed to fetch menus', error)
    }
  }

  return {
    // ... (exports remain the same)
    token,
    userInfo,
    menus,
    isNewUser,
    reservation,
    setReservation,
    setAway,
    checkIn,
    clearReservation,
    clearToken,
    handleLogin,
    handleRegister,
    handleGuestLogin,
    handleWeChatLogin,
    completeBinding,
    handleLogout,
    fetchMenus
  }
})
