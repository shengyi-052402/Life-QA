import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getCurrentUserInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

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
    token.value = null
    userInfo.value = null
    removeToken()
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

  return {
    token,
    userInfo,
    login,
    logout,
    fetchUserInfo
  }
})
