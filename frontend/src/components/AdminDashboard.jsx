import { useCallback, useEffect, useState } from 'react'
import './AdminDashboard.css'

export default function AdminDashboard({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState('overview')
  const [rooms, setRooms] = useState([])
  const [beds, setBeds] = useState([])
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(true)
  const [toasts, setToasts] = useState([])

  // Filters for Matrix View
  const [filterWard, setFilterWard] = useState('')
  const [filterStatus, setFilterStatus] = useState('')

  // Form states: Create Room
  const [newRoomNumber, setNewRoomNumber] = useState('')
  const [newRoomType, setNewRoomType] = useState('')
  const [newRoomWard, setNewRoomWard] = useState('')
  const [creatingRoom, setCreatingRoom] = useState(false)

  // Form states: Create Bed
  const [newBedRoomId, setNewBedRoomId] = useState('')
  const [newBedNumber, setNewBedNumber] = useState('')
  const [newBedStatus, setNewBedStatus] = useState('AVAILABLE')
  const [creatingBed, setCreatingBed] = useState(false)

  // Form states: Admit Patient
  const [patientCode, setPatientCode] = useState('')
  const [patientName, setPatientName] = useState('')
  const [patientAge, setPatientAge] = useState('')
  const [patientGender, setPatientGender] = useState('MALE')
  const [patientDiagnosis, setPatientDiagnosis] = useState('')
  const [admissionBedId, setAdmissionBedId] = useState('')
  const [admittingPatient, setAdmittingPatient] = useState(false)

  // Form states: Recommendations
  const [recRoomType, setRecRoomType] = useState('ICU')
  const [recWard, setRecWard] = useState('')
  const [recommendations, setRecommendations] = useState(null)
  const [loadingRecs, setLoadingRecs] = useState(false)

  // Analytics State
  const [analyticsData, setAnalyticsData] = useState(null)

  // Portal Account provision states on admission
  const [createAccountOnAdmit, setCreateAccountOnAdmit] = useState(false)
  const [portalUsername, setPortalUsername] = useState('')
  const [portalPassword, setPortalPassword] = useState('')

  // Provisioning existing patient modal state
  const [provisioningPatient, setProvisioningPatient] = useState(null)
  const [provUsername, setProvUsername] = useState('')
  const [provPassword, setProvPassword] = useState('')
  const [submittingProv, setSubmittingProv] = useState(false)

  // Doctor registrations & approvals state
  const [pendingDoctors, setPendingDoctors] = useState([])
  const [loadingDoctors, setLoadingDoctors] = useState(false)
  const [actionDoctorId, setActionDoctorId] = useState(null)

  const showToast = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random()
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }, [])

  // Centralized fetch helper ensuring session credentials and 401 interception
  const apiFetch = useCallback(
    async (url, options = {}) => {
      const response = await fetch(url, {
        ...options,
        credentials: 'include',
      })
      if (response.status === 401) {
        if (onLogout) onLogout()
        throw new Error('Session expired. Redirecting to login...')
      }
      return response
    },
    [onLogout]
  )

  const loadPendingDoctors = useCallback(async () => {
    try {
      setLoadingDoctors(true)
      const res = await apiFetch('/api/auth/doctors/pending')
      if (res.ok) {
        const json = await res.json()
        setPendingDoctors(json.data || [])
      }
    } catch {
      // non-blocking
    } finally {
      setLoadingDoctors(false)
    }
  }, [apiFetch])

  const loadAllData = useCallback(async () => {
    try {
      setLoading(true)
      const [roomsRes, bedsRes, patientsRes, analyticsRes, doctorsRes] = await Promise.all([
        apiFetch('/api/rooms'),
        apiFetch('/api/beds'),
        apiFetch('/api/patients'),
        apiFetch('/api/analytics/summary'),
        apiFetch('/api/auth/doctors/pending'),
      ])

      if (roomsRes.ok && bedsRes.ok && patientsRes.ok) {
        const roomsJson = await roomsRes.json()
        const bedsJson = await bedsRes.json()
        const patientsJson = await patientsRes.json()

        setRooms(roomsJson.data || [])
        setBeds(bedsJson.data || [])
        setPatients(patientsJson.data || [])

        if (analyticsRes.ok) {
          const aJson = await analyticsRes.json()
          setAnalyticsData(aJson.data)
        }

        if (doctorsRes && doctorsRes.ok) {
          const dJson = await doctorsRes.json()
          setPendingDoctors(dJson.data || [])
        }
      } else {
        showToast('Failed to load some hospital data', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error communicating with server', 'error')
    } finally {
      setLoading(false)
    }
  }, [apiFetch, showToast])

  useEffect(() => {
    let ignore = false

    const initLoad = async () => {
      try {
        const [roomsRes, bedsRes, patientsRes, analyticsRes, doctorsRes] = await Promise.all([
          apiFetch('/api/rooms'),
          apiFetch('/api/beds'),
          apiFetch('/api/patients'),
          apiFetch('/api/analytics/summary'),
          apiFetch('/api/auth/doctors/pending'),
        ])

        if (ignore) return

        if (roomsRes.ok && bedsRes.ok && patientsRes.ok) {
          const roomsJson = await roomsRes.json()
          const bedsJson = await bedsRes.json()
          const patientsJson = await patientsRes.json()

          setRooms(roomsJson.data || [])
          setBeds(bedsJson.data || [])
          setPatients(patientsJson.data || [])

          if (analyticsRes.ok) {
            const aJson = await analyticsRes.json()
            setAnalyticsData(aJson.data)
          }

          if (doctorsRes && doctorsRes.ok) {
            const dJson = await doctorsRes.json()
            setPendingDoctors(dJson.data || [])
          }
        } else {
          showToast('Failed to load some hospital data', 'error')
        }
      } catch (err) {
        if (!ignore) {
          showToast(err.message || 'Error communicating with server', 'error')
        }
      } finally {
        if (!ignore) {
          setLoading(false)
        }
      }
    }

    initLoad()

    return () => {
      ignore = true
    }
  }, [apiFetch, showToast])

  // Dynamic KPI Metrics
  const totalRooms = rooms.length
  const totalBeds = beds.length
  const availableBeds = beds.filter((b) => b.status === 'AVAILABLE').length
  const occupiedBeds = beds.filter((b) => b.status === 'OCCUPIED').length
  const maintenanceBeds = beds.filter((b) => b.status === 'MAINTENANCE').length
  const activePatients = patients.filter((p) => p.status === 'ADMITTED').length

  // Wards list for filter
  const wards = Array.from(new Set(rooms.map((r) => r.wardDepartment).filter(Boolean))).sort()

  // ----------------------------------------------------------------
  // Handlers: Room & Bed Admin
  // ----------------------------------------------------------------
  const handleCreateRoom = async (e) => {
    e.preventDefault()
    if (!newRoomNumber.trim() || !newRoomType || !newRoomWard.trim()) {
      showToast('Please fill out all room fields', 'error')
      return
    }

    setCreatingRoom(true)
    try {
      const res = await apiFetch('/api/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          roomNumber: newRoomNumber.trim(),
          roomType: newRoomType,
          wardDepartment: newRoomWard.trim(),
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Room ${data.data.roomNumber} created successfully!`, 'success')
        setNewRoomNumber('')
        setNewRoomType('')
        setNewRoomWard('')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to create room', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setCreatingRoom(false)
    }
  }

  const handleCreateBed = async (e) => {
    e.preventDefault()
    if (!newBedRoomId || !newBedNumber.trim()) {
      showToast('Please select a target room and bed number', 'error')
      return
    }

    setCreatingBed(true)
    try {
      const res = await apiFetch('/api/beds', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          roomId: parseInt(newBedRoomId, 10),
          bedNumber: newBedNumber.trim(),
          status: newBedStatus,
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Bed ${data.data.bedNumber} added successfully!`, 'success')
        setNewBedNumber('')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to create bed', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setCreatingBed(false)
    }
  }

  const updateBedStatus = async (bedId, newStatus) => {
    try {
      const res = await apiFetch(
        `/api/beds/${bedId}/status?status=${encodeURIComponent(newStatus)}`,
        { method: 'PATCH' }
      )
      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Bed status updated to ${newStatus}`, 'success')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to update bed status', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  // ----------------------------------------------------------------
  // Handlers: Patients Admission & Discharge
  // ----------------------------------------------------------------
  const handleAdmitPatient = async (e) => {
    e.preventDefault()
    if (
      !patientCode.trim() ||
      !patientName.trim() ||
      !patientAge ||
      !patientDiagnosis.trim() ||
      !admissionBedId
    ) {
      showToast('Please fill out all required admission fields', 'error')
      return
    }

    setAdmittingPatient(true)
    try {
      const res = await apiFetch('/api/patients/admit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          patientCode: patientCode.trim(),
          name: patientName.trim(),
          age: parseInt(patientAge, 10),
          gender: patientGender,
          diagnosis: patientDiagnosis.trim(),
          bedId: parseInt(admissionBedId, 10),
          createPortalAccount: createAccountOnAdmit,
          portalUsername: portalUsername.trim() || undefined,
          portalPassword: portalPassword || undefined,
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        showToast(
          `Patient ${data.data.name} admitted to Bed ${data.data.bedNumber}!`,
          'success'
        )
        setPatientCode('')
        setPatientName('')
        setPatientAge('')
        setPatientDiagnosis('')
        setAdmissionBedId('')
        setCreateAccountOnAdmit(false)
        setPortalUsername('')
        setPortalPassword('')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to admit patient', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setAdmittingPatient(false)
    }
  }

  const handleProvisionAccount = async (e) => {
    e.preventDefault()
    if (!provUsername.trim() || !provPassword || provPassword.length < 6) {
      showToast('Username and password (min 6 characters) are required', 'error')
      return
    }

    setSubmittingProv(true)
    try {
      const res = await apiFetch(`/api/patients/${provisioningPatient.id}/create-account`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: provUsername.trim(),
          password: provPassword,
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Portal account @${provUsername.trim()} created for ${provisioningPatient.name}!`, 'success')
        setProvisioningPatient(null)
        setProvUsername('')
        setProvPassword('')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to create portal account', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setSubmittingProv(false)
    }
  }

  const handleDischargePatient = async (patientId, patientName, bedNumber) => {
    if (
      !window.confirm(
        `Are you sure you want to discharge patient ${patientName}? Bed ${bedNumber} will become AVAILABLE.`
      )
    ) {
      return
    }

    try {
      const res = await apiFetch(`/api/patients/${patientId}/discharge`, {
        method: 'POST',
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast(
          `Patient ${data.data.name} discharged successfully. Bed released!`,
          'success'
        )
        loadAllData()
      } else {
        showToast(data.message || 'Failed to discharge patient', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  // ----------------------------------------------------------------
  // Handlers: Smart Bed Recommendations
  // ----------------------------------------------------------------
  const handleRecommendBeds = async (e) => {
    e.preventDefault()
    setLoadingRecs(true)
    setRecommendations(null)

    try {
      const res = await apiFetch('/api/allocations/recommend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          roomType: recRoomType,
          wardDepartment: recWard.trim(),
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        setRecommendations(data.data || [])
      } else {
        showToast(data.message || 'Failed to fetch recommendations', 'error')
        setRecommendations([])
      }
    } catch (err) {
      showToast(err.message, 'error')
      setRecommendations([])
    } finally {
      setLoadingRecs(false)
    }
  }

  const selectRecommendedBed = (bedId) => {
    setActiveTab('patients')
    setAdmissionBedId(String(bedId))
    showToast('Recommended bed selected in Admission form!', 'info')
  }

  const handleApproveDoctor = async (doctorId, doctorName) => {
    try {
      setActionDoctorId(doctorId)
      const res = await apiFetch(`/api/auth/doctors/${doctorId}/approve`, {
        method: 'PATCH',
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Doctor account for '${doctorName || 'Doctor'}' approved and activated successfully.`, 'success')
        loadPendingDoctors()
      } else {
        showToast(data.message || 'Failed to approve doctor', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error communicating with server', 'error')
    } finally {
      setActionDoctorId(null)
    }
  }

  const handleRejectDoctor = async (doctorId, doctorName) => {
    try {
      setActionDoctorId(doctorId)
      const res = await apiFetch(`/api/auth/doctors/${doctorId}/reject`, {
        method: 'PATCH',
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Doctor registration for '${doctorName || 'Doctor'}' has been rejected.`, 'info')
        loadPendingDoctors()
      } else {
        showToast(data.message || 'Failed to reject doctor', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error communicating with server', 'error')
    } finally {
      setActionDoctorId(null)
    }
  }

  const handleLogoutClick = async () => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      })
    } catch (err) {
      console.error('Logout error:', err)
    } finally {
      if (onLogout) onLogout()
    }
  }

  // Filtered rooms for Live Matrix View
  let filteredRooms = rooms
  if (filterWard) {
    filteredRooms = filteredRooms.filter(
      (r) => r.wardDepartment.toLowerCase() === filterWard.toLowerCase()
    )
  }

  const titles = {
    overview: {
      title: 'Overview & Live Bed Matrix',
      desc: 'Real-time status of hospital rooms, capacity, and bed occupancies',
    },
    'rooms-beds': {
      title: 'Room & Bed Administration',
      desc: 'Create and configure hospital rooms and beds inventory',
    },
    patients: {
      title: 'Patient Admissions & Discharges',
      desc: 'Admit incoming patients, assign beds, and handle discharges',
    },
    recommendation: {
      title: 'Smart Bed Recommendation Engine',
      desc: 'Intelligently match patients to optimal available beds by care level',
    },
    analytics: {
      title: 'Hospital Bed Utilization & Analytics',
      desc: 'Real-time occupancy rates, ward-level utilization, and length-of-stay metrics',
    },
    doctors: {
      title: 'Doctor Registrations & Credentials Review',
      desc: 'Verify medical registrations and approve or reject doctor portal access',
    },
  }

  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="logo-icon">🏥</div>
          <div>
            <h2>SmartHospital</h2>
            <span className="subtitle">Admin Operations & Bed Control</span>
          </div>
        </div>

        <nav className="nav-menu">
          <button
            className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            <span>📊</span> Overview & Live Beds
          </button>
          <button
            className={`nav-item ${activeTab === 'rooms-beds' ? 'active' : ''}`}
            onClick={() => setActiveTab('rooms-beds')}
          >
            <span>🛏️</span> Room & Bed Admin
          </button>
          <button
            className={`nav-item ${activeTab === 'patients' ? 'active' : ''}`}
            onClick={() => setActiveTab('patients')}
          >
            <span>🧑‍⚕️</span> Patient Admissions
          </button>
          <button
            className={`nav-item ${activeTab === 'recommendation' ? 'active' : ''}`}
            onClick={() => setActiveTab('recommendation')}
          >
            <span>⚡</span> Smart Recommendation
          </button>
          <button
            className={`nav-item ${activeTab === 'analytics' ? 'active' : ''}`}
            onClick={() => setActiveTab('analytics')}
          >
            <span>📈</span> Utilization & Analytics
          </button>
          <button
            className={`nav-item ${activeTab === 'doctors' ? 'active' : ''}`}
            onClick={() => setActiveTab('doctors')}
          >
            <span>👨‍⚕️</span> Doctor Approvals
            {pendingDoctors.length > 0 && (
              <span
                style={{
                  marginLeft: 'auto',
                  backgroundColor: '#dc2626',
                  color: '#ffffff',
                  fontSize: '0.72rem',
                  fontWeight: '700',
                  padding: '0.15rem 0.5rem',
                  borderRadius: '9999px',
                }}
              >
                {pendingDoctors.length}
              </span>
            )}
          </button>
        </nav>

        <div className="sidebar-footer">
          <span className="pulse-dot"></span>
          <span>Backend Connected (Port 8080)</span>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        {/* Topbar */}
        <header className="topbar">
          <div className="page-title">
            <h1>{titles[activeTab]?.title}</h1>
            <p>{titles[activeTab]?.desc}</p>
          </div>

          <div className="topbar-actions">
            <div className="user-badge">
              <span className="user-avatar">👤</span>
              <div className="user-details">
                <span className="user-name">
                  {user?.fullName || user?.username || 'Administrator'}
                </span>
                <span className="user-role-tag">{user?.role || 'ROLE_ADMIN'}</span>
              </div>
            </div>

            <button
              className="btn btn-secondary"
              onClick={loadAllData}
              disabled={loading}
              title="Refresh live data from server"
            >
              <span>🔄</span> Refresh
            </button>

            <button
              className="btn btn-secondary"
              onClick={handleLogoutClick}
              title="Sign out of system"
            >
              <span>🚪</span> Logout
            </button>
          </div>
        </header>

        {/* Dynamic KPI Summary Cards */}
        <section className="kpi-grid">
          <div className="kpi-card">
            <div className="kpi-icon blue">🚪</div>
            <div>
              <div className="kpi-label">Total Rooms</div>
              <div className="kpi-value">{totalRooms}</div>
            </div>
          </div>
          <div className="kpi-card">
            <div className="kpi-icon indigo">🛏️</div>
            <div>
              <div className="kpi-label">Total Beds</div>
              <div className="kpi-value">{totalBeds}</div>
            </div>
          </div>
          <div className="kpi-card">
            <div className="kpi-icon green">✅</div>
            <div>
              <div className="kpi-label">Available Beds</div>
              <div className="kpi-value green-text">{availableBeds}</div>
            </div>
          </div>
          <div className="kpi-card">
            <div className="kpi-icon red">🛌</div>
            <div>
              <div className="kpi-label">Occupied Beds</div>
              <div className="kpi-value red-text">{occupiedBeds}</div>
            </div>
          </div>
          <div className="kpi-card">
            <div className="kpi-icon amber">🛠️</div>
            <div>
              <div className="kpi-label">In Maintenance</div>
              <div className="kpi-value amber-text">{maintenanceBeds}</div>
            </div>
          </div>
          <div className="kpi-card">
            <div className="kpi-icon purple">🧑‍⚕️</div>
            <div>
              <div className="kpi-label">Active Inpatients</div>
              <div className="kpi-value">{activePatients}</div>
            </div>
          </div>
        </section>

        {/* Tab 1: Live Overview & Bed Matrix */}
        {activeTab === 'overview' && (
          <section>
            <div className="section-header">
              <h2>Live Room & Bed Matrix</h2>
              <div className="filter-group">
                <label htmlFor="filter-ward">Ward:</label>
                <select
                  id="filter-ward"
                  value={filterWard}
                  onChange={(e) => setFilterWard(e.target.value)}
                >
                  <option value="">All Wards</option>
                  {wards.map((w) => (
                    <option key={w} value={w}>
                      {w}
                    </option>
                  ))}
                </select>

                <label htmlFor="filter-status">Status:</label>
                <select
                  id="filter-status"
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                >
                  <option value="">All Statuses</option>
                  <option value="AVAILABLE">Available Only</option>
                  <option value="OCCUPIED">Occupied Only</option>
                  <option value="MAINTENANCE">Maintenance Only</option>
                </select>
              </div>
            </div>

            {filteredRooms.length === 0 ? (
              <div className="card text-center text-muted">
                No rooms match the selected filter criteria.
              </div>
            ) : (
              <div className="room-grid">
                {filteredRooms.map((room) => {
                  let roomBeds = beds.filter((b) => b.roomId === room.id)
                  if (filterStatus) {
                    roomBeds = roomBeds.filter((b) => b.status === filterStatus)
                  }

                  return (
                    <div key={room.id} className="room-card">
                      <div className="room-card-header">
                        <div>
                          <div className="room-number-badge">
                            Room {room.roomNumber}
                          </div>
                          <div className="room-meta">
                            {room.roomType} • {room.wardDepartment}
                          </div>
                        </div>
                        <span className="room-capacity-badge">
                          {room.availableBeds}/{room.totalBeds} Available
                        </span>
                      </div>

                      <div className="bed-chips-container">
                        {roomBeds.length === 0 ? (
                          <span className="text-muted" style={{ fontSize: '0.8rem' }}>
                            No beds matching status
                          </span>
                        ) : (
                          roomBeds.map((bed) => (
                            <div
                              key={bed.id}
                              className={`bed-chip status-${bed.status}`}
                              title={`Bed #${bed.bedNumber} - ${bed.status}. Click to cycle status.`}
                              onClick={() => {
                                if (bed.status === 'OCCUPIED') {
                                  showToast(
                                    `Cannot change status of Bed ${bed.bedNumber}: A patient is currently admitted to this bed. Discharge the patient first.`,
                                    'error'
                                  )
                                  return
                                }
                                const next =
                                  bed.status === 'AVAILABLE'
                                    ? 'MAINTENANCE'
                                    : 'AVAILABLE'
                                updateBedStatus(bed.id, next)
                              }}
                            >
                              <span>🛏️</span>
                              <span>{bed.bedNumber}</span>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </section>
        )}

        {/* Tab 2: Room & Bed Administration */}
        {activeTab === 'rooms-beds' && (
          <section>
            <div className="forms-split-grid">
              {/* Create Room Form */}
              <div className="card form-card">
                <h3>➕ Register New Room</h3>
                <p className="form-desc">
                  Define a new hospital room with care type and department.
                </p>
                <form onSubmit={handleCreateRoom}>
                  <div className="form-group">
                    <label htmlFor="create-room-num">Room Number *</label>
                    <input
                      id="create-room-num"
                      type="text"
                      placeholder="e.g. 101-ICU, R-204"
                      value={newRoomNumber}
                      onChange={(e) => setNewRoomNumber(e.target.value)}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="create-room-type">Room Type *</label>
                    <select
                      id="create-room-type"
                      value={newRoomType}
                      onChange={(e) => setNewRoomType(e.target.value)}
                      required
                    >
                      <option value="">Select Room Type</option>
                      <option value="ICU">ICU (Intensive Care)</option>
                      <option value="ICU-STEPDOWN">ICU-Stepdown</option>
                      <option value="GENERAL">General Ward</option>
                      <option value="SEMI_PRIVATE">Semi-Private Room</option>
                      <option value="PRIVATE">Private Room</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label htmlFor="create-room-ward">Ward / Department *</label>
                    <input
                      id="create-room-ward"
                      type="text"
                      placeholder="e.g. Critical Care, Cardiology, Surgery"
                      value={newRoomWard}
                      onChange={(e) => setNewRoomWard(e.target.value)}
                      required
                    />
                  </div>
                  <button
                    type="submit"
                    className="btn btn-primary full-width"
                    disabled={creatingRoom}
                  >
                    {creatingRoom ? 'Creating Room...' : 'Create Room'}
                  </button>
                </form>
              </div>

              {/* Create Bed Form */}
              <div className="card form-card">
                <h3>➕ Add Bed to Room</h3>
                <p className="form-desc">
                  Assign a new bed to an existing room with default status.
                </p>
                <form onSubmit={handleCreateBed}>
                  <div className="form-group">
                    <label htmlFor="create-bed-room">Target Room *</label>
                    <select
                      id="create-bed-room"
                      value={newBedRoomId}
                      onChange={(e) => setNewBedRoomId(e.target.value)}
                      required
                    >
                      <option value="">Select Target Room</option>
                      {rooms.map((r) => (
                        <option key={r.id} value={r.id}>
                          Room {r.roomNumber} ({r.roomType} - {r.wardDepartment})
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label htmlFor="create-bed-num">Bed Number / Label *</label>
                    <input
                      id="create-bed-num"
                      type="text"
                      placeholder="e.g. BED-1, B-02"
                      value={newBedNumber}
                      onChange={(e) => setNewBedNumber(e.target.value)}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="create-bed-init-status">Initial Status</label>
                    <select
                      id="create-bed-init-status"
                      value={newBedStatus}
                      onChange={(e) => setNewBedStatus(e.target.value)}
                    >
                      <option value="AVAILABLE">AVAILABLE</option>
                      <option value="MAINTENANCE">MAINTENANCE</option>
                    </select>
                  </div>
                  <button
                    type="submit"
                    className="btn btn-primary full-width"
                    disabled={creatingBed}
                  >
                    {creatingBed ? 'Adding Bed...' : 'Add Bed'}
                  </button>
                </form>
              </div>
            </div>

            {/* Inventory Table */}
            <div className="card">
              <h3>All Hospital Beds Inventory</h3>
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Bed ID</th>
                      <th>Bed Number</th>
                      <th>Room</th>
                      <th>Room Type</th>
                      <th>Ward</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {beds.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="text-center text-muted">
                          No beds registered yet.
                        </td>
                      </tr>
                    ) : (
                      beds.map((bed) => (
                        <tr key={bed.id}>
                          <td>
                            <strong>#{bed.id}</strong>
                          </td>
                          <td>{bed.bedNumber}</td>
                          <td>Room {bed.roomNumber}</td>
                          <td>{bed.roomType || rooms.find((r) => r.id === bed.roomId)?.roomType || 'Standard'}</td>
                          <td>{bed.wardDepartment}</td>
                          <td>
                            <span className={`status-badge ${bed.status}`}>
                              {bed.status}
                            </span>
                          </td>
                          <td>
                            <div className="status-actions">
                              <button
                                className="btn btn-secondary btn-sm"
                                onClick={() => updateBedStatus(bed.id, 'AVAILABLE')}
                                disabled={bed.status === 'AVAILABLE' || bed.status === 'OCCUPIED'}
                                title={
                                  bed.status === 'OCCUPIED'
                                    ? 'Cannot set OCCUPIED bed to AVAILABLE while patient is admitted'
                                    : 'Set Available'
                                }
                              >
                                Set Available
                              </button>
                              <button
                                className="btn btn-secondary btn-sm"
                                onClick={() => updateBedStatus(bed.id, 'MAINTENANCE')}
                                disabled={bed.status === 'MAINTENANCE' || bed.status === 'OCCUPIED'}
                                title={
                                  bed.status === 'OCCUPIED'
                                    ? 'Cannot set OCCUPIED bed to MAINTENANCE while patient is admitted'
                                    : 'Set Maint.'
                                }
                              >
                                Set Maint.
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {/* Tab 3: Patient Admission & Discharge */}
        {activeTab === 'patients' && (
          <section>
            {/* Admission Form */}
            <div className="card form-card">
              <h3>🏥 Patient Admission Desk</h3>
              <p className="form-desc">
                Admit an incoming patient to an available bed. The bed will
                automatically become OCCUPIED.
              </p>
              <form onSubmit={handleAdmitPatient}>
                <div className="form-row">
                  <div className="form-group col-4">
                    <label htmlFor="admit-code">Patient ID / MRN *</label>
                    <input
                      id="admit-code"
                      type="text"
                      placeholder="e.g. PAT-1001"
                      value={patientCode}
                      onChange={(e) => setPatientCode(e.target.value)}
                      required
                    />
                  </div>
                  <div className="form-group col-4">
                    <label htmlFor="admit-name">Patient Full Name *</label>
                    <input
                      id="admit-name"
                      type="text"
                      placeholder="e.g. John Doe"
                      value={patientName}
                      onChange={(e) => setPatientName(e.target.value)}
                      required
                    />
                  </div>
                  <div className="form-group col-2">
                    <label htmlFor="admit-age">Age *</label>
                    <input
                      id="admit-age"
                      type="number"
                      min="1"
                      max="130"
                      placeholder="45"
                      value={patientAge}
                      onChange={(e) => setPatientAge(e.target.value)}
                      required
                    />
                  </div>
                  <div className="form-group col-2">
                    <label htmlFor="admit-gender">Gender *</label>
                    <select
                      id="admit-gender"
                      value={patientGender}
                      onChange={(e) => setPatientGender(e.target.value)}
                      required
                    >
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group col-6">
                    <label htmlFor="admit-diag">Diagnosis / Clinical Condition *</label>
                    <input
                      id="admit-diag"
                      type="text"
                      placeholder="e.g. Acute Coronary Syndrome"
                      value={patientDiagnosis}
                      onChange={(e) => setPatientDiagnosis(e.target.value)}
                      required
                    />
                  </div>
                  <div className="form-group col-6">
                    <label htmlFor="admit-bed">Select Available Bed *</label>
                    <select
                      id="admit-bed"
                      value={admissionBedId}
                      onChange={(e) => setAdmissionBedId(e.target.value)}
                      required
                    >
                      <option value="">Select an available bed...</option>
                      {beds
                        .filter((b) => b.status === 'AVAILABLE')
                        .map((b) => (
                          <option key={b.id} value={b.id}>
                            Bed {b.bedNumber} — Room {b.roomNumber} ({b.wardDepartment})
                          </option>
                        ))}
                    </select>
                  </div>
                </div>

                <div style={{ marginTop: '0.75rem', marginBottom: '1rem', padding: '0.75rem 1rem', background: '#f8fafc', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontWeight: 600 }}>
                    <input
                      type="checkbox"
                      checked={createAccountOnAdmit}
                      onChange={(e) => setCreateAccountOnAdmit(e.target.checked)}
                    />
                    <span>Provision Patient Portal Account (Emergency / Critical Care)</span>
                  </label>
                  {createAccountOnAdmit && (
                    <div className="form-row" style={{ marginTop: '0.75rem' }}>
                      <div className="form-group col-6">
                        <label>Portal Username (leave blank to default to patient code)</label>
                        <input
                          type="text"
                          placeholder="e.g. jdoe_patient"
                          value={portalUsername}
                          onChange={(e) => setPortalUsername(e.target.value)}
                        />
                      </div>
                      <div className="form-group col-6">
                        <label>Portal Password * (min 6 characters)</label>
                        <input
                          type="password"
                          placeholder="Create portal password"
                          value={portalPassword}
                          onChange={(e) => setPortalPassword(e.target.value)}
                          required={createAccountOnAdmit}
                        />
                      </div>
                    </div>
                  )}
                </div>

                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={admittingPatient}
                >
                  {admittingPatient ? 'Admitting Patient...' : 'Admit Patient & Assign Bed'}
                </button>
              </form>
            </div>

            {/* Active Inpatients Table */}
            <div className="card">
              <h3>Currently Admitted Patients (Active Inpatients)</h3>
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Patient Code</th>
                      <th>Name</th>
                      <th>Age / Gender</th>
                      <th>Diagnosis</th>
                      <th>Assigned Bed</th>
                      <th>Room & Ward</th>
                      <th>Admission Date</th>
                      <th>Portal Access</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {patients.filter((p) => p.status === 'ADMITTED').length === 0 ? (
                      <tr>
                        <td colSpan="10" className="text-center text-muted">
                          No active inpatients currently admitted.
                        </td>
                      </tr>
                    ) : (
                      patients
                        .filter((p) => p.status === 'ADMITTED')
                        .map((p) => (
                          <tr key={p.id}>
                            <td>
                              <strong>#{p.id}</strong>
                            </td>
                            <td>
                              <code>{p.patientCode}</code>
                            </td>
                            <td>
                              <strong>{p.name}</strong>
                            </td>
                            <td>
                              {p.age} / {p.gender}
                            </td>
                            <td>{p.diagnosis || '-'}</td>
                            <td>
                              <span className="status-badge OCCUPIED">
                                {p.bedNumber || 'Assigned'}
                              </span>
                            </td>
                            <td>
                              Room {p.roomNumber || '-'} ({p.wardDepartment || '-'})
                            </td>
                            <td>
                              {p.admissionDate
                                ? new Date(p.admissionDate).toLocaleString()
                                : '-'}
                            </td>
                            <td>
                              {p.username ? (
                                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', padding: '0.2rem 0.6rem', borderRadius: '9999px', background: '#dcfce7', color: '#166534', fontSize: '0.8rem', fontWeight: 600 }}>
                                  ✅ @{p.username}
                                </span>
                              ) : (
                                <button
                                  type="button"
                                  className="btn btn-secondary btn-sm"
                                  style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem' }}
                                  onClick={() => {
                                    setProvisioningPatient(p)
                                    setProvUsername(p.patientCode ? p.patientCode.toLowerCase().replace(/[^a-z0-9]/g, '') : '')
                                    setProvPassword('')
                                  }}
                                  title="Provision login credentials for this admitted patient"
                                >
                                  + Create Account
                                </button>
                              )}
                            </td>
                            <td>
                              <button
                                className="btn btn-danger btn-sm"
                                onClick={() =>
                                  handleDischargePatient(p.id, p.name, p.bedNumber)
                                }
                              >
                                Discharge
                              </button>
                            </td>
                          </tr>
                        ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Discharged Patients History */}
            <div className="card">
              <h3>Discharged Patients History</h3>
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Patient Code</th>
                      <th>Name</th>
                      <th>Diagnosis</th>
                      <th>Discharged From Bed</th>
                      <th>Admission Date</th>
                      <th>Discharge Date</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {patients.filter((p) => p.status === 'DISCHARGED').length === 0 ? (
                      <tr>
                        <td colSpan="8" className="text-center text-muted">
                          No discharged patients on record.
                        </td>
                      </tr>
                    ) : (
                      patients
                        .filter((p) => p.status === 'DISCHARGED')
                        .map((p) => (
                          <tr key={p.id}>
                            <td>#{p.id}</td>
                            <td>
                              <code>{p.patientCode}</code>
                            </td>
                            <td>{p.name}</td>
                            <td>{p.diagnosis || '-'}</td>
                            <td>{p.bedNumber || 'Released'}</td>
                            <td>
                              {p.admissionDate
                                ? new Date(p.admissionDate).toLocaleString()
                                : '-'}
                            </td>
                            <td>
                              {p.dischargeDate
                                ? new Date(p.dischargeDate).toLocaleString()
                                : '-'}
                            </td>
                            <td>
                              <span className="status-badge DISCHARGED">
                                DISCHARGED
                              </span>
                            </td>
                          </tr>
                        ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {/* Tab 4: Smart Bed Recommendation */}
        {activeTab === 'recommendation' && (
          <section>
            <div className="card form-card">
              <h3>⚡ Rule-Based Bed Recommendation Engine</h3>
              <p className="form-desc">
                Intelligently ranks available beds by medical care suitability and
                ward proximity.
              </p>
              <form onSubmit={handleRecommendBeds}>
                <div className="form-row">
                  <div className="form-group col-6">
                    <label htmlFor="rec-room-type-select">
                      Required Room / Care Level *
                    </label>
                    <select
                      id="rec-room-type-select"
                      value={recRoomType}
                      onChange={(e) => setRecRoomType(e.target.value)}
                      required
                    >
                      <option value="ICU">ICU (Intensive Care Unit)</option>
                      <option value="ICU-STEPDOWN">ICU-Stepdown</option>
                      <option value="GENERAL">General Ward</option>
                      <option value="SEMI_PRIVATE">Semi-Private</option>
                      <option value="PRIVATE">Private</option>
                    </select>
                  </div>
                  <div className="form-group col-6">
                    <label htmlFor="rec-ward-input">
                      Preferred Department / Ward
                    </label>
                    <input
                      id="rec-ward-input"
                      type="text"
                      placeholder="e.g. Critical Care, Cardiology, Surgery"
                      value={recWard}
                      onChange={(e) => setRecWard(e.target.value)}
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loadingRecs}
                >
                  {loadingRecs
                    ? 'Evaluating Hospital Beds...'
                    : 'Find Optimal Available Beds'}
                </button>
              </form>
            </div>

            <div className="card">
              <h3>Ranked Bed Recommendations</h3>
              {recommendations === null ? (
                <p className="text-muted" style={{ marginTop: '0.5rem' }}>
                  Enter care criteria above and click &quot;Find Optimal Available
                  Beds&quot; to view ranked recommendations.
                </p>
              ) : recommendations.length === 0 ? (
                <p className="text-muted" style={{ marginTop: '0.5rem' }}>
                  No available beds match the specified care level and department.
                </p>
              ) : (
                <div className="recommendations-container">
                  {recommendations.map((rec) => {
                    const isExact = rec.matchScore === 100
                    return (
                      <div
                        key={rec.bedId}
                        className={`recommendation-card score-${rec.matchScore}`}
                      >
                        <div className="rec-info">
                          <span
                            className={`rec-score-badge ${isExact ? 'exact' : 'clinical'}`}
                          >
                            {isExact
                              ? '⭐ 100% Exact Match'
                              : '⚕️ 75% Cross-Ward Clinical Match'}
                          </span>
                          <h4>
                            Bed {rec.bedNumber} — Room {rec.roomNumber} ({rec.roomType})
                          </h4>
                          <p>
                            <strong>Ward:</strong> {rec.wardDepartment}
                          </p>
                          <p className="text-muted">
                            <em>{rec.matchReason}</em>
                          </p>
                        </div>
                        <div>
                          <button
                            className="btn btn-primary"
                            onClick={() => selectRecommendedBed(rec.bedId)}
                          >
                            Select & Admit Here
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </section>
        )}

        {/* Tab 5: Analytics & Bed Utilization */}
        {activeTab === 'analytics' && (
          <section>
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <h3 style={{ margin: 0 }}>📊 Hospital-Wide Bed Utilization Metrics</h3>
                  <p className="form-desc" style={{ margin: '0.25rem 0 0 0' }}>
                    Real-time occupancy rates, clinical capacity thresholds, and inpatient turnover
                  </p>
                </div>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                  <span style={{ fontSize: '1.5rem', fontWeight: 800, color: (analyticsData?.overallOccupancyRate || 0) > 85 ? '#dc2626' : (analyticsData?.overallOccupancyRate || 0) > 70 ? '#d97706' : '#16a34a' }}>
                    {analyticsData?.overallOccupancyRate != null ? `${analyticsData.overallOccupancyRate}%` : `${totalBeds > 0 ? ((occupiedBeds * 100) / totalBeds).toFixed(1) : 0}%`} Occupancy
                  </span>
                </div>
              </div>

              {/* High-level Analytics KPIs */}
              <div className="kpi-grid" style={{ marginTop: '1.5rem' }}>
                <div className="kpi-card">
                  <div className="kpi-icon blue">🏥</div>
                  <div>
                    <div className="kpi-label">Operational Capacity</div>
                    <div className="kpi-value">{analyticsData?.totalBeds ?? totalBeds} Beds</div>
                  </div>
                </div>
                <div className="kpi-card">
                  <div className="kpi-icon red">🛌</div>
                  <div>
                    <div className="kpi-label">Active Inpatients</div>
                    <div className="kpi-value red-text">{analyticsData?.activePatients ?? activePatients}</div>
                  </div>
                </div>
                <div className="kpi-card">
                  <div className="kpi-icon green">✅</div>
                  <div>
                    <div className="kpi-label">Available Capacity</div>
                    <div className="kpi-value green-text">{analyticsData?.availableBeds ?? availableBeds}</div>
                  </div>
                </div>
                <div className="kpi-card">
                  <div className="kpi-icon amber">🛠️</div>
                  <div>
                    <div className="kpi-label">Maintenance Beds</div>
                    <div className="kpi-value amber-text">{analyticsData?.maintenanceBeds ?? maintenanceBeds}</div>
                  </div>
                </div>
                <div className="kpi-card">
                  <div className="kpi-icon purple">⏱️</div>
                  <div>
                    <div className="kpi-label">Avg Length of Stay (ALOS)</div>
                    <div className="kpi-value purple-text">
                      {analyticsData?.averageLengthOfStayHours != null ? `${analyticsData.averageLengthOfStayHours} hrs` : '—'}
                    </div>
                  </div>
                </div>
                <div className="kpi-card">
                  <div className="kpi-icon indigo">📋</div>
                  <div>
                    <div className="kpi-label">Total Discharged</div>
                    <div className="kpi-value indigo-text">{analyticsData?.dischargedPatients ?? 0}</div>
                  </div>
                </div>
              </div>
            </div>

            {/* Ward-level Breakdown */}
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <h3>🏥 Ward & Department Utilization Breakdown</h3>
              <p className="form-desc">
                Current capacity and bed utilization segmented across hospital wings and care wards
              </p>
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Ward / Department</th>
                      <th>Rooms</th>
                      <th>Total Beds</th>
                      <th>Occupied</th>
                      <th>Available</th>
                      <th>Maintenance</th>
                      <th>Occupancy Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(!analyticsData?.wardBreakdown || analyticsData.wardBreakdown.length === 0) ? (
                      <tr>
                        <td colSpan="7" className="text-center text-muted">
                          No ward data available.
                        </td>
                      </tr>
                    ) : (
                      analyticsData.wardBreakdown.map((w) => (
                        <tr key={w.wardName}>
                          <td><strong>{w.wardName}</strong></td>
                          <td>{w.totalRooms}</td>
                          <td>{w.totalBeds}</td>
                          <td style={{ color: '#dc2626', fontWeight: 600 }}>{w.occupiedBeds}</td>
                          <td style={{ color: '#16a34a', fontWeight: 600 }}>{w.availableBeds}</td>
                          <td style={{ color: '#d97706' }}>{w.maintenanceBeds}</td>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                              <div style={{ flex: 1, height: '8px', background: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                                <div
                                  style={{
                                    width: `${Math.min(100, w.occupancyRate)}%`,
                                    height: '100%',
                                    background: w.occupancyRate > 85 ? '#dc2626' : w.occupancyRate > 70 ? '#d97706' : '#16a34a',
                                  }}
                                />
                              </div>
                              <span style={{ fontWeight: 600, minWidth: '45px', fontSize: '0.85rem' }}>{w.occupancyRate}%</span>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Room Type Breakdown */}
            <div className="card">
              <h3>🛏️ Care Level & Room Type Distribution</h3>
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Room Type / Care Level</th>
                      <th>Total Beds</th>
                      <th>Occupied</th>
                      <th>Available</th>
                      <th>Maintenance</th>
                      <th>Occupancy Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(!analyticsData?.roomTypeBreakdown || analyticsData.roomTypeBreakdown.length === 0) ? (
                      <tr>
                        <td colSpan="6" className="text-center text-muted">
                          No room type distribution data available.
                        </td>
                      </tr>
                    ) : (
                      analyticsData.roomTypeBreakdown.map((t) => (
                        <tr key={t.roomType}>
                          <td><strong>{t.roomType}</strong></td>
                          <td>{t.totalBeds}</td>
                          <td style={{ color: '#dc2626', fontWeight: 600 }}>{t.occupiedBeds}</td>
                          <td style={{ color: '#16a34a', fontWeight: 600 }}>{t.availableBeds}</td>
                          <td style={{ color: '#d97706' }}>{t.maintenanceBeds}</td>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                              <div style={{ flex: 1, height: '8px', background: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                                <div
                                  style={{
                                    width: `${Math.min(100, t.occupancyRate)}%`,
                                    height: '100%',
                                    background: t.occupancyRate > 85 ? '#dc2626' : t.occupancyRate > 70 ? '#d97706' : '#16a34a',
                                  }}
                                />
                              </div>
                              <span style={{ fontWeight: 600, minWidth: '45px', fontSize: '0.85rem' }}>{t.occupancyRate}%</span>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {/* Tab 6: Doctor Registrations & Approvals */}
        {activeTab === 'doctors' && (
          <section>
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginBottom: '1rem',
                  paddingBottom: '0.75rem',
                  borderBottom: '1px solid var(--border-color)',
                }}
              >
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.25rem', color: 'var(--text-primary)' }}>
                    Pending Doctor Registration Requests
                  </h3>
                  <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    Review submitted credentials, specialization, and official medical registration numbers.
                  </p>
                </div>
                <button
                  className="btn btn-secondary"
                  onClick={loadPendingDoctors}
                  disabled={loadingDoctors}
                  style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
                >
                  <span>🔄</span> {loadingDoctors ? 'Refreshing...' : 'Refresh Requests'}
                </button>
              </div>

              {pendingDoctors.length === 0 ? (
                <div
                  style={{
                    textAlign: 'center',
                    padding: '3rem 1.5rem',
                    color: 'var(--text-secondary)',
                    backgroundColor: '#f8fafc',
                    borderRadius: 'var(--radius-md)',
                    border: '1px dashed #cbd5e1',
                  }}
                >
                  <div style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>✅</div>
                  <h4 style={{ margin: '0 0 0.5rem 0', color: 'var(--text-primary)', fontSize: '1.1rem' }}>
                    No Pending Doctor Registrations
                  </h4>
                  <p style={{ margin: 0, fontSize: '0.9rem', maxWidth: '480px', marginInline: 'auto' }}>
                    All doctor registration applications have been reviewed. When new doctors register on the portal, their credentials will appear here for administrative verification.
                  </p>
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Doctor Name</th>
                        <th>Username & Contact</th>
                        <th>Specialization</th>
                        <th>Qualifications</th>
                        <th>Medical Reg. No.</th>
                        <th>Applied On</th>
                        <th style={{ textAlign: 'center' }}>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingDoctors.map((doc) => (
                        <tr key={doc.id}>
                          <td>
                            <strong>Dr. {doc.fullName}</strong>
                          </td>
                          <td>
                            <div style={{ fontWeight: 600 }}>@{doc.username}</div>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                              {doc.email || 'No email provided'}
                            </div>
                          </td>
                          <td>
                            <span
                              style={{
                                display: 'inline-block',
                                padding: '0.2rem 0.6rem',
                                borderRadius: '9999px',
                                fontSize: '0.75rem',
                                fontWeight: '600',
                                backgroundColor: '#e0f2fe',
                                color: '#0369a1',
                              }}
                            >
                              {doc.specialization || 'General'}
                            </span>
                          </td>
                          <td>{doc.qualifications || 'N/A'}</td>
                          <td>
                            <code
                              style={{
                                backgroundColor: '#f1f5f9',
                                padding: '0.2rem 0.4rem',
                                borderRadius: '4px',
                                fontSize: '0.85rem',
                                color: '#0f172a',
                              }}
                            >
                              {doc.medicalRegistrationNumber || 'N/A'}
                            </code>
                          </td>
                          <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                            {doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : 'Recent'}
                          </td>
                          <td>
                            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                              <button
                                className="btn btn-primary"
                                style={{
                                  backgroundColor: '#16a34a',
                                  borderColor: '#16a34a',
                                  padding: '0.4rem 0.8rem',
                                  fontSize: '0.85rem',
                                }}
                                disabled={actionDoctorId === doc.id}
                                onClick={() => handleApproveDoctor(doc.id, doc.fullName)}
                              >
                                {actionDoctorId === doc.id ? 'Processing...' : '✓ Approve'}
                              </button>
                              <button
                                className="btn btn-danger"
                                style={{
                                  backgroundColor: '#dc2626',
                                  borderColor: '#dc2626',
                                  padding: '0.4rem 0.8rem',
                                  fontSize: '0.85rem',
                                }}
                                disabled={actionDoctorId === doc.id}
                                onClick={() => handleRejectDoctor(doc.id, doc.fullName)}
                              >
                                {actionDoctorId === doc.id ? 'Processing...' : '✕ Reject'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </section>
        )}
      </main>

      {/* Provision Portal Account Modal */}
      {provisioningPatient && (
        <div className="modal-backdrop">
          <div className="modal-dialog">
            <div className="modal-header">
              <h3>Provision Portal Account for {provisioningPatient.name}</h3>
              <button
                type="button"
                className="btn-close"
                onClick={() => setProvisioningPatient(null)}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleProvisionAccount}>
              <div className="modal-body">
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                  Create secure portal credentials linked to patient code <code>{provisioningPatient.patientCode}</code>.
                </p>
                <div className="form-group">
                  <label>Portal Username *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={provUsername}
                    onChange={(e) => setProvUsername(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Temporary Password * (min 6 characters)</label>
                  <input
                    type="password"
                    className="form-control"
                    value={provPassword}
                    onChange={(e) => setProvPassword(e.target.value)}
                    required
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setProvisioningPatient(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submittingProv}
                >
                  {submittingProv ? 'Creating Account...' : 'Create Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Floating Toast Alerts */}
      <div className="toast-container">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.type}`}>
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
