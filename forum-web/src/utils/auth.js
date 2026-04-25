const TOKEN_KEY = 'forum_token'
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function parseTokenPayload(token) {
  if (!token) return null

  try {
    const payload = token.split('.')[1]
    if (!payload) return null

    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=')
    return JSON.parse(window.atob(padded))
  } catch (error) {
    return null
  }
}

export function isTokenExpiringSoon(token, thresholdMs = TOKEN_REFRESH_THRESHOLD) {
  const payload = parseTokenPayload(token)
  if (!payload?.exp) {
    return true
  }

  return payload.exp * 1000 - Date.now() <= thresholdMs
}
