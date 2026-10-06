import request from '@/utils/request'

export function getPostPage(params) {
  return request({
    url: '/posts',
    method: 'get',
    params
  })
}

export function getRecommendedPosts(params) {
  return request({
    url: '/posts/recommend',
    method: 'get',
    params
  })
}

export function getPostDetail(id) {
  return request({
    url: `/posts/${id}`,
    method: 'get'
  })
}

export function getPostEditDetail(id) {
  return request({
    url: `/posts/${id}/edit`,
    method: 'get'
  })
}

export function createPost(data) {
  return request({
    url: '/posts',
    method: 'post',
    data
  })
}

export function updatePost(id, data) {
  return request({
    url: `/posts/${id}`,
    method: 'put',
    data
  })
}

export function deletePost(id) {
  return request({
    url: `/posts/${id}`,
    method: 'delete'
  })
}
export function getGlobePosts() {
  return request({
    url: '/posts/globe',
    method: 'get'
  })
}
