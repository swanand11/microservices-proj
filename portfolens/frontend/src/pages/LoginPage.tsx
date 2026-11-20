import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { AuthShell } from '../components/AuthShell'
import { useAuth } from '../auth/AuthContext'
import type { ApiErrorBody } from '../types'

export function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  if (isAuthenticated) return <Navigate to="/dashboard" replace />
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError(''); setBusy(true)
    try { await login(email, password); navigate(location.state?.from?.pathname || '/dashboard', { replace: true }) }
    catch (err) { setError(axios.isAxiosError<ApiErrorBody>(err) ? err.response?.data.message || 'Could not reach the server. Please try again.' : 'Sign in failed.') }
    finally { setBusy(false) }
  }
  return <AuthShell mode="login"><form className="auth-form" onSubmit={submit}>
    <label>Email address<input type="email" autoComplete="email" placeholder="you@example.com" value={email} onChange={e => setEmail(e.target.value)} required maxLength={254} /></label>
    <label>Password<div className="password-wrap"><input type="password" autoComplete="current-password" placeholder="Enter your password" value={password} onChange={e => setPassword(e.target.value)} required /><Link to="/register" className="forgot-link">Forgot?</Link></div></label>
    {error && <div className="form-error" role="alert">{error}</div>}
    <button className="primary-button" type="submit" disabled={busy}>{busy ? <><span className="spinner small" /> Signing in…</> : <>Sign in <span>→</span></>}</button>
    <div className="secure-note"><span>▣</span> Your account is secured with encrypted authentication</div>
  </form></AuthShell>
}
