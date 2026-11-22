import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000',
  headers: { 'Content-Type': 'application/json' },
})

let accessToken: string | null = null
export function getStoredAccessToken() {
  try { return window.sessionStorage.getItem('portfolens.accessToken') }
  catch { return null }
}
export function setAccessToken(token: string | null) {
  accessToken = token
  try {
    if (token) window.sessionStorage.setItem('portfolens.accessToken', token)
    else window.sessionStorage.removeItem('portfolens.accessToken')
  } catch { /* The in-memory session still works when storage is unavailable. */ }
}

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

api.interceptors.response.use((response) => response, (error: unknown) => {
  if (axios.isAxiosError(error) && error.response?.status === 401 && accessToken) {
    setAccessToken(null)
    window.dispatchEvent(new Event('portfolens:session-expired'))
  }
  return Promise.reject(error)
})
