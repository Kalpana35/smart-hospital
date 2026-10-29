import { useState } from 'react'
import './Login.css'

export default function Login({ onLoginSuccess }) {
  const [activeTab, setActiveTab] = useState('login') // 'login' | 'register'

  // Login form state
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})

  // Self-registration form state
  const [regPatientCode, setRegPatientCode] = useState('')
  const [regFullName, setRegFullName] = useState('')
  const [regUsername, setRegUsername] = useState('')
  const [regPassword, setRegPassword] = useState('')
  const [regConfirmPassword, setRegConfirmPassword] = useState('')
  const [showRegPassword, setShowRegPassword] = useState(false)
  const [regLoading, setRegLoading] = useState(false)

  const validateLogin = () => {
    const errs = {}
    if (!username.trim()) {
      errs.username = 'Username is required'
    }
    if (!password) {
      errs.password = 'Password is required'
    }
    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleLoginSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccessMsg('')

    if (!validateLogin()) {
      return
    }

    setLoading(true)

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          username: username.trim(),
          password,
        }),
      })

      setPassword('')
      const result = await response.json()

      if (response.ok && result.success) {
        setSuccessMsg('Authentication successful! Routing to dashboard...')

        const meResponse = await fetch('/api/auth/me', {
          method: 'GET',
          credentials: 'include',
        })

        if (meResponse.ok) {
          const meResult = await meResponse.json()
          const userProfile = meResult.data || result.data

          setTimeout(() => {
            if (onLoginSuccess) {
              onLoginSuccess(userProfile)
            }
          }, 400)
        } else {
          if (onLoginSuccess) {
            onLoginSuccess(result.data)
          }
        }
      } else {
        setError(result.message || 'Invalid username or password. Please try again.')
      }
    } catch {
      setError('Unable to reach the hospital server. Please verify your connection.')
    } finally {
      setLoading(false)
    }
  }

  const handleRegisterSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccessMsg('')

    if (!regPatientCode.trim()) {
      setError('Patient Code / MRN is required to link your hospital record.')
      return
    }
    if (!regFullName.trim()) {
      setError('Full Name is required (must match your hospital admission record).')
      return
    }
    if (!regUsername.trim()) {
      setError('Desired Username is required.')
      return
    }
    if (regPassword.length < 6) {
      setError('Password must be at least 6 characters.')
      return
    }
    if (regPassword !== regConfirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setRegLoading(true)
    try {
      const res = await fetch('/api/auth/register-patient', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          patientCode: regPatientCode.trim(),
          fullName: regFullName.trim(),
          username: regUsername.trim(),
          password: regPassword,
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        setSuccessMsg('Registration successful! Account linked to patient record. Please sign in.')
        setUsername(regUsername.trim())
        setActiveTab('login')
        setRegPatientCode('')
        setRegFullName('')
        setRegUsername('')
        setRegPassword('')
        setRegConfirmPassword('')
      } else {
        setError(data.message || 'Failed to complete registration.')
      }
    } catch {
      setError('Unable to reach the hospital server. Please try again.')
    } finally {
      setRegLoading(false)
    }
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">🏥</div>
          <h1>SmartHospital</h1>
          <p>Bed Management & Allocation Optimization</p>
          <div className="role-chips">
            <span className="role-chip">Admin</span>
            <span className="role-chip">Doctor</span>
            <span className="role-chip">Patient</span>
          </div>
        </div>

        {/* Tab Switch */}
        <div className="auth-tab-switch">
          <button
            type="button"
            className={`auth-tab-btn ${activeTab === 'login' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('login')
              setError('')
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`auth-tab-btn ${activeTab === 'register' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('register')
              setError('')
              setSuccessMsg('')
            }}
          >
            Patient Self-Registration
          </button>
        </div>

        {error && (
          <div className="login-alert error" role="alert">
            <span>⚠️</span>
            <span>{error}</span>
          </div>
        )}

        {successMsg && (
          <div className="login-alert info" role="status">
            <span>✅</span>
            <span>{successMsg}</span>
          </div>
        )}

        {activeTab === 'login' ? (
          <form className="login-form" onSubmit={handleLoginSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <div className="input-wrapper">
                <input
                  id="username"
                  type="text"
                  className={`form-input ${fieldErrors.username ? 'has-error' : ''}`}
                  placeholder="Enter username"
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value)
                    if (fieldErrors.username) {
                      setFieldErrors((prev) => ({ ...prev, username: '' }))
                    }
                  }}
                  disabled={loading}
                  autoFocus
                  autoComplete="username"
                />
              </div>
              {fieldErrors.username && (
                <span className="field-error">{fieldErrors.username}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <div className="input-wrapper">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  className={`form-input ${fieldErrors.password ? 'has-error' : ''}`}
                  placeholder="Enter password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value)
                    if (fieldErrors.password) {
                      setFieldErrors((prev) => ({ ...prev, password: '' }))
                    }
                  }}
                  disabled={loading}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  className="toggle-password-btn"
                  onClick={() => setShowPassword((prev) => !prev)}
                  title={showPassword ? 'Hide password' : 'Show password'}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  tabIndex={-1}
                >
                  {showPassword ? '👁️' : '🔒'}
                </button>
              </div>
              {fieldErrors.password && (
                <span className="field-error">{fieldErrors.password}</span>
              )}
            </div>

            <button type="submit" className="btn-submit" disabled={loading}>
              {loading ? (
                <>
                  <span className="spinner"></span>
                  <span>Signing In...</span>
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </form>
        ) : (
          <form className="login-form" onSubmit={handleRegisterSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="regPatientCode">Hospital Patient Code / MRN *</label>
              <div className="input-wrapper">
                <input
                  id="regPatientCode"
                  type="text"
                  className="form-input"
                  placeholder="e.g. DEMO-2026-003 or PAT-101"
                  value={regPatientCode}
                  onChange={(e) => setRegPatientCode(e.target.value)}
                  disabled={regLoading}
                  required
                />
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                Your official patient code from your admission or intake slip.
              </span>
            </div>

            <div className="form-group">
              <label htmlFor="regFullName">Full Name *</label>
              <div className="input-wrapper">
                <input
                  id="regFullName"
                  type="text"
                  className="form-input"
                  placeholder="Must match official hospital record"
                  value={regFullName}
                  onChange={(e) => setRegFullName(e.target.value)}
                  disabled={regLoading}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="regUsername">Choose Username *</label>
              <div className="input-wrapper">
                <input
                  id="regUsername"
                  type="text"
                  className="form-input"
                  placeholder="Enter desired username"
                  value={regUsername}
                  onChange={(e) => setRegUsername(e.target.value)}
                  disabled={regLoading}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="regPassword">Password * (min 6 characters)</label>
              <div className="input-wrapper">
                <input
                  id="regPassword"
                  type={showRegPassword ? 'text' : 'password'}
                  className="form-input"
                  placeholder="Create password"
                  value={regPassword}
                  onChange={(e) => setRegPassword(e.target.value)}
                  disabled={regLoading}
                  required
                />
                <button
                  type="button"
                  className="toggle-password-btn"
                  onClick={() => setShowRegPassword((prev) => !prev)}
                  title={showRegPassword ? 'Hide password' : 'Show password'}
                  aria-label={showRegPassword ? 'Hide password' : 'Show password'}
                  tabIndex={-1}
                >
                  {showRegPassword ? '👁️' : '🔒'}
                </button>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="regConfirmPassword">Confirm Password *</label>
              <div className="input-wrapper">
                <input
                  id="regConfirmPassword"
                  type={showRegPassword ? 'text' : 'password'}
                  className="form-input"
                  placeholder="Re-enter password"
                  value={regConfirmPassword}
                  onChange={(e) => setRegConfirmPassword(e.target.value)}
                  disabled={regLoading}
                  required
                />
              </div>
            </div>

            <button type="submit" className="btn-submit" disabled={regLoading}>
              {regLoading ? (
                <>
                  <span className="spinner"></span>
                  <span>Verifying & Linking...</span>
                </>
              ) : (
                'Register & Link Account'
              )}
            </button>
          </form>
        )}

        <div className="login-footer-note">
          <strong>Session Authentication:</strong> Protected by Spring Security HTTP-only cookies.
          Credentials are never stored in browser storage.
        </div>
      </div>
    </div>
  )
}
