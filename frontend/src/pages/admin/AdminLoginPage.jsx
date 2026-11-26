import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import toast from 'react-hot-toast'
import { Shield, Eye, EyeOff, Mail, Lock, ArrowLeft } from 'lucide-react'

export default function AdminLoginPage() {
  const { login, logout, loading } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [showPw, setShowPw] = useState(false)
  const [errors, setErrors] = useState({})

  const validate = () => {
    const e = {}
    if (!form.email) e.email = 'Admin email is required'
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Enter a valid email'
    if (!form.password) e.password = 'Password is required'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    try {
      const user = await login(form)
      if (user.role !== 'ADMIN') {
        await logout()
        toast.error('Access denied. This portal is restricted to authorized college administrators.')
        return
      }
      toast.success(`Welcome, ${user.name}!`)
      navigate('/admin/dashboard')
    } catch (err) {
      const msg = err.response?.data?.message || 'Invalid administrator credentials'
      toast.error(msg)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__header">
          <Link to="/" className="auth-logo">
            <Shield size={24} />
            <span>College L&amp;F Admin</span>
          </Link>
          <h1 className="auth-card__title">Administrator Portal</h1>
          <p className="auth-card__sub">Authorized college personnel only</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label">Admin Email</label>
            <div className="input-wrap">
              <Mail size={16} className="input-icon" />
              <input
                type="email"
                className={`form-input form-input--icon ${errors.email ? 'form-input--error' : ''}`}
                placeholder="admin@college.edu"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            {errors.email && <span className="form-error">{errors.email}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <div className="input-wrap">
              <Lock size={16} className="input-icon" />
              <input
                type={showPw ? 'text' : 'password'}
                className={`form-input form-input--icon form-input--icon-right ${errors.password ? 'form-input--error' : ''}`}
                placeholder="Administrator password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
              <button type="button" className="input-icon-right" onClick={() => setShowPw(!showPw)}>
                {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {errors.password && <span className="form-error">{errors.password}</span>}
          </div>

          <button type="submit" className="btn btn--primary btn--full" disabled={loading}>
            {loading ? 'Verifying…' : 'Sign In as Admin'}
          </button>
        </form>

        <p className="auth-card__footer">
          <Link to="/login" className="auth-link" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <ArrowLeft size={14} /> Student Login
          </Link>
        </p>
      </div>
    </div>
  )
}
