import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, isTokenExpiringSoon, removeToken, setToken } from './auth'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api', // Vite proxy 将会拦截并在开发环境转发到后端
  timeout: 10000
})

let refreshPromise = null

async function ensureFreshToken() {
  const token = getToken()
  if (!token) return null
  if (!isTokenExpiringSoon(token)) return token

  if (!refreshPromise) {
    refreshPromise = axios({
      url: '/api/auth/refresh',
      method: 'post',
      headers: {
        Authorization: `Bearer ${token}`
      },
      timeout: 10000
    })
      .then(response => {
        const res = response.data
        if (res.code !== 200 || !res.data?.token) {
          throw new Error(res.message || '刷新 Token 失败')
        }
        setToken(res.data.token)
        return res.data.token
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

// 请求拦截器
service.interceptors.request.use(
  async config => {
    const isRefreshRequest = config.url === '/auth/refresh'
    const token = isRefreshRequest ? getToken() : await ensureFreshToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 如果 code 不是 200，则判断为错误
    if (res.code !== 200) {
      ElMessage({
        message: res.message || 'Error',
        type: 'error',
        duration: 5 * 1000
      })
      
      // 401: Token 过期或未登录
      if (res.code === 401) {
        removeToken()
        router.push(`/login?redirect=${router.currentRoute.value.fullPath}`)
      }
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res
    }
  },
  error => {
    console.log('err' + error)
    // 处理 HTTP 状态码错误
    let message = error.message
    if (error.response && error.response.status === 401) {
      message = '认证失败，请重新登录'
      removeToken()
      router.push(`/login?redirect=${router.currentRoute.value.fullPath}`)
    }
    ElMessage({
      message: message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
