import { useCallback, useEffect, useState } from 'react'
import './ReceptionistDashboard.css'

export default function ReceptionistDashboard({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState('overview')
  const [rooms, setRooms] = useState([])
  const [beds, setBeds] = useState([])
  const [patients, setPatients] = useState([])
  const [appointments, setAppointments] = useState([])
  const [doctors, setDoctors] = useState([])
  const [loading, setLoading] = useState(true)
  const [toasts, setToasts] = useState([])

  // Intake Form State
  const [patientCode, setPatientCode] = useState('')
  const [patientName, setPatientName] = useState('')
  const [patientAge, setPatientAge] = useState('')
  const [patientGender, setPatientGender] = useState('MALE')
  const [patientDiagnosis, setPatientDiagnosis] = useState('')
  const [selectedBedId, setSelectedBedId] = useState('')
  const [allocationMode, setAllocationMode] = useState('recommend') // 'recommend' | 'manual'
  const [overrideReason, setOverrideReason] = useState('')
  const [createAccountOnAdmit, setCreateAccountOnAdmit] = useState(false)
  const [portalUsername, setPortalUsername] = useState('')
  const [portalPassword, setPortalPassword] = useState('')
  const [admittingPatient, setAdmittingPatient] = useState(false)

  // Recommendation engine states
  const [recRoomType, setRecRoomType] = useState('ICU')
  const [recWard, setRecWard] = useState('')
  const [recommendations, setRecommendations] = useState(null)
  const [loadingRecs, setLoadingRecs] = useState(false)

  // Discharges search & filter
  const [dischargeSearch, setDischargeSearch] = useState('')
  const [dischargingId, setDischargingId] = useState(null)

  // Bed matrix filters
  const [matrixWardFilter, setMatrixWardFilter] = useState('')
  const [matrixStatusFilter, setMatrixStatusFilter] = useState('')

  // Appointments booking state
  const [bookingPatientCode, setBookingPatientCode] = useState('')
  const [bookingDoctorId, setBookingDoctorId] = useState('')
  const [bookingDept, setBookingDept] = useState('General Medicine')
  const [bookingDateTime, setBookingDateTime] = useState('')
  const [bookingReason, setBookingReason] = useState('')
  const [submittingBooking, setSubmittingBooking] = useState(false)
  const [cancellingApptId, setCancellingApptId] = useState(null)

  const showToast = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random()
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }, [])

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

  const loadAllData = useCallback(async () => {
    try {
      setLoading(true)
      const [roomsRes, bedsRes, patientsRes, apptRes, docRes] = await Promise.all([
        apiFetch('/api/rooms'),
        apiFetch('/api/beds'),
        apiFetch('/api/patients'),
        apiFetch('/api/appointments/my'),
        apiFetch('/api/appointments/doctors'),
      ])

      if (roomsRes.ok && bedsRes.ok && patientsRes.ok) {
        const roomsJson = await roomsRes.json()
        const bedsJson = await bedsRes.json()
        const patientsJson = await patientsRes.json()

        setRooms(roomsJson.data || [])
        setBeds(bedsJson.data || [])
        setPatients(patientsJson.data || [])

        if (apptRes.ok) {
          const apptJson = await apptRes.json()
          setAppointments(apptJson.data || [])
        }
        if (docRes.ok) {
          const docJson = await docRes.json()
          setDoctors(docJson.data || [])
          if (docJson.data?.length > 0 && !bookingDoctorId) {
            setBookingDoctorId(String(docJson.data[0].id))
          }
        }
      } else {
        showToast('Failed to load front-desk data from server', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error communicating with server', 'error')
    } finally {
      setLoading(false)
    }
  }, [apiFetch, bookingDoctorId, showToast])

  useEffect(() => {
    let ignore = false
    const initLoad = async () => {
      try {
        const [roomsRes, bedsRes, patientsRes, apptRes, docRes] = await Promise.all([
          apiFetch('/api/rooms'),
          apiFetch('/api/beds'),
          apiFetch('/api/patients'),
          apiFetch('/api/appointments/my'),
          apiFetch('/api/appointments/doctors'),
        ])

        if (!ignore && roomsRes.ok && bedsRes.ok && patientsRes.ok) {
          const roomsJson = await roomsRes.json()
          const bedsJson = await bedsRes.json()
          const patientsJson = await patientsRes.json()

          setRooms(roomsJson.data || [])
          setBeds(bedsJson.data || [])
          setPatients(patientsJson.data || [])

          if (apptRes.ok) {
            const apptJson = await apptRes.json()
            setAppointments(apptJson.data || [])
          }
          if (docRes.ok) {
            const docJson = await docRes.json()
            setDoctors(docJson.data || [])
            if (docJson.data?.length > 0) {
              setBookingDoctorId(String(docJson.data[0].id))
            }
          }
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

  // Get Bed Recommendations
  const handleGetRecommendations = async (e) => {
    e.preventDefault()
    setLoadingRecs(true)
    try {
      const payload = {
        roomType: recRoomType,
        wardDepartment: recWard.trim() || undefined,
        limit: 5,
      }
      const res = await apiFetch('/api/allocations/recommend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        setRecommendations(data.data || [])
        if (data.data?.length === 0) {
          showToast('No available beds match these recommendation criteria', 'info')
        } else {
          showToast(`Found ${data.data.length} optimized bed recommendation(s)`, 'success')
        }
      } else {
        showToast(data.message || 'Failed to fetch recommendations', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setLoadingRecs(false)
    }
  }

  // Handle Admission Submission
  const handleAdmitSubmit = async (e) => {
    e.preventDefault()
    if (!patientCode.trim()) {
      showToast('Patient Code / MRN is required', 'error')
      return
    }
    if (!patientName.trim()) {
      showToast('Patient Full Name is required', 'error')
      return
    }
    if (!selectedBedId) {
      showToast('Please select an available bed for admission', 'error')
      return
    }
    if (allocationMode === 'manual' && !overrideReason.trim()) {
      showToast('A reason is mandatory when manually assigning or overriding bed allocation', 'error')
      return
    }

    setAdmittingPatient(true)
    try {
      const payload = {
        patientCode: patientCode.trim(),
        name: patientName.trim(),
        age: patientAge ? parseInt(patientAge, 10) : undefined,
        gender: patientGender,
        diagnosis: patientDiagnosis.trim() || undefined,
        bedId: parseInt(selectedBedId, 10),
        allocationType: allocationMode === 'manual' ? 'MANUAL_OVERRIDE' : 'RECOMMENDED',
        overrideReason: allocationMode === 'manual' ? overrideReason.trim() : undefined,
        createPortalAccount: createAccountOnAdmit,
        portalUsername: createAccountOnAdmit && portalUsername.trim() ? portalUsername.trim() : undefined,
        portalPassword: createAccountOnAdmit && portalPassword ? portalPassword : undefined,
      }

      const res = await apiFetch('/api/patients/admit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const data = await res.json()

      if (res.ok && data.success) {
        showToast(`Patient ${patientName.trim()} admitted successfully!`, 'success')
        setPatientCode('')
        setPatientName('')
        setPatientAge('')
        setPatientDiagnosis('')
        setSelectedBedId('')
        setOverrideReason('')
        setCreateAccountOnAdmit(false)
        setPortalUsername('')
        setPortalPassword('')
        setRecommendations(null)
        loadAllData()
        setActiveTab('overview')
      } else {
        showToast(data.message || 'Failed to admit patient', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setAdmittingPatient(false)
    }
  }

  // Handle Discharge
  const handleDischargePatient = async (patientId, patientNameStr) => {
    if (!window.confirm(`Confirm discharge for patient "${patientNameStr}"? Assigned bed will be released.`)) {
      return
    }
    setDischargingId(patientId)
    try {
      const res = await apiFetch(`/api/patients/${patientId}/discharge`, {
        method: 'POST',
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Patient "${patientNameStr}" successfully discharged!`, 'success')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to discharge patient', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setDischargingId(null)
    }
  }

  // Handle Appointment Booking
  const handleBookAppointment = async (e) => {
    e.preventDefault()
    if (!bookingPatientCode.trim()) {
      showToast('Patient Code / MRN is required', 'error')
      return
    }
    if (!bookingDoctorId) {
      showToast('Doctor selection is required', 'error')
      return
    }
    if (!bookingDateTime) {
      showToast('Appointment Date & Time is required', 'error')
      return
    }

    setSubmittingBooking(true)
    try {
      const payload = {
        patientCode: bookingPatientCode.trim(),
        doctorId: parseInt(bookingDoctorId, 10),
        department: bookingDept.trim(),
        appointmentDateTime: bookingDateTime,
        reason: bookingReason.trim() || undefined,
      }

      const res = await apiFetch('/api/appointments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const data = await res.json()

      if (res.ok && data.success) {
        showToast('Appointment scheduled successfully!', 'success')
        setBookingPatientCode('')
        setBookingReason('')
        setBookingDateTime('')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to schedule appointment', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setSubmittingBooking(false)
    }
  }

  // Handle Appointment Cancellation
  const handleCancelAppt = async (apptId) => {
    const reasonPrompt = window.prompt('Enter reason for cancellation:')
    if (reasonPrompt === null) return

    setCancellingApptId(apptId)
    try {
      const res = await apiFetch(`/api/appointments/${apptId}/cancel`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: reasonPrompt.trim() || 'Cancelled by Front Desk Reception' }),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast('Appointment cancelled successfully', 'info')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to cancel appointment', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setCancellingApptId(null)
    }
  }

  // Metrics computation
  const totalRooms = rooms.length
  const totalBeds = beds.length
  const availableBeds = beds.filter((b) => b.status === 'AVAILABLE')
  const occupiedBeds = beds.filter((b) => b.status === 'OCCUPIED')
  const maintenanceBeds = beds.filter((b) => b.status === 'MAINTENANCE')
  const admittedPatients = patients.filter((p) => p.status === 'ADMITTED')

  // Discharges filtered list
  const filteredAdmittedPatients = admittedPatients.filter((p) => {
    if (!dischargeSearch.trim()) return true
    const q = dischargeSearch.toLowerCase()
    return (
      p.patientCode?.toLowerCase().includes(q) ||
      p.name?.toLowerCase().includes(q) ||
      p.diagnosis?.toLowerCase().includes(q) ||
      p.assignedBedNumber?.toLowerCase().includes(q) ||
      p.roomNumber?.toLowerCase().includes(q)
    )
  })

  // Bed Matrix filtering
  const filteredBeds = beds.filter((b) => {
    const matchesWard = !matrixWardFilter || b.wardDepartment === matrixWardFilter
    const matchesStatus = !matrixStatusFilter || b.status === matrixStatusFilter
    return matchesWard && matchesStatus
  })

  // Unique wards for filters
  const uniqueWards = Array.from(new Set(rooms.map((r) => r.wardDepartment).filter(Boolean)))

  return (
    <div className="admin-container">
      {/* Toast Notifications */}
      <div className="toast-container">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            {t.message}
          </div>
        ))}
      </div>

      {/* Header */}
      <header className="admin-header">
        <div className="header-left">
          <div className="brand-logo">
            <span className="logo-icon">&#10010;</span>
            <div className="brand-text">
              <span className="brand-title">SmartHospital</span>
              <span className="brand-subtitle">Front-Desk &amp; Patient Reception Station</span>
            </div>
          </div>
        </div>
        <div className="header-right">
          <div className="user-badge">
            <span className="user-name">{user?.fullName || user?.username || 'Receptionist'}</span>
            <span className="receptionist-tag">Front Desk Reception</span>
          </div>
          <button className="btn-logout" onClick={onLogout} title="Sign Out">
            Sign Out
          </button>
        </div>
      </header>

      {/* Receptionist Quick Action Banner */}
      <div className="reception-header-banner">
        <div className="reception-banner-title">
          <h2>Patient Reception &amp; Admissions Desk</h2>
          <p>Instant bed allocation, intake triage, appointments, and discharge dispatch</p>
        </div>
        <div className="reception-banner-actions">
          <button
            className="btn-banner-action"
            onClick={() => setActiveTab('intake')}
          >
            &#43; New Patient Intake
          </button>
          <button
            className="btn-banner-action"
            onClick={() => setActiveTab('discharge')}
          >
            &#10003; Process Discharges
          </button>
          <button
            className="btn-banner-action"
            onClick={() => setActiveTab('appointments')}
          >
            &#128197; Doctor Appointments
          </button>
        </div>
      </div>

      {/* Tab Navigation */}
      <nav className="tab-navigation">
        <button
          className={`tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          Front-Desk Overview
        </button>
        <button
          className={`tab-btn ${activeTab === 'intake' ? 'active' : ''}`}
          onClick={() => setActiveTab('intake')}
        >
          Smart Bed Intake &amp; Admission
        </button>
        <button
          className={`tab-btn ${activeTab === 'discharge' ? 'active' : ''}`}
          onClick={() => setActiveTab('discharge')}
        >
          Patient Discharges ({admittedPatients.length})
        </button>
        <button
          className={`tab-btn ${activeTab === 'beds' ? 'active' : ''}`}
          onClick={() => setActiveTab('beds')}
        >
          Bed Availability Matrix
        </button>
        <button
          className={`tab-btn ${activeTab === 'appointments' ? 'active' : ''}`}
          onClick={() => setActiveTab('appointments')}
        >
          Appointments ({appointments.length})
        </button>
        <button
          className={`tab-btn ${activeTab === 'doctors' ? 'active' : ''}`}
          onClick={() => setActiveTab('doctors')}
        >
          Doctor Directory ({doctors.length})
        </button>
      </nav>

      {/* Main Content Area */}
      <main className="admin-main">
        {loading ? (
          <div className="loading-state">
            <div className="spinner"></div>
            <p>Loading front-desk hospital data...</p>
          </div>
        ) : (
          <>
            {/* TAB: OVERVIEW */}
            {activeTab === 'overview' && (
              <div className="overview-tab">
                <div className="kpi-grid">
                  <div className="kpi-card">
                    <span className="kpi-label">Available Beds</span>
                    <span className="kpi-value text-success">{availableBeds.length}</span>
                    <span className="kpi-subtext">Ready for immediate intake</span>
                  </div>
                  <div className="kpi-card">
                    <span className="kpi-label">Occupied Beds</span>
                    <span className="kpi-value text-warning">{occupiedBeds.length}</span>
                    <span className="kpi-subtext">Active inpatients</span>
                  </div>
                  <div className="kpi-card">
                    <span className="kpi-label">Maintenance / Cleaning</span>
                    <span className="kpi-value text-secondary">{maintenanceBeds.length}</span>
                    <span className="kpi-subtext">Temporarily offline</span>
                  </div>
                  <div className="kpi-card">
                    <span className="kpi-label">Total Rooms / Capacity</span>
                    <span className="kpi-value">{totalRooms} / {totalBeds} Beds</span>
                    <span className="kpi-subtext">Registered facilities</span>
                  </div>
                  <div className="kpi-card">
                    <span className="kpi-label">Scheduled Appointments</span>
                    <span className="kpi-value">{appointments.filter((a) => a.status === 'SCHEDULED').length}</span>
                    <span className="kpi-subtext">Consultations pending</span>
                  </div>
                </div>

                {/* Recent Admissions & Inpatient Snapshot */}
                <div className="card" style={{ marginTop: '2rem' }}>
                  <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3>Currently Admitted Patients ({admittedPatients.length})</h3>
                    <button className="btn-secondary" onClick={() => setActiveTab('discharge')}>
                      Manage Discharges &rarr;
                    </button>
                  </div>
                  <div className="card-body">
                    {admittedPatients.length === 0 ? (
                      <p className="empty-text">No patients currently admitted.</p>
                    ) : (
                      <div className="table-responsive">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>Patient Code</th>
                              <th>Name</th>
                              <th>Age / Gender</th>
                              <th>Assigned Bed</th>
                              <th>Room / Ward</th>
                              <th>Diagnosis</th>
                              <th>Admission Date</th>
                              <th>Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {admittedPatients.slice(0, 8).map((p) => (
                              <tr key={p.id}>
                                <td><strong>{p.patientCode}</strong></td>
                                <td>{p.name}</td>
                                <td>{p.age ? `${p.age} yrs` : 'N/A'} / {p.gender}</td>
                                <td>
                                  <span className="badge badge-success">{p.assignedBedNumber || 'N/A'}</span>
                                </td>
                                <td>{p.roomNumber || 'N/A'} ({p.wardDepartment || 'General'})</td>
                                <td>{p.diagnosis || 'General Observation'}</td>
                                <td>{p.admissionDate ? new Date(p.admissionDate).toLocaleString() : 'N/A'}</td>
                                <td>
                                  <button
                                    className="btn-danger btn-sm"
                                    onClick={() => handleDischargePatient(p.id, p.name)}
                                    disabled={dischargingId === p.id}
                                  >
                                    {dischargingId === p.id ? 'Releasing...' : 'Discharge'}
                                  </button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB: SMART BED INTAKE & ADMISSION */}
            {activeTab === 'intake' && (
              <div className="intake-tab">
                <div className="card">
                  <div className="card-header">
                    <h3>Patient Intake &amp; Optimized Bed Allocation</h3>
                    <p style={{ color: 'var(--text-secondary)', margin: '0.25rem 0 0 0', fontSize: '0.85rem' }}>
                      Register new inpatient arrival, match with the optimal bed, and optionally provision their portal account.
                    </p>
                  </div>
                  <div className="card-body">
                    {/* Allocation Mode Switch */}
                    <div className="allocation-mode-switch">
                      <button
                        type="button"
                        className={`allocation-mode-btn ${allocationMode === 'recommend' ? 'active' : ''}`}
                        onClick={() => setAllocationMode('recommend')}
                      >
                        &#9889; AI Bed Optimization &amp; Recommendation
                      </button>
                      <button
                        type="button"
                        className={`allocation-mode-btn ${allocationMode === 'manual' ? 'active' : ''}`}
                        onClick={() => setAllocationMode('manual')}
                      >
                        &#9998; Manual Bed Selection &amp; Clinical Override
                      </button>
                    </div>

                    {/* Recommendation Finder */}
                    {allocationMode === 'recommend' && (
                      <div style={{ background: '#f8fafc', padding: '1.25rem', borderRadius: 'var(--radius-md)', marginBottom: '1.5rem', border: '1px solid var(--border-color)' }}>
                        <h4 style={{ margin: '0 0 1rem 0', fontSize: '1rem', color: 'var(--text-primary)' }}>Find Optimized Bed Recommendation</h4>
                        <form onSubmit={handleGetRecommendations} style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
                          <div style={{ flex: 1, minWidth: '180px' }}>
                            <label className="form-label">Required Room Type</label>
                            <select
                              className="form-select"
                              value={recRoomType}
                              onChange={(e) => setRecRoomType(e.target.value)}
                            >
                              <option value="ICU">ICU (Intensive Care)</option>
                              <option value="GENERAL">General Ward</option>
                              <option value="PRIVATE">Private Suite</option>
                              <option value="EMERGENCY">Emergency Triage</option>
                              <option value="STANDARD">Standard</option>
                            </select>
                          </div>
                          <div style={{ flex: 1, minWidth: '180px' }}>
                            <label className="form-label">Department / Ward (Optional)</label>
                            <input
                              type="text"
                              className="form-input"
                              placeholder="e.g. Critical Care, Cardiology"
                              value={recWard}
                              onChange={(e) => setRecWard(e.target.value)}
                            />
                          </div>
                          <button type="submit" className="btn-primary" disabled={loadingRecs}>
                            {loadingRecs ? 'Analyzing...' : 'Search Recommended Beds'}
                          </button>
                        </form>

                        {/* Recommendation Results */}
                        {recommendations && (
                          <div style={{ marginTop: '1.25rem' }}>
                            <h5 style={{ margin: '0 0 0.5rem 0' }}>Available Bed Recommendations:</h5>
                            {recommendations.length === 0 ? (
                              <p className="empty-text">No available beds found for this room type. Switch to manual selection or choose another ward.</p>
                            ) : (
                              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '0.75rem' }}>
                                {recommendations.map((rec) => (
                                  <div
                                    key={rec.bedId}
                                    style={{
                                      border: selectedBedId === String(rec.bedId) ? '2px solid var(--primary)' : '1px solid var(--border-color)',
                                      background: selectedBedId === String(rec.bedId) ? '#eff6ff' : '#ffffff',
                                      padding: '0.85rem',
                                      borderRadius: 'var(--radius-md)',
                                      cursor: 'pointer',
                                    }}
                                    onClick={() => setSelectedBedId(String(rec.bedId))}
                                  >
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                      <strong>{rec.bedNumber}</strong>
                                      <span className="badge badge-success">Match: {Math.round(rec.matchScore)}%</span>
                                    </div>
                                    <p style={{ margin: '0.35rem 0', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                                      Room: {rec.roomNumber} ({rec.wardDepartment})
                                    </p>
                                    <p style={{ margin: 0, fontSize: '0.75rem', color: '#166534' }}>
                                      {rec.reason || 'Optimally matched'}
                                    </p>
                                    <button
                                      type="button"
                                      className="btn-secondary btn-sm"
                                      style={{ marginTop: '0.5rem', width: '100%' }}
                                      onClick={(e) => {
                                        e.stopPropagation()
                                        setSelectedBedId(String(rec.bedId))
                                        showToast(`Bed ${rec.bedNumber} selected!`, 'info')
                                      }}
                                    >
                                      {selectedBedId === String(rec.bedId) ? '&#10003; Selected' : 'Choose Bed'}
                                    </button>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    )}

                    {/* Manual Mode Notice & Override Reason */}
                    {allocationMode === 'manual' && (
                      <div className="override-notice">
                        <strong>Clinical Manual Override Activated</strong>
                        <span>
                          Antigravity Hospital Policy requires an audit reason whenever bypassing automated bed matching algorithms.
                        </span>
                        <div style={{ marginTop: '0.75rem' }}>
                          <label className="form-label" style={{ color: '#92400e' }}>
                            Mandatory Override Reason *
                          </label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. Patient isolation protocol, urgent family request, direct physician order"
                            value={overrideReason}
                            onChange={(e) => setOverrideReason(e.target.value)}
                            required
                          />
                        </div>
                      </div>
                    )}

                    {/* Admission Intake Form */}
                    <form onSubmit={handleAdmitSubmit}>
                      <div className="form-grid">
                        <div className="form-group">
                          <label className="form-label">Patient Code / MRN *</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. PAT-2026-001"
                            value={patientCode}
                            onChange={(e) => setPatientCode(e.target.value)}
                            required
                          />
                        </div>

                        <div className="form-group">
                          <label className="form-label">Full Name *</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. Robert Williams"
                            value={patientName}
                            onChange={(e) => setPatientName(e.target.value)}
                            required
                          />
                        </div>

                        <div className="form-group">
                          <label className="form-label">Age</label>
                          <input
                            type="number"
                            min="0"
                            max="130"
                            className="form-input"
                            placeholder="e.g. 45"
                            value={patientAge}
                            onChange={(e) => setPatientAge(e.target.value)}
                          />
                        </div>

                        <div className="form-group">
                          <label className="form-label">Gender</label>
                          <select
                            className="form-select"
                            value={patientGender}
                            onChange={(e) => setPatientGender(e.target.value)}
                          >
                            <option value="MALE">Male</option>
                            <option value="FEMALE">Female</option>
                            <option value="OTHER">Other</option>
                          </select>
                        </div>

                        <div className="form-group" style={{ gridColumn: 'span 2' }}>
                          <label className="form-label">Primary Clinical Diagnosis / Admission Reason</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. Acute Coronary Syndrome, Post-Op Monitoring"
                            value={patientDiagnosis}
                            onChange={(e) => setPatientDiagnosis(e.target.value)}
                          />
                        </div>

                        <div className="form-group" style={{ gridColumn: 'span 2' }}>
                          <label className="form-label">Select Available Bed *</label>
                          <select
                            className="form-select"
                            value={selectedBedId}
                            onChange={(e) => setSelectedBedId(e.target.value)}
                            required
                          >
                            <option value="">-- Choose an Available Bed ({availableBeds.length} available) --</option>
                            {availableBeds.map((b) => (
                              <option key={b.id} value={b.id}>
                                {b.bedNumber} &mdash; Room {b.roomNumber} ({b.wardDepartment || 'General'} - {b.roomType || 'Standard'})
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>

                      {/* Optional Portal Account Creation */}
                      <div style={{ marginTop: '1.5rem', background: '#f8fafc', padding: '1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontWeight: 600 }}>
                          <input
                            type="checkbox"
                            checked={createAccountOnAdmit}
                            onChange={(e) => setCreateAccountOnAdmit(e.target.checked)}
                          />
                          Create patient portal credentials during admission (Emergency / Front-desk onboarding)
                        </label>

                        {createAccountOnAdmit && (
                          <div className="form-grid" style={{ marginTop: '1rem' }}>
                            <div className="form-group">
                              <label className="form-label">Portal Username (Defaults to MRN)</label>
                              <input
                                type="text"
                                className="form-input"
                                placeholder={patientCode.toLowerCase() || 'username'}
                                value={portalUsername}
                                onChange={(e) => setPortalUsername(e.target.value)}
                              />
                            </div>
                            <div className="form-group">
                              <label className="form-label">Temporary Password (Min 6 chars) *</label>
                              <input
                                type="password"
                                className="form-input"
                                placeholder="Temporary password"
                                value={portalPassword}
                                onChange={(e) => setPortalPassword(e.target.value)}
                                required={createAccountOnAdmit}
                              />
                            </div>
                          </div>
                        )}
                      </div>

                      <div style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => {
                            setPatientCode('')
                            setPatientName('')
                            setSelectedBedId('')
                            setRecommendations(null)
                          }}
                        >
                          Clear Form
                        </button>
                        <button type="submit" className="btn-primary" disabled={admittingPatient}>
                          {admittingPatient ? 'Admitting Patient...' : '&#10003; Complete Patient Admission'}
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              </div>
            )}

            {/* TAB: DISCHARGE MANAGEMENT */}
            {activeTab === 'discharge' && (
              <div className="discharge-tab">
                <div className="card">
                  <div className="card-header">
                    <h3>Inpatient Discharges &amp; Bed Turnover</h3>
                    <p style={{ color: 'var(--text-secondary)', margin: '0.25rem 0 0 0', fontSize: '0.85rem' }}>
                      Discharging releases the occupied bed back to AVAILABLE and completes the patient stay history.
                    </p>
                  </div>
                  <div className="card-body">
                    <div className="reception-toolbar">
                      <div className="search-box">
                        <span className="search-icon">&#128269;</span>
                        <input
                          type="text"
                          placeholder="Search admitted patient by name, code, bed or room..."
                          value={dischargeSearch}
                          onChange={(e) => setDischargeSearch(e.target.value)}
                        />
                      </div>
                    </div>

                    {filteredAdmittedPatients.length === 0 ? (
                      <p className="empty-text">No admitted patients match your search criteria.</p>
                    ) : (
                      <div className="table-responsive">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>Patient Code</th>
                              <th>Patient Name</th>
                              <th>Age / Gender</th>
                              <th>Assigned Bed</th>
                              <th>Room / Ward</th>
                              <th>Admission Diagnosis</th>
                              <th>Admission Timestamp</th>
                              <th>Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {filteredAdmittedPatients.map((p) => (
                              <tr key={p.id}>
                                <td><strong>{p.patientCode}</strong></td>
                                <td>{p.name}</td>
                                <td>{p.age ? `${p.age} yrs` : 'N/A'} / {p.gender}</td>
                                <td><span className="badge badge-success">{p.assignedBedNumber || 'N/A'}</span></td>
                                <td>{p.roomNumber || 'N/A'} ({p.wardDepartment || 'General'})</td>
                                <td>{p.diagnosis || 'Observation'}</td>
                                <td>{p.admissionDate ? new Date(p.admissionDate).toLocaleString() : 'N/A'}</td>
                                <td>
                                  <button
                                    className="btn-danger btn-sm"
                                    onClick={() => handleDischargePatient(p.id, p.name)}
                                    disabled={dischargingId === p.id}
                                  >
                                    {dischargingId === p.id ? 'Releasing...' : 'Release & Discharge'}
                                  </button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB: BED AVAILABILITY MATRIX */}
            {activeTab === 'beds' && (
              <div className="beds-tab">
                <div className="card">
                  <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                    <div>
                      <h3>Hospital Bed Availability Matrix</h3>
                      <p style={{ color: 'var(--text-secondary)', margin: '0.25rem 0 0 0', fontSize: '0.85rem' }}>
                        Live view of bed occupancies and ward capacities
                      </p>
                    </div>
                    <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                      <select
                        className="form-select"
                        value={matrixWardFilter}
                        onChange={(e) => setMatrixWardFilter(e.target.value)}
                      >
                        <option value="">All Wards ({uniqueWards.length})</option>
                        {uniqueWards.map((w) => (
                          <option key={w} value={w}>{w}</option>
                        ))}
                      </select>
                      <select
                        className="form-select"
                        value={matrixStatusFilter}
                        onChange={(e) => setMatrixStatusFilter(e.target.value)}
                      >
                        <option value="">All Bed Statuses</option>
                        <option value="AVAILABLE">AVAILABLE ({availableBeds.length})</option>
                        <option value="OCCUPIED">OCCUPIED ({occupiedBeds.length})</option>
                        <option value="MAINTENANCE">MAINTENANCE ({maintenanceBeds.length})</option>
                      </select>
                    </div>
                  </div>
                  <div className="card-body">
                    {filteredBeds.length === 0 ? (
                      <p className="empty-text">No beds match filter selection.</p>
                    ) : (
                      <div className="table-responsive">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>Bed Number</th>
                              <th>Room Number</th>
                              <th>Room Type</th>
                              <th>Ward / Department</th>
                              <th>Current Status</th>
                            </tr>
                          </thead>
                          <tbody>
                            {filteredBeds.map((b) => (
                              <tr key={b.id}>
                                <td><strong>{b.bedNumber}</strong></td>
                                <td>Room {b.roomNumber}</td>
                                <td>{b.roomType || 'Standard'}</td>
                                <td>{b.wardDepartment || 'General'}</td>
                                <td>
                                  <span
                                    className={`badge ${
                                      b.status === 'AVAILABLE'
                                        ? 'badge-success'
                                        : b.status === 'OCCUPIED'
                                        ? 'badge-warning'
                                        : 'badge-secondary'
                                    }`}
                                  >
                                    {b.status}
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB: APPOINTMENTS */}
            {activeTab === 'appointments' && (
              <div className="appointments-tab">
                {/* Book Appointment Card */}
                <div className="card" style={{ marginBottom: '1.5rem' }}>
                  <div className="card-header">
                    <h3>Schedule New Doctor Consultation</h3>
                  </div>
                  <div className="card-body">
                    <form onSubmit={handleBookAppointment}>
                      <div className="form-grid">
                        <div className="form-group">
                          <label className="form-label">Patient Code / MRN *</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. PAT-2026-001"
                            value={bookingPatientCode}
                            onChange={(e) => setBookingPatientCode(e.target.value)}
                            required
                          />
                        </div>

                        <div className="form-group">
                          <label className="form-label">Consulting Doctor *</label>
                          <select
                            className="form-select"
                            value={bookingDoctorId}
                            onChange={(e) => setBookingDoctorId(e.target.value)}
                            required
                          >
                            <option value="">-- Choose Doctor --</option>
                            {doctors.map((d) => (
                              <option key={d.id} value={d.id}>
                                {d.fullName} ({d.specialization || 'General'})
                              </option>
                            ))}
                          </select>
                        </div>

                        <div className="form-group">
                          <label className="form-label">Department</label>
                          <input
                            type="text"
                            className="form-input"
                            value={bookingDept}
                            onChange={(e) => setBookingDept(e.target.value)}
                          />
                        </div>

                        <div className="form-group">
                          <label className="form-label">Appointment Date &amp; Time *</label>
                          <input
                            type="datetime-local"
                            className="form-input"
                            value={bookingDateTime}
                            onChange={(e) => setBookingDateTime(e.target.value)}
                            required
                          />
                        </div>

                        <div className="form-group" style={{ gridColumn: 'span 2' }}>
                          <label className="form-label">Consultation Reason / Chief Symptoms</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. Routine hypertension review, persistent cough"
                            value={bookingReason}
                            onChange={(e) => setBookingReason(e.target.value)}
                          />
                        </div>
                      </div>

                      <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
                        <button type="submit" className="btn-primary" disabled={submittingBooking}>
                          {submittingBooking ? 'Booking...' : '&#128197; Book Appointment'}
                        </button>
                      </div>
                    </form>
                  </div>
                </div>

                {/* Appointment Schedule Table */}
                <div className="card">
                  <div className="card-header">
                    <h3>Hospital Appointment Schedule ({appointments.length})</h3>
                  </div>
                  <div className="card-body">
                    {appointments.length === 0 ? (
                      <p className="empty-text">No appointments found in the schedule.</p>
                    ) : (
                      <div className="table-responsive">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>ID</th>
                              <th>Patient</th>
                              <th>Doctor</th>
                              <th>Department</th>
                              <th>Scheduled Date/Time</th>
                              <th>Reason</th>
                              <th>Status</th>
                              <th>Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {appointments.map((a) => (
                              <tr key={a.id}>
                                <td>#{a.id}</td>
                                <td><strong>{a.patientName}</strong> ({a.patientCode})</td>
                                <td>{a.doctorName}</td>
                                <td>{a.department || 'General'}</td>
                                <td>{a.appointmentDateTime ? new Date(a.appointmentDateTime).toLocaleString() : 'N/A'}</td>
                                <td>{a.reason || 'General Checkup'}</td>
                                <td>
                                  <span
                                    className={`badge ${
                                      a.status === 'SCHEDULED'
                                        ? 'badge-success'
                                        : a.status === 'COMPLETED'
                                        ? 'badge-secondary'
                                        : 'badge-danger'
                                    }`}
                                  >
                                    {a.status}
                                  </span>
                                </td>
                                <td>
                                  {a.status === 'SCHEDULED' && (
                                    <button
                                      className="btn-danger btn-sm"
                                      onClick={() => handleCancelAppt(a.id)}
                                      disabled={cancellingApptId === a.id}
                                    >
                                      {cancellingApptId === a.id ? 'Cancelling...' : 'Cancel'}
                                    </button>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB: DOCTOR DIRECTORY */}
            {activeTab === 'doctors' && (
              <div className="doctors-tab">
                <div className="card">
                  <div className="card-header">
                    <h3>Hospital Medical Staff &amp; Doctors Directory</h3>
                    <p style={{ color: 'var(--text-secondary)', margin: '0.25rem 0 0 0', fontSize: '0.85rem' }}>
                      Roster of verified doctors available for patient referrals and consultations
                    </p>
                  </div>
                  <div className="card-body">
                    {doctors.length === 0 ? (
                      <p className="empty-text">No active doctors currently registered.</p>
                    ) : (
                      <div className="doctor-directory-grid">
                        {doctors.map((doc) => (
                          <div key={doc.id} className="doctor-directory-card">
                            <h4 className="doc-name">{doc.fullName}</h4>
                            <p className="doc-specialty">&#9877; {doc.specialization || 'General Practitioner'}</p>
                            <p className="doc-detail"><strong>Qualifications:</strong> {doc.qualifications || 'MBBS'}</p>
                            <p className="doc-detail"><strong>Reg Number:</strong> {doc.medicalRegistrationNumber || 'Verified'}</p>
                            <p className="doc-detail"><strong>Username:</strong> @{doc.username}</p>
                            <button
                              type="button"
                              className="btn-secondary btn-sm"
                              style={{ marginTop: '0.5rem' }}
                              onClick={() => {
                                setBookingDoctorId(String(doc.id))
                                setActiveTab('appointments')
                              }}
                            >
                              Schedule Appointment &rarr;
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  )
}
