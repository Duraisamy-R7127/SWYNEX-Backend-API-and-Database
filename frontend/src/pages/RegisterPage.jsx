import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'
import { Search, Eye, EyeOff, Mail, Lock, User, Phone } from 'lucide-react'

export default function RegisterPage() {
  const { register, loading } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' })
  const [showPw, setShowPw] = useState(false)
  const [errors, setErrors] = useState({})

  const validate = () => {
    const e = {}
    if (!form.name || form.name.length < 2) e.name = 'Name must be at least 2 characters'
    if (!form.email) e.email = 'Email is required'
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Enter a valid email'
    if (!form.password) e.password = 'Password is required'
    else if (!/^(?=.*[A-Za-z])(?=.*\d).{8,64}$/.test(form.password))
      e.password = 'Min 8 chars with at least one letter and one digit'
    if (form.phone && !/^[0-9+\-\s]{7,15}$/.test(form.phone))
      e.phone = 'Phone must be 7-15 digits'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    try {
      await register({ ...form, phone: form.phone || undefined })
      toast.success('Account created! Please sign in.')
      navigate('/login')
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed'
      toast.error(msg)
    }
  }

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div className="auth-page">
      <div className="auth-card auth-card--wide">
        <div className="auth-card__header">
          <Link to="/" className="auth-logo">
            <Search size={24} />
            <span>College L&amp;F Hub</span>
          </Link>
          <h1 className="auth-card__title">Student Registration</h1>
          <p className="auth-card__sub">Create an account to report lost items and claim found belongings</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <div className="input-wrap">
                <User size={16} className="input-icon" />
                <input type="text" className={`form-input form-input--icon ${errors.name ? 'form-input--error' : ''}`}
                  placeholder="John Doe" value={form.name} onChange={set('name')} />
              </div>
              {errors.name && <span className="form-error">{errors.name}</span>}
            </div>
            <div className="form-group">
              <label className="form-label">Phone <span className="form-optional">(optional)</span></label>
              <div className="input-wrap">
                <Phone size={16} className="input-icon" />
                <input type="tel" className={`form-input form-input--icon ${errors.phone ? 'form-input--error' : ''}`}
                  placeholder="+1 234 567 8900" value={form.phone} onChange={set('phone')} />
              </div>
              {errors.phone && <span className="form-error">{errors.phone}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <div className="input-wrap">
              <Mail size={16} className="input-icon" />
              <input type="email" className={`form-input form-input--icon ${errors.email ? 'form-input--error' : ''}`}
                placeholder="you@example.com" value={form.email} onChange={set('email')} />
            </div>
            {errors.email && <span className="form-error">{errors.email}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <div className="input-wrap">
              <Lock size={16} className="input-icon" />
              <input type={showPw ? 'text' : 'password'}
                className={`form-input form-input--icon form-input--icon-right ${errors.password ? 'form-input--error' : ''}`}
                placeholder="Min 8 chars, letter + digit" value={form.password} onChange={set('password')} />
              <button type="button" className="input-icon-right" onClick={() => setShowPw(!showPw)}>
                {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {errors.password && <span className="form-error">{errors.password}</span>}
          </div>

          <button type="submit" className="btn btn--primary btn--full" disabled={loading}>
            {loading ? 'Creating account…' : 'Create Account'}
          </button>
        </form>

        <p className="auth-card__footer">
          Already have an account? <Link to="/login" className="auth-link">Sign in</Link>
        </p>
      </div>
      <div className="auth-visual" />
    </div>
  )
}
