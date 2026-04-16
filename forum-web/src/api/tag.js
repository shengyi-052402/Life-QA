import request from '@/utils/request'

export function getTags(params) {
  return request({
    url: '/tags',
    method: 'get',
    params
  })
}

export function getHotTags(limit) {
  return request({
    url: '/tags/hot',
    method: 'get',
    params: { limit }
  })
}
