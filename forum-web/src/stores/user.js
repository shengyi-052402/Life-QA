import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getCurrentUserInfo, refreshToken as refreshTokenApi, updateCurrentUserInfo, updateCurrentUserPassword } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { useNotificationStore } from '@/stores/notification'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref(null)

  function setTokenState(newToken) {
    token.value = newToken
    setToken(newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
  }

  function logout() {
    const notificationStore = useNotificationStore()
    token.value = null
    userInfo.value = null
    removeToken()
    notificationStore.clear()
  }

  // 登录并存储 token
  async function login(loginForm) {
    const res = await loginApi(loginForm)
    if (res.data && res.data.token) {
      setTokenState(res.data.token)
      setUserInfo(res.data)
      return true
    }
    return false
  }

  // 获取当前用户信息
  async function fetchUserInfo() {
    if (!token.value) return null
    try {
      const res = await getCurrentUserInfo()
      setUserInfo(res.data)
      return res.data
    } catch (error) {
      logout()
      throw error
    }
  }

  async function updateProfile(profileForm) {
    await updateCurrentUserInfo(profileForm)
    return fetchUserInfo()
  }

  async function changePassword(passwordForm) {
    return updateCurrentUserPassword(passwordForm)
  }

  async function refreshToken() {
    if (!token.value) return null

    const res = await refreshTokenApi()
    if (res.data?.token) {
      setTokenState(res.data.token)
      if (userInfo.value) {
        userInfo.value = {
          ...userInfo.value,
          token: res.data.token
        }
      }
    }
    return res.data
  }

  return {
    token,
    userInfo,
    login,
    logout,
    fetchUserInfo,
    updateProfile,
    changePassword,
    setUserInfo,
    refreshToken,
    setTokenState
  }
})
