import { useState, type FormEvent } from 'react'
import axios from 'axios'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { ApiErrorBody, UserProfile } from '../types'

export function ProfilePage() {
  const { user, updateUser } = useAuth()
  const [fullName, setFullName] = useState(user?.fullName || '')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setMessage(''); setError(''); setBusy(true)
    try { const { data } = await api.put<UserProfile>('/api/users/me', { fullName }); updateUser(data); setMessage('Your profile has been updated.') }
    catch (err) { setError(axios.isAxiosError<ApiErrorBody>(err) ? err.response?.data.message || 'Could not reach the server. Please try again.' : 'Profile update failed.') }
    finally { setBusy(false) }
  }
  return <div className="page-content profile-page"><div className="page-heading"><div><div className="eyebrow">ACCOUNT SETTINGS</div><h1>Your <span className="accent">profile.</span></h1><p>Keep your personal details up to date.</p></div></div>
    <section className="profile-card"><div className="profile-banner"><div className="profile-avatar">{user?.fullName.slice(0, 1).toUpperCase()}</div><div><h2>{user?.fullName}</h2><p>{user?.email}</p></div><span className="verified-badge"><i /> ACTIVE</span></div>
      <form className="profile-form" onSubmit={submit}><div className="section-title"><div><h2>Personal details</h2><p>Update the name shown in your workspace.</p></div></div>
        <label>Full name<input value={fullName} onChange={e => setFullName(e.target.value)} required maxLength={120} /></label>
        <label>Email address<input value={user?.email || ''} readOnly /><span className="field-hint">Email changes aren’t available yet.</span></label>
        {error && <div className="form-error" role="alert">{error}</div>}{message && <div className="form-success" role="status">{message}</div>}
        <button className="primary-button save-button" type="submit" disabled={busy || fullName.trim() === user?.fullName}>{busy ? <><span className="spinner small" /> Saving…</> : <>Save changes <span>→</span></>}</button>
      </form>
    </section>
  </div>
}
