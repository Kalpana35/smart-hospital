import './DashboardPlaceholder.css'

export default function DashboardPlaceholder({ user, roleName, onLogout }) {
  const getRoleDetails = () => {
    switch (roleName) {
      case 'ROLE_ADMIN':
        return {
          title: 'Admin Operations Dashboard',
          subtitle: 'Live Bed Matrix, Room Management & Allocation Control',
          icon: '📊',
          path: '/admin',
        }
      case 'ROLE_DOCTOR':
        return {
          title: 'Doctor Clinical Workstation',
          subtitle: 'Patient Admissions, Bed Status & Allocation Recommendations',
          icon: '🧑‍⚕️',
          path: '/doctor',
        }
      case 'ROLE_PATIENT':
        return {
          title: 'Patient Portal',
          subtitle: 'Personal Admission Records & Bed Status',
          icon: '🏥',
          path: '/patient',
        }
      default:
        return {
          title: 'Hospital Dashboard',
          subtitle: 'Smart Hospital Bed Management System',
          icon: '🏥',
          path: '/dashboard',
        }
    }
  }

  const details = getRoleDetails()

  const handleLogoutClick = async () => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      })
    } catch (err) {
      console.error('Logout error:', err)
    } finally {
      if (onLogout) {
        onLogout()
      }
    }
  }

  return (
    <div className="dashboard-shell">
      <header className="dashboard-topbar">
        <div className="brand">
          <span className="brand-icon">🏥</span>
          <div className="brand-text">
            <h2>SmartHospital</h2>
            <span>Bed Management & Allocation</span>
          </div>
        </div>

        <div className="topbar-right">
          <div className="user-badge">
            <span className="user-avatar">👤</span>
            <div className="user-info">
              <span className="user-name">
                {user?.fullName || user?.username || 'User'}
              </span>
              <span className="user-role">{user?.role || roleName}</span>
            </div>
          </div>

          <button
            type="button"
            className="btn-logout"
            onClick={handleLogoutClick}
            title="Log out of system"
          >
            <span>🚪</span>
            <span>Sign Out</span>
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="role-card">
          <div className="role-header">
            <div className="role-icon">{details.icon}</div>
            <div>
              <h1>{details.title}</h1>
              <p>{details.subtitle}</p>
            </div>
          </div>

          <div className="status-callout">
            ✅ <strong>Login & Role Authentication Verified:</strong> You have
            successfully authenticated as <code>{user?.username}</code> with
            role <code>{user?.role}</code>. Current active route:{' '}
            <code>{details.path}</code>.
          </div>

          <ul className="next-steps-list">
            <li>
              <span>🔒</span>
              <span>
                <strong>Session State:</strong> Verified by backend Spring
                Security session cookie (<code>JSESSIONID</code>).
              </span>
            </li>
            <li>
              <span>🚀</span>
              <span>
                <strong>Next Step:</strong> Dashboards will be implemented in
                the upcoming phase according to role specifications.
              </span>
            </li>
          </ul>
        </div>
      </main>
    </div>
  )
}

