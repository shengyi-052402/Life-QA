import request from '@/utils/request'

export function getCommentPage(params) {
  return request({
    url: '/comments',
    method: 'get',
    params
  })
}

export function createComment(data) {
  return request({
    url: '/comments',
    method: 'post',
    data
  })
}

export function deleteComment(id) {
  return request({
    url: `/comments/${id}`,
    method: 'delete'
  })
}

export function getCommentLocation(id, params) {
  return request({
    url: `/comments/${id}/location`,
    method: 'get',
    params
  })
}

// ---- 以下为 interaction.js 也能放这，稍微合并一下 ----

export function togglePostLike(postId) {
  return request({
    url: `/interactions/posts/${postId}/like`,
    method: 'post'
  })
}

export function toggleCommentLike(commentId) {
  return request({
    url: `/interactions/comments/${commentId}/like`,
    method: 'post'
  })
}

export function togglePostFavorite(postId) {
  return request({
    url: `/interactions/posts/${postId}/favorite`,
    method: 'post'
  })
}
