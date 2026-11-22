import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'

export function AuthShell({ children, mode }: { children: ReactNode; mode: 'login' | 'register' }) {
  return <main className="auth-page"><div className="auth-glow glow-one" /><div className="auth-glow glow-two" />
    <div className="auth-brand"><Link className="brand" to="/"><span className="brand-mark">P</span><span>portfo<span className="accent">lens</span></span></Link><span className="brand-tag">A clearer view of your financial future</span></div>
    <section className="auth-card"><div className="auth-card-top"><div className="eyebrow"><span className="live-dot" /> YOUR INVESTING, IN FOCUS</div><h1>{mode === 'login' ? 'Welcome back.' : 'Start with clarity.'}</h1><p>{mode === 'login' ? 'Sign in to continue to your workspace.' : 'Create your account to set up your workspace.'}</p></div>{children}<div className="auth-switch">{mode === 'login' ? <>New to PortfoLens? <Link to="/register">Create an account <span>→</span></Link></> : <>Already have an account? <Link to="/login">Sign in <span>→</span></Link></>}</div></section>
    <footer className="auth-footer"><span>© 2025 PortfoLens</span><span>BUILT FOR A CLEARER PERSPECTIVE <i>✳</i></span></footer>
  </main>
}
