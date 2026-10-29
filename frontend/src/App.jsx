import { useEffect, useState } from 'react'
import AdminDashboard from './components/AdminDashboard'
import DoctorDashboard from './components/DoctorDashboard'
import Login from './components/Login'
import PatientDashboard from './components/PatientDashboard'

export default function App() {
  const [currentPath, setCurrentPath] = useState(window.location.pathname || '/')
  const [currentUser, setCurrentUser] = useState(null)
  const [checkingAuth, setCheckingAuth] = useState(true)

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
          if (['/admin', '/doctor', '/patient'].includes(initialPath)) {
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

  // Route: Admin Dashboard
  if (currentPath === '/admin') {
    if (!currentUser) {
      return <Login onLoginSuccess={handleLoginSuccess} />
    }
    return (
      <AdminDashboard
        user={currentUser}
        onLogout={handleLogout}
      />
    )
  }

  // Route: Doctor Dashboard
  if (currentPath === '/doctor') {
    if (!currentUser) {
      return <Login onLoginSuccess={handleLoginSuccess} />
    }
    return (
      <DoctorDashboard
        user={currentUser}
        onLogout={handleLogout}
      />
    )
  }

  // Route: Patient Dashboard
  if (currentPath === '/patient') {
    if (!currentUser) {
      return <Login onLoginSuccess={handleLoginSuccess} />
    }
    return (
      <PatientDashboard
        user={currentUser}
        onLogout={handleLogout}
      />
    )
  }

  // Default Route: Login Page
  return <Login onLoginSuccess={handleLoginSuccess} />
}
