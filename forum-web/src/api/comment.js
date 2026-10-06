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

async function setInteractionState(url, active) {
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      return await request({ url, method: 'put', data: { active }, silentNetworkError: attempt === 0 })
    } catch (error) {
      const retryable = ['ECONNABORTED', 'ETIMEDOUT', 'ERR_NETWORK'].includes(error.code)
      if (!retryable || attempt === 1) throw error
      // Retry the exact desired state, never toggle after an ambiguous timeout.
    }
  }
}

export function setPostLike(postId, active) {
  return setInteractionState(`/interactions/posts/${postId}/like`, active)
}

export function setCommentLike(commentId, active) {
  return setInteractionState(`/interactions/comments/${commentId}/like`, active)
}

export function setPostFavorite(postId, active) {
  return setInteractionState(`/interactions/posts/${postId}/favorite`, active)
}
