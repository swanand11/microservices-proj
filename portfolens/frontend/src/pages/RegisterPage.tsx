import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { AuthShell } from '../components/AuthShell'
import { useAuth } from '../auth/AuthContext'
import type { ApiErrorBody } from '../types'

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  if (isAuthenticated) return <Navigate to="/dashboard" replace />
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError('')
    if (password !== confirm) { setError('Passwords do not match.'); return }
    if (password.length < 8) { setError('Use at least 8 characters for your password.'); return }
    setBusy(true)
    try { await register(fullName.trim(), email.trim().toLowerCase(), password); navigate('/dashboard', { replace: true }) }
    catch (err) { setError(axios.isAxiosError<ApiErrorBody>(err) ? err.response?.data.message || 'Could not reach the server. Please try again.' : 'Registration failed.') }
    finally { setBusy(false) }
  }
  return <AuthShell mode="register"><form className="auth-form" onSubmit={submit}>
    <label>Full name<input autoComplete="name" placeholder="e.g. Aditi Sharma" value={fullName} onChange={e => setFullName(e.target.value)} required maxLength={120} /></label>
    <label>Email address<input type="email" autoComplete="email" placeholder="you@example.com" value={email} onChange={e => setEmail(e.target.value)} required maxLength={254} /></label>
    <label>Password<input type="password" autoComplete="new-password" placeholder="At least 8 characters" value={password} onChange={e => setPassword(e.target.value)} required minLength={8} maxLength={72} /></label>
    <label>Confirm password<input type="password" autoComplete="new-password" placeholder="Enter password again" value={confirm} onChange={e => setConfirm(e.target.value)} required /></label>
    {error && <div className="form-error" role="alert">{error}</div>}
    <button className="primary-button" type="submit" disabled={busy}>{busy ? <><span className="spinner small" /> Creating account…</> : <>Create account <span>→</span></>}</button>
    <div className="secure-note"><span>▣</span> Passwords are securely hashed and never stored in plain text</div>
  </form></AuthShell>
}
