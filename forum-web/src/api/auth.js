import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function getCurrentUserInfo() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

export function updateCurrentUserInfo(data) {
  return request({
    url: '/auth/me',
    method: 'put',
    data
  })
}

export function updateCurrentUserPassword(data) {
  return request({
    url: '/auth/me/password',
    method: 'put',
    data
  })
}

export function refreshToken() {
  return request({
    url: '/auth/refresh',
    method: 'post'
  })
}
