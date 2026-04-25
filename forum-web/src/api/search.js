import request from '@/utils/request'

export function searchPosts(params) {
  return request({
    url: '/search',
    method: 'get',
    params
  })
}

export function getSearchSuggestions(keyword) {
  return request({
    url: '/search/suggestions',
    method: 'get',
    params: { keyword }
  })
}
