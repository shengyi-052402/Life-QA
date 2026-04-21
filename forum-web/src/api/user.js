import request from '@/utils/request'

export function getUserProfile(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export function getUserPosts(id, params) {
  return request({
    url: `/users/${id}/posts`,
    method: 'get',
    params
  })
}

export function getMyFavorites(params) {
  return request({
    url: '/users/me/favorites',
    method: 'get',
    params
  })
}

export function getUserActivities(id, params) {
  return request({
    url: `/users/${id}/activities`,
    method: 'get',
    params
  })
}
