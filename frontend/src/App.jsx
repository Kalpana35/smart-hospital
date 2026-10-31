import { useEffect, useState } from 'react'
import AdminDashboard from './components/AdminDashboard'
import DoctorDashboard from './components/DoctorDashboard'
import Login from './components/Login'
import PatientDashboard from './components/PatientDashboard'
import ReceptionistDashboard from './components/ReceptionistDashboard'
import { validateStrongPassword } from './utils/passwordPolicy'

export default function App() {
  const [currentPath, setCurrentPath] = useState(window.location.pathname || '/')
  const [currentUser, setCurrentUser] = useState(null)
  const [checkingAuth, setCheckingAuth] = useState(true)

  // First Login Password Change state
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [pwdError, setPwdError] = useState('')
  const [pwdSuccess, setPwdSuccess] = useState('')
  const [pwdLoading, setPwdLoading] = useState(false)

  const navigate = (path) => {
    window.history.pushState({}, '', path)
    setCurrentPath(path)
  }

  // Synchronize on browser Back/Forward buttons
  useEffect(() => {
    const onPopState = () => {
      setCurrentPath(window.location.pathname || '/')
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  // Check active session on initial load via /api/auth/me
  useEffect(() => {
    const checkActiveSession = async () => {
      try {
        const response = await fetch('/api/auth/me', {
          method: 'GET',
          credentials: 'include',
        })

        if (response.ok) {
          const result = await response.json()
          if (result.success && result.data) {
            const user = result.data
            setCurrentUser(user)

            // If the user lands on / or /login while logged in, redirect to their role dashboard
            const initialPath = window.location.pathname || '/'
            if (initialPath === '/' || initialPath === '/login') {
              if (user.role === 'ROLE_ADMIN') {
                navigate('/admin')
              } else if (user.role === 'ROLE_RECEPTIONIST') {
                navigate('/receptionist')
              } else if (user.role === 'ROLE_DOCTOR') {
                navigate('/doctor')
              } else if (user.role === 'ROLE_PATIENT') {
                navigate('/patient')
              }
            }
          }
        } else {
          setCurrentUser(null)
          const initialPath = window.location.pathname || '/'
          if (['/admin', '/receptionist', '/doctor', '/patient'].includes(initialPath)) {
            navigate('/login')
          }
        }
      } catch {
        setCurrentUser(null)
      } finally {
        setCheckingAuth(false)
      }
    }

    checkActiveSession()
  }, [])

  const handleLoginSuccess = (userProfile) => {
    setCurrentUser(userProfile)
    const role = userProfile?.role

    if (role === 'ROLE_ADMIN') {
      navigate('/admin')
    } else if (role === 'ROLE_RECEPTIONIST') {
      navigate('/receptionist')
    } else if (role === 'ROLE_DOCTOR') {
      navigate('/doctor')
    } else if (role === 'ROLE_PATIENT') {
      navigate('/patient')
    } else {
      navigate('/')
    }
  }

  const handleLogout = () => {
    setCurrentUser(null)
    navigate('/login')
  }

  const handlePasswordChangeSubmit = async (e) => {
    e.preventDefault()
    setPwdError('')
    setPwdSuccess('')

    if (!currentPassword) {
      setPwdError('Current temporary password is required.')
      return
    }
    const pwdErrors = validateStrongPassword(newPassword)
    if (pwdErrors.length > 0) {
      setPwdError(`New password must meet policy: ${pwdErrors.join(', ')} (e.g., Reception@123)`)
      return
    }
    if (newPassword !== confirmPassword) {
      setPwdError('New passwords do not match.')
      return
    }

    setPwdLoading(true)
    try {
      const res = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          currentPassword,
          newPassword,
        }),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        setPwdSuccess('Password updated successfully!')
        setTimeout(() => {
          setCurrentUser((prev) => ({ ...prev, passwordChangeRequired: false }))
        }, 800)
      } else {
        setPwdError(data.message || 'Failed to update password')
      }
    } catch {
      setPwdError('Unable to connect to server. Please try again.')
    } finally {
      setPwdLoading(false)
    }
  }

  if (checkingAuth) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          backgroundColor: '#0f172a',
          color: '#ffffff',
          fontFamily: 'sans-serif',
          gap: '0.75rem',
        }}
      >
        <span
          style={{
            width: '20px',
            height: '20px',
            border: '2px solid rgba(255,255,255,0.3)',
            borderRadius: '50%',
            borderTopColor: '#ffffff',
            display: 'inline-block',
            animation: 'spin 0.6s linear infinite',
          }}
        ></span>
        <span>Verifying SmartHospital session...</span>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    )
  }

  // Render Role Dashboard component
  const renderDashboard = () => {
    if (!currentUser) {
      return <Login onLoginSuccess={handleLoginSuccess} />
    }

    // Route: Admin Dashboard
    if (currentPath === '/admin') {
      if (currentUser.role !== 'ROLE_ADMIN') {
        if (currentUser.role === 'ROLE_RECEPTIONIST') return <ReceptionistDashboard user={currentUser} onLogout={handleLogout} />
        if (currentUser.role === 'ROLE_DOCTOR') return <DoctorDashboard user={currentUser} onLogout={handleLogout} />
        if (currentUser.role === 'ROLE_PATIENT') return <PatientDashboard user={currentUser} onLogout={handleLogout} />
        return <Login onLoginSuccess={handleLoginSuccess} />
      }
      return <AdminDashboard user={currentUser} onLogout={handleLogout} />
    }

    // Route: Receptionist Dashboard
    if (currentPath === '/receptionist') {
      if (currentUser.role !== 'ROLE_RECEPTIONIST' && currentUser.role !== 'ROLE_ADMIN') {
        if (currentUser.role === 'ROLE_DOCTOR') return <DoctorDashboard user={currentUser} onLogout={handleLogout} />
        if (currentUser.role === 'ROLE_PATIENT') return <PatientDashboard user={currentUser} onLogout={handleLogout} />
        return <Login onLoginSuccess={handleLoginSuccess} />
      }
      return <ReceptionistDashboard user={currentUser} onLogout={handleLogout} />
    }

    // Route: Doctor Dashboard
    if (currentPath === '/doctor') {
      if (currentUser.role !== 'ROLE_DOCTOR' && currentUser.role !== 'ROLE_ADMIN') {
        if (currentUser.role === 'ROLE_RECEPTIONIST') return <ReceptionistDashboard user={currentUser} onLogout={handleLogout} />
        if (currentUser.role === 'ROLE_PATIENT') return <PatientDashboard user={currentUser} onLogout={handleLogout} />
        return <Login onLoginSuccess={handleLoginSuccess} />
      }
      return <DoctorDashboard user={currentUser} onLogout={handleLogout} />
    }

    // Route: Patient Dashboard
    if (currentPath === '/patient') {
      return <PatientDashboard user={currentUser} onLogout={handleLogout} />
    }

    // Default Route according to role
    if (currentUser.role === 'ROLE_ADMIN') return <AdminDashboard user={currentUser} onLogout={handleLogout} />
    if (currentUser.role === 'ROLE_RECEPTIONIST') return <ReceptionistDashboard user={currentUser} onLogout={handleLogout} />
    if (currentUser.role === 'ROLE_DOCTOR') return <DoctorDashboard user={currentUser} onLogout={handleLogout} />
    if (currentUser.role === 'ROLE_PATIENT') return <PatientDashboard user={currentUser} onLogout={handleLogout} />

    return <Login onLoginSuccess={handleLoginSuccess} />
  }

  return (
    <>
      {renderDashboard()}

      {/* Forced Password Change Modal on First Login */}
      {currentUser && currentUser.passwordChangeRequired && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.85)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 99999,
            padding: '1.5rem',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '12px',
              maxWidth: '460px',
              width: '100%',
              padding: '2rem',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.3)',
            }}
          >
            <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
              <span style={{ fontSize: '2.5rem' }}>🔒</span>
              <h2 style={{ margin: '0.5rem 0 0.25rem 0', color: '#0f172a' }}>Update Temporary Password</h2>
              <p style={{ margin: 0, fontSize: '0.875rem', color: '#64748b' }}>
                Your hospital staff account was provisioned with a temporary password. Please set a new permanent password to continue.
              </p>
            </div>

            {pwdError && (
              <div
                style={{
                  backgroundColor: '#fee2e2',
                  border: '1px solid #fecaca',
                  color: '#b91c1c',
                  padding: '0.75rem',
                  borderRadius: '6px',
                  marginBottom: '1rem',
                  fontSize: '0.85rem',
                }}
              >
                {pwdError}
              </div>
            )}

            {pwdSuccess && (
              <div
                style={{
                  backgroundColor: '#dcfce7',
                  border: '1px solid #bbf7d0',
                  color: '#15803d',
                  padding: '0.75rem',
                  borderRadius: '6px',
                  marginBottom: '1rem',
                  fontSize: '0.85rem',
                }}
              >
                {pwdSuccess}
              </div>
            )}

            <form onSubmit={handlePasswordChangeSubmit}>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  Current Temporary Password *
                </label>
                <input
                  type="password"
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.85rem',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.9rem',
                    boxSizing: 'border-box',
                  }}
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="Enter temporary password"
                  required
                />
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  New Permanent Password *
                </label>
                <input
                  type="password"
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.85rem',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.9rem',
                    boxSizing: 'border-box',
                  }}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="e.g. Reception@123"
                  required
                />
                <span style={{ display: 'block', fontSize: '0.75rem', color: '#64748b', marginTop: '0.3rem' }}>
                  Must be at least 6 characters with uppercase (A-Z), lowercase (a-z), number (0-9), and special character (@, #, $, !).
                </span>
              </div>

              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  Confirm New Password *
                </label>
                <input
                  type="password"
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.85rem',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.9rem',
                    boxSizing: 'border-box',
                  }}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter new password"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={pwdLoading}
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  fontWeight: 600,
                  fontSize: '0.95rem',
                  cursor: 'pointer',
                  opacity: pwdLoading ? 0.7 : 1,
                }}
              >
                {pwdLoading ? 'Updating Password...' : 'Save & Proceed to Dashboard'}
              </button>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
