export interface UserProfile {
  id: string
  fullName: string
  email: string
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  user: UserProfile
}

export interface ApiErrorBody {
  timestamp?: string
  status?: number
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}
