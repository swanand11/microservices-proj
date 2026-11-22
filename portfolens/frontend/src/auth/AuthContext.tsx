import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, getStoredAccessToken, setAccessToken } from '../api/client'
import type { AuthResponse, UserProfile } from '../types'

interface AuthContextValue {
  user: UserProfile | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>
  logout: () => void
  updateUser: (user: UserProfile) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  useEffect(() => {
    let current = true
    const onSessionExpired = () => setUser(null)
    window.addEventListener('portfolens:session-expired', onSessionExpired)
    const token = getStoredAccessToken()
    if (!token) {
      setIsLoading(false)
      return () => { current = false; window.removeEventListener('portfolens:session-expired', onSessionExpired) }
    }
    setAccessToken(token)
    api.get<UserProfile>('/api/users/me')
      .then(({ data }) => { if (current) setUser(data) })
      .catch(() => { setAccessToken(null); if (current) setUser(null) })
      .finally(() => { if (current) setIsLoading(false) })
    return () => { current = false; window.removeEventListener('portfolens:session-expired', onSessionExpired) }
  }, [])
  const login = useCallback(async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password })
    setAccessToken(data.accessToken)
    setUser(data.user)
  }, [])
  const register = useCallback(async (fullName: string, email: string, password: string) => {
    await api.post('/api/auth/register', { fullName, email, password })
    await login(email, password)
  }, [login])
  const logout = useCallback(() => { setAccessToken(null); setUser(null) }, [])
  const updateUser = useCallback((updated: UserProfile) => setUser(updated), [])
  const value = useMemo(() => ({ user, isAuthenticated: user !== null, isLoading, login, register, logout, updateUser }), [user, isLoading, login, register, logout, updateUser])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
