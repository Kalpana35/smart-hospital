import { useState } from 'react'
import { validateStrongPassword } from '../utils/passwordPolicy'
import './Login.css'

export default function Login({ onLoginSuccess }) {
  const [view, setView] = useState('signin') // 'signin' | 'patient-signup'

  // Login form state
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})

  // Patient self-registration form state
  const [regPatientCode, setRegPatientCode] = useState('')
  const [regFullName, setRegFullName] = useState('')
  const [regUsername, setRegUsername] = useState('')
  const [regPassword, setRegPassword] = useState('')
  const [regConfirmPassword, setRegConfirmPassword] = useState('')
  const [showRegPassword, setShowRegPassword] = useState(false)
  const [showRegConfirmPassword, setShowRegConfirmPassword] = useState(false)
  const [regLoading, setRegLoading] = useState(false)

  // Real-time password criteria for registration
  const pwdReqs = {
    length: regPassword.length >= 6,
    uppercase: /[A-Z]/.test(regPassword),
    lowercase: /[a-z]/.test(regPassword),
    number: /[0-9]/.test(regPassword),
    special: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?`~]/.test(regPassword),
  }

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
        setSuccessMsg('Authentication successful! Routing to your dashboard...')

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
          }, 350)
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

  const handlePatientRegisterSubmit = async (e) => {
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

    const pwdErrors = validateStrongPassword(regPassword)
    if (pwdErrors.length > 0) {
      setError(`Password does not meet requirements: ${pwdErrors.join(', ')} (e.g., Reception@123)`)
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
        setView('signin')
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
    <div className="login-page">
      <div className="login-card">
        {/* Brand Header */}
        <div className="brand-header">
          <div className="hospital-logo-badge">
            <svg
              className="hospital-icon-svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <rect x="3" y="3" width="18" height="18" rx="4" />
              <line x1="12" y1="8" x2="12" y2="16" />
              <line x1="8" y1="12" x2="16" y2="12" />
            </svg>
          </div>
          <h1 className="brand-title">SmartHospital</h1>
          <p className="brand-tagline">Bed Management & Allocation Optimization</p>
        </div>

        {/* View Switcher Alert / Notice */}
        {error && (
          <div className="login-alert error" role="alert">
            <span className="alert-icon">⚠️</span>
            <div className="alert-text">{error}</div>
          </div>
        )}

        {successMsg && (
          <div className="login-alert info" role="status">
            <span className="alert-icon">✅</span>
            <div className="alert-text">{successMsg}</div>
          </div>
        )}

        {view === 'signin' ? (
          /* Sign In View */
          <div className="auth-view">
            <div className="form-header">
              <h2 className="view-heading">Welcome Back</h2>
              <p className="view-subtitle">Sign in to your hospital account.</p>
            </div>

            <form className="auth-form" onSubmit={handleLoginSubmit} noValidate>
              <div className="form-group">
                <label htmlFor="username" className="form-label">Username</label>
                <div className="input-field-wrapper">
                  <input
                    id="username"
                    type="text"
                    className={`form-input ${fieldErrors.username ? 'has-error' : ''}`}
                    placeholder="Enter your username"
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
                  <span className="field-error-text">{fieldErrors.username}</span>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="password" className="form-label">Password</label>
                <div className="input-field-wrapper">
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    className={`form-input ${fieldErrors.password ? 'has-error' : ''}`}
                    placeholder="Enter your password"
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
                    className="password-toggle-btn"
                    onClick={() => setShowPassword((prev) => !prev)}
                    title={showPassword ? 'Hide password' : 'Show password'}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    tabIndex={-1}
                  >
                    {showPassword ? (
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                      </svg>
                    ) : (
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </div>
                {fieldErrors.password && (
                  <span className="field-error-text">{fieldErrors.password}</span>
                )}
              </div>

              <button type="submit" className="btn-primary-signin" disabled={loading}>
                {loading ? (
                  <>
                    <span className="btn-spinner"></span>
                    <span>Signing In...</span>
                  </>
                ) : (
                  'Sign In'
                )}
              </button>
            </form>

            {/* Link to Patient Sign-Up */}
            <div className="auth-footer-nav">
              <span>Don't have an account? </span>
              <button
                type="button"
                className="btn-link"
                onClick={() => {
                  setView('patient-signup')
                  setError('')
                  setSuccessMsg('')
                }}
              >
                Patient Sign-Up
              </button>
            </div>

            {/* Staff Accounts Provisioning Notice */}
            <div className="staff-admin-notice">
              <span className="staff-notice-icon">🔒</span>
              <div className="staff-notice-text">
                <strong>Hospital Staff:</strong> Doctor, Receptionist, and Administrator accounts are provisioned exclusively by hospital administrators. Doctors use the credentials provided by the Admin.
              </div>
            </div>
          </div>
        ) : (
          /* Patient Sign-Up View */
          <div className="auth-view">
            <div className="form-header">
              <h2 className="view-heading">Create Your Patient Account</h2>
              <p className="view-subtitle">Link your existing hospital admission record or MRN.</p>
            </div>

            <form className="auth-form" onSubmit={handlePatientRegisterSubmit} noValidate>
              <div className="form-group">
                <label htmlFor="regPatientCode" className="form-label">Hospital Patient Code / MRN *</label>
                <div className="input-field-wrapper">
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
                <span className="field-help-text">
                  Your official patient code from your admission or intake slip.
                </span>
              </div>

              <div className="form-group">
                <label htmlFor="regFullName" className="form-label">Full Name *</label>
                <div className="input-field-wrapper">
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
                <label htmlFor="regUsername" className="form-label">Choose Username *</label>
                <div className="input-field-wrapper">
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
                <label htmlFor="regPassword" className="form-label">Password * (Strong Password Policy)</label>
                <div className="input-field-wrapper">
                  <input
                    id="regPassword"
                    type={showRegPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Create a strong password"
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    disabled={regLoading}
                    required
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowRegPassword((prev) => !prev)}
                    title={showRegPassword ? 'Hide password' : 'Show password'}
                    aria-label={showRegPassword ? 'Hide password' : 'Show password'}
                    tabIndex={-1}
                  >
                    {showRegPassword ? (
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                      </svg>
                    ) : (
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </div>

                {/* Password Criteria Checklist */}
                <div className="pwd-checklist">
                  <div className={`pwd-criterion ${pwdReqs.length ? 'valid' : ''}`}>
                    <span className="criterion-indicator">{pwdReqs.length ? '✓' : '•'}</span>
                    <span>Min 6 characters</span>
                  </div>
                  <div className={`pwd-criterion ${pwdReqs.uppercase ? 'valid' : ''}`}>
                    <span className="criterion-indicator">{pwdReqs.uppercase ? '✓' : '•'}</span>
                    <span>Uppercase (A-Z)</span>
                  </div>
                  <div className={`pwd-criterion ${pwdReqs.lowercase ? 'valid' : ''}`}>
                    <span className="criterion-indicator">{pwdReqs.lowercase ? '✓' : '•'}</span>
                    <span>Lowercase (a-z)</span>
                  </div>
                  <div className={`pwd-criterion ${pwdReqs.number ? 'valid' : ''}`}>
                    <span className="criterion-indicator">{pwdReqs.number ? '✓' : '•'}</span>
                    <span>Number (0-9)</span>
                  </div>
                  <div className={`pwd-criterion ${pwdReqs.special ? 'valid' : ''}`}>
                    <span className="criterion-indicator">{pwdReqs.special ? '✓' : '•'}</span>
                    <span>Special (@, #, $, !)</span>
                  </div>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="regConfirmPassword" className="form-label">Confirm Password *</label>
                <div className="input-field-wrapper">
                  <input
                    id="regConfirmPassword"
                    type={showRegConfirmPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Re-enter password"
                    value={regConfirmPassword}
                    onChange={(e) => setRegConfirmPassword(e.target.value)}
                    disabled={regLoading}
                    required
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowRegConfirmPassword((prev) => !prev)}
                    title={showRegConfirmPassword ? 'Hide password' : 'Show password'}
                    aria-label={showRegConfirmPassword ? 'Hide password' : 'Show password'}
                    tabIndex={-1}
                  >
                    {showRegConfirmPassword ? (
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                      </svg>
                    ) : (
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              <button type="submit" className="btn-primary-signin" disabled={regLoading}>
                {regLoading ? (
                  <>
                    <span className="btn-spinner"></span>
                    <span>Verifying & Linking...</span>
                  </>
                ) : (
                  'Create Patient Account'
                )}
              </button>
            </form>

            <div className="auth-footer-nav">
              <span>Already have an account? </span>
              <button
                type="button"
                className="btn-link"
                onClick={() => {
                  setView('signin')
                  setError('')
                  setSuccessMsg('')
                }}
              >
                Sign In
              </button>
            </div>
          </div>
        )}

        <div className="card-security-footer">
          <span>Protected by Spring Security HTTP-only session cookies</span>
        </div>
      </div>
    </div>
  )
}
