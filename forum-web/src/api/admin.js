import request from '@/utils/request'

export function getAdminStats() {
  return request({
    url: '/admin/stats',
    method: 'get'
  })
}

export function getAdminUsers(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function updateAdminUser(id, data) {
  return request({
    url: `/admin/users/${id}`,
    method: 'put',
    data
  })
}

export function getAdminPosts(params) {
  return request({
    url: '/admin/posts',
    method: 'get',
    params
  })
}

export function updateAdminPost(id, data) {
  return request({
    url: `/admin/posts/${id}`,
    method: 'put',
    data
  })
}

export function deleteAdminPost(id) {
  return request({
    url: `/admin/posts/${id}`,
    method: 'delete'
  })
}

export function getAdminComments(params) {
  return request({
    url: '/admin/comments',
    method: 'get',
    params
  })
}

export function deleteAdminComment(id) {
  return request({
    url: `/admin/comments/${id}`,
    method: 'delete'
  })
}

export function createAdminCategory(data) {
  return request({
    url: '/admin/categories',
    method: 'post',
    data
  })
}

export function updateAdminCategory(id, data) {
  return request({
    url: `/admin/categories/${id}`,
    method: 'put',
    data
  })
}

export function deleteAdminCategory(id) {
  return request({
    url: `/admin/categories/${id}`,
    method: 'delete'
  })
}

export function reindexSearch() {
  return request({
    url: '/admin/search/reindex',
    method: 'post'
  })
}
