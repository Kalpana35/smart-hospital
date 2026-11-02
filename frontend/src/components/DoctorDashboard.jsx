import { useCallback, useEffect, useState } from 'react'
import './DoctorDashboard.css'

export default function DoctorDashboard({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState('inpatients')
  const [rooms, setRooms] = useState([])
  const [beds, setBeds] = useState([])
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(true)
  const [toasts, setToasts] = useState([])

  // Search & Filtering states
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ADMITTED')
  const [wardFilter, setWardFilter] = useState('')

  // Patient Details Modal state
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [loadingPatientDetail, setLoadingPatientDetail] = useState(false)
  const [dischargingPatient, setDischargingPatient] = useState(false)
  const [expectedDischargeInput, setExpectedDischargeInput] = useState('')
  const [updatingExpectedDischarge, setUpdatingExpectedDischarge] = useState(false)

  // Bed Availability Matrix filter state
  const [matrixWardFilter, setMatrixWardFilter] = useState('')
  const [matrixStatusFilter, setMatrixStatusFilter] = useState('')

  // Recommendation engine states
  const [recRoomType, setRecRoomType] = useState('ICU')
  const [recWard, setRecWard] = useState('')
  const [recommendations, setRecommendations] = useState(null)
  const [loadingRecs, setLoadingRecs] = useState(false)

  // Admission Modal state (from Bed Recommender)
  const [admitModalBed, setAdmitModalBed] = useState(null)
  const [admitPatientCode, setAdmitPatientCode] = useState('')
  const [admitPatientName, setAdmitPatientName] = useState('')
  const [admitPatientAge, setAdmitPatientAge] = useState('')
  const [admitPatientGender, setAdmitPatientGender] = useState('MALE')
  const [admitPatientDiagnosis, setAdmitPatientDiagnosis] = useState('')
  const [submittingAdmission, setSubmittingAdmission] = useState(false)

  // Doctor Appointments state
  const [appointments, setAppointments] = useState([])
  const [completingAppt, setCompletingAppt] = useState(null)
  const [consultNotes, setConsultNotes] = useState('')
  const [consultDiagnosis, setConsultDiagnosis] = useState('')
  const [consultTreatment, setConsultTreatment] = useState('')
  const [consultPrescription, setConsultPrescription] = useState('')
  const [consultations, setConsultations] = useState([])
  const [submittingComplete, setSubmittingComplete] = useState(false)

  const showToast = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random()
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }, [])

  // Centralized authenticated API fetch helper
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
      const [roomsRes, bedsRes, patientsRes, apptRes, consultRes] = await Promise.all([
        apiFetch('/api/rooms'),
        apiFetch('/api/beds'),
        apiFetch('/api/patients'),
        apiFetch('/api/appointments/my'),
        apiFetch('/api/consultations/my'),
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
        if (consultRes.ok) {
          const consultJson = await consultRes.json()
          setConsultations(consultJson.data || [])
        }
      } else {
        showToast('Failed to load clinical data from server', 'error')
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
        const [roomsRes, bedsRes, patientsRes, apptRes, consultRes] = await Promise.all([
          apiFetch('/api/rooms'),
          apiFetch('/api/beds'),
          apiFetch('/api/patients'),
          apiFetch('/api/appointments/my'),
          apiFetch('/api/consultations/my'),
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
          if (consultRes.ok) {
            const consultJson = await consultRes.json()
            setConsultations(consultJson.data || [])
          }
        } else if (!ignore) {
          showToast('Failed to load clinical data from server', 'error')
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

  const handleCompleteAppt = async (e) => {
    e.preventDefault()
    if (!completingAppt) return
    setSubmittingComplete(true)
    try {
      // 1. Record consultation diagnosis & prescription
      await apiFetch('/api/consultations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          patientId: completingAppt.patientId,
          appointmentId: completingAppt.id,
          diagnosis: consultDiagnosis.trim() || completingAppt.reason || 'General Consultation',
          treatment: consultTreatment.trim() || undefined,
          prescription: consultPrescription.trim() || undefined,
          notes: consultNotes.trim() || undefined,
          status: 'COMPLETED',
        }),
      })

      // 2. Mark appointment completed
      const res = await apiFetch(`/api/appointments/${completingAppt.id}/complete`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ notes: consultNotes.trim() || consultDiagnosis.trim() }),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast('Clinical consultation & prescription recorded successfully!', 'success')
        setCompletingAppt(null)
        setConsultNotes('')
        setConsultDiagnosis('')
        setConsultTreatment('')
        setConsultPrescription('')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to complete appointment', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setSubmittingComplete(false)
    }
  }

  const handleCancelDoctorAppt = async (apptId) => {
    const reason = window.prompt('Please enter cancellation reason:')
    if (reason === null) return
    try {
      const res = await apiFetch(`/api/appointments/${apptId}/cancel`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: reason.trim() || 'Cancelled by doctor' }),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast('Appointment cancelled', 'info')
        loadAllData()
      } else {
        showToast(data.message || 'Failed to cancel appointment', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  // Open single patient details using existing GET /api/patients/{id}
  const handleOpenPatientDetails = async (patientId) => {
    try {
      setLoadingPatientDetail(true)
      const res = await apiFetch(`/api/patients/${patientId}`)
      const data = await res.json()
      if (res.ok && data.success && data.data) {
        setSelectedPatient(data.data)
        if (data.data.expectedDischargeDate) {
          setExpectedDischargeInput(data.data.expectedDischargeDate.slice(0, 16))
        } else {
          setExpectedDischargeInput('')
        }
      } else {
        showToast(data.message || 'Failed to retrieve patient details', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error fetching patient details', 'error')
    } finally {
      setLoadingPatientDetail(false)
    }
  }

  // Update expected discharge target date (PATCH /api/patients/{id}/expected-discharge)
  const handleUpdateExpectedDischarge = async (e) => {
    if (e) e.preventDefault()
    if (!selectedPatient) return
    if (selectedPatient.status !== 'ADMITTED') {
      showToast('Expected discharge date can only be updated for actively admitted patients.', 'error')
      return
    }
    if (!expectedDischargeInput) {
      showToast('Please select a target discharge date and time.', 'error')
      return
    }

    setUpdatingExpectedDischarge(true)
    try {
      const res = await apiFetch(`/api/patients/${selectedPatient.id}/expected-discharge`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          expectedDischargeDate: expectedDischargeInput,
        }),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast('Expected discharge date updated successfully!', 'success')
        setSelectedPatient(data.data)
        loadAllData()
      } else {
        showToast(data.message || 'Failed to update expected discharge date', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error updating expected discharge date', 'error')
    } finally {
      setUpdatingExpectedDischarge(false)
    }
  }

  // Discharge patient from details modal
  const handleDischargeFromModal = async (patientId, patientName, bedNumber) => {
    if (
      !window.confirm(
        `Confirm clinical discharge for patient ${patientName}? Bed ${bedNumber || ''} will be released.`
      )
    ) {
      return
    }

    setDischargingPatient(true)
    try {
      const res = await apiFetch(`/api/patients/${patientId}/discharge`, {
        method: 'POST',
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Patient ${patientName} discharged successfully. Bed released!`, 'success')
        setSelectedPatient(null)
        loadAllData()
      } else {
        showToast(data.message || 'Failed to discharge patient', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error discharging patient', 'error')
    } finally {
      setDischargingPatient(false)
    }
  }

  // Open admission modal for a recommended bed
  const handleOpenAdmitModal = (rec) => {
    const liveBed = beds.find((b) => b.id === rec.bedId)
    if (liveBed && liveBed.status !== 'AVAILABLE') {
      showToast(
        `Bed ${rec.bedNumber} is currently ${liveBed.status} and cannot be selected for admission.`,
        'error'
      )
      return
    }

    setAdmitModalBed(rec)
    setAdmitPatientCode('')
    setAdmitPatientName('')
    setAdmitPatientAge('')
    setAdmitPatientGender('MALE')
    setAdmitPatientDiagnosis('')
  }

  const handleCloseAdmitModal = () => {
    if (submittingAdmission) return
    setAdmitModalBed(null)
  }

  const handleSubmitAdmission = async (e) => {
    e.preventDefault()

    if (!admitModalBed) return

    const trimmedCode = admitPatientCode.trim()
    const trimmedName = admitPatientName.trim()
    const parsedAge = parseInt(admitPatientAge, 10)
    const trimmedDiagnosis = admitPatientDiagnosis.trim()

    if (!trimmedCode) {
      showToast('Patient ID / MRN is required.', 'error')
      return
    }
    if (!trimmedName) {
      showToast('Patient Full Name is required.', 'error')
      return
    }
    if (!admitPatientAge || isNaN(parsedAge) || parsedAge <= 0) {
      showToast('Valid patient age is required.', 'error')
      return
    }
    if (!admitPatientGender) {
      showToast('Patient gender is required.', 'error')
      return
    }
    if (!trimmedDiagnosis) {
      showToast('Clinical diagnosis is required.', 'error')
      return
    }

    // Verify bed is still available before submitting
    const liveBed = beds.find((b) => b.id === admitModalBed.bedId)
    if (liveBed && liveBed.status !== 'AVAILABLE') {
      showToast(
        `Bed ${admitModalBed.bedNumber} is no longer AVAILABLE (current status: ${liveBed.status}).`,
        'error'
      )
      return
    }

    setSubmittingAdmission(true)
    try {
      const res = await apiFetch('/api/patients/admit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          patientCode: trimmedCode,
          name: trimmedName,
          age: parsedAge,
          gender: admitPatientGender,
          diagnosis: trimmedDiagnosis,
          bedId: admitModalBed.bedId,
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        showToast(
          `Patient ${data.data.name} admitted successfully to Bed ${data.data.bedNumber}!`,
          'success'
        )
        const admittedBedId = admitModalBed.bedId
        setAdmitModalBed(null)
        // Refresh clinical data so live occupancy and patient directory update
        await loadAllData()
        // Remove admitted bed from recommendations list
        setRecommendations((prev) =>
          prev ? prev.filter((r) => r.bedId !== admittedBedId) : null
        )
      } else {
        showToast(data.message || 'Failed to admit patient.', 'error')
      }
    } catch (err) {
      showToast(err.message || 'Error admitting patient to bed.', 'error')
    } finally {
      setSubmittingAdmission(false)
    }
  }

  // Bed allocation recommendation handler
  const handleGetRecommendations = async (e) => {
    e.preventDefault()
    if (!recRoomType) {
      showToast('Please select a required room type', 'error')
      return
    }

    setLoadingRecs(true)
    setRecommendations(null)
    try {
      const res = await apiFetch('/api/allocations/recommend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          roomType: recRoomType,
          wardDepartment: recWard.trim() || null,
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        setRecommendations(data.data || [])
        showToast(`Found ${data.data?.length || 0} recommended bed(s)`, 'info')
      } else {
        showToast(data.message || 'No suitable recommendations found', 'error')
        setRecommendations([])
      }
    } catch (err) {
      showToast(err.message || 'Failed to fetch recommendations', 'error')
    } finally {
      setLoadingRecs(false)
    }
  }

  const handleLogoutClick = async () => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      })
    } finally {
      if (onLogout) onLogout()
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return '—'
    try {
      const d = new Date(dateString)
      return isNaN(d.getTime())
        ? dateString
        : d.toLocaleString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })
    } catch {
      return dateString
    }
  }

  // Dynamic KPI Metrics
  const activeInpatients = patients.filter((p) => p.status === 'ADMITTED').length
  const dischargedPatients = patients.filter((p) => p.status === 'DISCHARGED').length
  const totalBeds = beds.length
  const availableBeds = beds.filter((b) => b.status === 'AVAILABLE').length
  const occupiedBeds = beds.filter((b) => b.status === 'OCCUPIED').length
  const icuAdmissions = patients.filter(
    (p) =>
      p.status === 'ADMITTED' &&
      (p.roomType?.toUpperCase().includes('ICU') ||
        p.wardDepartment?.toLowerCase().includes('critical'))
  ).length

  // Unique wards for filter
  const allWards = Array.from(
    new Set(rooms.map((r) => r.wardDepartment).filter(Boolean))
  ).sort()

  // Filtered patients list
  const filteredPatients = patients.filter((patient) => {
    // Status filter
    if (statusFilter !== 'ALL' && patient.status !== statusFilter) {
      return false
    }

    // Ward filter
    if (wardFilter && patient.wardDepartment !== wardFilter) {
      return false
    }

    // Text search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase().trim()
      const matchName = patient.name?.toLowerCase().includes(query)
      const matchCode = patient.patientCode?.toLowerCase().includes(query)
      const matchDiag = patient.diagnosis?.toLowerCase().includes(query)
      const matchBed = patient.bedNumber?.toLowerCase().includes(query)
      const matchRoom = patient.roomNumber?.toLowerCase().includes(query)
      if (!matchName && !matchCode && !matchDiag && !matchBed && !matchRoom) {
        return false
      }
    }

    return true
  })

  // Filtered rooms for Live Matrix view
  const filteredRooms = rooms.filter((r) => {
    if (matrixWardFilter && r.wardDepartment !== matrixWardFilter) return false
    if (matrixStatusFilter) {
      const roomBeds = beds.filter((b) => b.roomId === r.id)
      const hasMatchingBed = roomBeds.some((b) => b.status === matrixStatusFilter)
      if (!hasMatchingBed) return false
    }
    return true
  })

  return (
    <div className="app-container">
      {/* Toast Notification Container */}
      <div className="toast-container" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast ${toast.type}`}>
            <span>{toast.message}</span>
          </div>
        ))}
      </div>

      {/* Doctor Portal Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <span className="logo-icon">🏥</span>
          <div>
            <h2>SmartHospital</h2>
            <div className="subtitle">Doctor Clinical Station</div>
          </div>
        </div>

        <nav className="nav-menu">
          <button
            className={`nav-item ${activeTab === 'inpatients' ? 'active' : ''}`}
            onClick={() => setActiveTab('inpatients')}
          >
            <span>🧑‍⚕️</span> Inpatient Directory
          </button>
          <button
            className={`nav-item ${activeTab === 'beds' ? 'active' : ''}`}
            onClick={() => setActiveTab('beds')}
          >
            <span>🛏️</span> Live Bed Matrix
          </button>
          <button
            className={`nav-item ${activeTab === 'recommendations' ? 'active' : ''}`}
            onClick={() => setActiveTab('recommendations')}
          >
            <span>🧭</span> Bed Recommender
          </button>
          <button
            className={`nav-item ${activeTab === 'appointments' ? 'active' : ''}`}
            onClick={() => setActiveTab('appointments')}
          >
            <span>📅</span> Consultations & Schedule
          </button>
          <button
            className={`nav-item ${activeTab === 'consultations' ? 'active' : ''}`}
            onClick={() => setActiveTab('consultations')}
          >
            <span>📋</span> Prescriptions & Notes ({consultations.length})
          </button>
        </nav>

        <div className="sidebar-footer">
          <span className="pulse-dot"></span>
          <span>Doctor Station Active</span>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="main-content">
        {/* Top Header Bar */}
        <header className="topbar">
          <div className="page-title">
            <h1>
              {activeTab === 'inpatients' && 'Inpatient Directory & Bed Assignments'}
              {activeTab === 'beds' && 'Live Bed Availability Matrix'}
              {activeTab === 'recommendations' && 'Clinical Bed Allocation Optimizer'}
              {activeTab === 'appointments' && 'My Consultations & Appointment Schedule'}
              {activeTab === 'consultations' && 'Clinical Prescriptions & Consultation Notes'}
            </h1>
            <p>
              {activeTab === 'inpatients' &&
                'Review patient profiles, clinical diagnoses, and live bed assignments.'}
              {activeTab === 'beds' &&
                'Live overview of all monitored hospital rooms, wards, and bed statuses.'}
              {activeTab === 'recommendations' &&
                'Compute optimal bed assignments for incoming patients based on required care.'}
              {activeTab === 'appointments' &&
                'Review scheduled visits, update consultation encounter notes, and mark completed.'}
              {activeTab === 'consultations' &&
                'Comprehensive history of recorded diagnoses, treatments, and patient prescriptions.'}
            </p>
          </div>

          <div className="topbar-actions">
            <div className="user-badge">
              <span className="user-avatar">🩺</span>
              <div className="user-details">
                <span className="user-name">
                  {user?.fullName || user?.username || 'Attending Physician'}
                </span>
                <span className="user-role-tag doctor-tag">
                  {user?.role || 'ROLE_DOCTOR'}
                </span>
              </div>
            </div>

            <button
              className="btn btn-secondary"
              onClick={loadAllData}
              disabled={loading}
              title="Refresh clinical data from server"
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

        {/* Clinical KPI Summary Metrics */}
        <section className="kpi-grid">
          <div className="kpi-card">
            <div className="kpi-icon blue">🧑‍⚕️</div>
            <div>
              <div className="kpi-label">Active Inpatients</div>
              <div className="kpi-value">{activeInpatients}</div>
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
            <div className="kpi-icon purple">🚨</div>
            <div>
              <div className="kpi-label">ICU Inpatients</div>
              <div className="kpi-value purple-text">{icuAdmissions}</div>
            </div>
          </div>
        </section>

        {/* ========================================================= */}
        {/* TAB 1: Inpatient Directory & Bed Assignments              */}
        {/* ========================================================= */}
        {activeTab === 'inpatients' && (
          <section>
            <div className="card">
              <div className="doctor-toolbar">
                <div className="search-box">
                  <span className="search-icon">🔍</span>
                  <input
                    type="text"
                    placeholder="Search by Name, Patient MRN, Diagnosis, or Room..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </div>

                <div className="filters-row">
                  <select
                    className="filter-select"
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    aria-label="Filter by patient status"
                  >
                    <option value="ADMITTED">Admitted Only ({activeInpatients})</option>
                    <option value="DISCHARGED">Discharged History ({dischargedPatients})</option>
                    <option value="ALL">All Patients ({patients.length})</option>
                  </select>

                  <select
                    className="filter-select"
                    value={wardFilter}
                    onChange={(e) => setWardFilter(e.target.value)}
                    aria-label="Filter by ward"
                  >
                    <option value="">All Wards</option>
                    {allWards.map((w) => (
                      <option key={w} value={w}>
                        {w}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Patient List Table */}
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Patient MRN</th>
                      <th>Patient Name</th>
                      <th>Age / Gender</th>
                      <th>Diagnosis</th>
                      <th>Admission Date</th>
                      <th>Expected Discharge</th>
                      <th>Assigned Bed</th>
                      <th>Room & Type</th>
                      <th>Ward</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredPatients.length === 0 ? (
                      <tr>
                        <td colSpan="11" className="text-center text-muted" style={{ padding: '2rem' }}>
                          {searchQuery || wardFilter || statusFilter !== 'ALL'
                            ? 'No patients match your search or filter criteria.'
                            : 'No patient records found in the hospital registry.'}
                        </td>
                      </tr>
                    ) : (
                      filteredPatients.map((patient) => {
                        const associatedRoom = rooms.find((r) => r.id === patient.roomId)
                        const roomTypeDisplay =
                          patient.roomType || associatedRoom?.roomType || 'Standard'

                        return (
                          <tr key={patient.id}>
                            <td>
                              <strong>{patient.patientCode}</strong>
                            </td>
                            <td>
                              <strong>{patient.name}</strong>
                            </td>
                            <td>
                              {patient.age} yrs / {patient.gender}
                            </td>
                            <td>
                              <span title={patient.diagnosis}>{patient.diagnosis || '—'}</span>
                            </td>
                            <td>{formatDate(patient.admissionDate)}</td>
                            <td>
                              {patient.expectedDischargeDate ? (
                                <span style={{ color: '#059669', fontWeight: 600, fontSize: '0.85rem' }}>
                                  ⏳ {formatDate(patient.expectedDischargeDate)}
                                </span>
                              ) : (
                                <span className="text-muted" style={{ fontSize: '0.85rem' }}>—</span>
                              )}
                            </td>
                            <td>
                              {patient.bedNumber ? (
                                <span className="status-badge OCCUPIED">
                                  {patient.bedNumber}
                                </span>
                              ) : (
                                <span className="text-muted">Unassigned</span>
                              )}
                            </td>
                            <td>
                              Room {patient.roomNumber || '—'}
                              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                                {roomTypeDisplay}
                              </div>
                            </td>
                            <td>{patient.wardDepartment || '—'}</td>
                            <td>
                              <span className={`status-badge ${patient.status}`}>
                                {patient.status}
                              </span>
                            </td>
                            <td>
                              <button
                                className="btn btn-secondary btn-sm"
                                onClick={() => handleOpenPatientDetails(patient.id)}
                                disabled={loadingPatientDetail}
                                title="Open full patient details"
                              >
                                View Details
                              </button>
                            </td>
                          </tr>
                        )
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {/* ========================================================= */}
        {/* TAB 2: Live Bed Availability & Status Matrix (Read-Only)  */}
        {/* ========================================================= */}
        {activeTab === 'beds' && (
          <section>
            <div className="info-callout">
              <span>ℹ️</span>
              <div>
                <strong>Clinical View:</strong> Live hospital bed occupancy is synchronized in real time. Bed and room structural configurations are managed by Hospital Administration.
              </div>
            </div>

            <div className="section-header">
              <h2>Live Room & Bed Matrix</h2>
              <div className="filter-group">
                <label htmlFor="doc-filter-ward">Ward:</label>
                <select
                  id="doc-filter-ward"
                  value={matrixWardFilter}
                  onChange={(e) => setMatrixWardFilter(e.target.value)}
                >
                  <option value="">All Wards</option>
                  {allWards.map((w) => (
                    <option key={w} value={w}>
                      {w}
                    </option>
                  ))}
                </select>

                <label htmlFor="doc-filter-status">Status:</label>
                <select
                  id="doc-filter-status"
                  value={matrixStatusFilter}
                  onChange={(e) => setMatrixStatusFilter(e.target.value)}
                >
                  <option value="">All Statuses</option>
                  <option value="AVAILABLE">Available Only</option>
                  <option value="OCCUPIED">Occupied Only</option>
                  <option value="MAINTENANCE">Maintenance Only</option>
                </select>
              </div>
            </div>

            {filteredRooms.length === 0 ? (
              <div className="card text-center text-muted" style={{ padding: '2rem' }}>
                No rooms match the selected filter criteria.
              </div>
            ) : (
              <div className="room-grid">
                {filteredRooms.map((room) => {
                  let roomBeds = beds.filter((b) => b.roomId === room.id)
                  if (matrixStatusFilter) {
                    roomBeds = roomBeds.filter((b) => b.status === matrixStatusFilter)
                  }

                  return (
                    <div key={room.id} className="room-card">
                      <div className="room-card-header">
                        <div>
                          <div className="room-number-badge">Room {room.roomNumber}</div>
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
                          roomBeds.map((bed) => {
                            const occupyingPatient = patients.find(
                              (p) => p.status === 'ADMITTED' && p.bedId === bed.id
                            )
                            return (
                              <div
                                key={bed.id}
                                className={`bed-chip status-${bed.status}`}
                                title={`Bed ${bed.bedNumber} - ${bed.status}${
                                  occupyingPatient ? ` (Occupied by ${occupyingPatient.name})` : ''
                                }`}
                                onClick={() => {
                                  if (occupyingPatient) {
                                    handleOpenPatientDetails(occupyingPatient.id)
                                  }
                                }}
                                style={{ cursor: occupyingPatient ? 'pointer' : 'default' }}
                              >
                                🛏️ {bed.bedNumber}
                                <span style={{ fontSize: '0.7rem', opacity: 0.85 }}>
                                  ({bed.status})
                                </span>
                              </div>
                            )
                          })
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}

            {/* Read-Only Beds Inventory Table */}
            <div className="card" style={{ marginTop: '2rem' }}>
              <h3>Current Hospital Bed Inventory</h3>
              <p className="form-desc">
                Current operational status of all beds across departments.
              </p>
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Bed ID</th>
                      <th>Bed Label</th>
                      <th>Room</th>
                      <th>Room Type</th>
                      <th>Ward / Dept</th>
                      <th>Status</th>
                      <th>Current Occupant</th>
                    </tr>
                  </thead>
                  <tbody>
                    {beds.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="text-center text-muted">
                          No beds registered in system.
                        </td>
                      </tr>
                    ) : (
                      beds.map((bed) => {
                        const occupant = patients.find(
                          (p) => p.status === 'ADMITTED' && p.bedId === bed.id
                        )
                        const room = rooms.find((r) => r.id === bed.roomId)
                        const roomType = bed.roomType || room?.roomType || 'Standard'

                        return (
                          <tr key={bed.id}>
                            <td>
                              <strong>#{bed.id}</strong>
                            </td>
                            <td>
                              <strong>{bed.bedNumber}</strong>
                            </td>
                            <td>Room {bed.roomNumber}</td>
                            <td>{roomType}</td>
                            <td>{bed.wardDepartment}</td>
                            <td>
                              <span className={`status-badge ${bed.status}`}>
                                {bed.status}
                              </span>
                            </td>
                            <td>
                              {occupant ? (
                                <button
                                  className="btn btn-secondary btn-sm"
                                  onClick={() => handleOpenPatientDetails(occupant.id)}
                                  title="View occupant details"
                                >
                                  👤 {occupant.name} ({occupant.patientCode})
                                </button>
                              ) : (
                                <span className="text-muted">—</span>
                              )}
                            </td>
                          </tr>
                        )
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {/* ========================================================= */}
        {/* TAB 3: Bed Recommendation Engine                          */}
        {/* ========================================================= */}
        {activeTab === 'recommendations' && (
          <section>
            <div className="card form-card">
              <h3>🧭 Clinical Bed Allocation Recommendation</h3>
              <p className="form-desc">
                Request an optimized bed recommendation for an incoming or transferring inpatient based on clinical room requirements and target ward.
              </p>
              <form onSubmit={handleGetRecommendations}>
                <div className="form-row">
                  <div className="form-group col-6">
                    <label htmlFor="doc-rec-room-type">Required Room Type *</label>
                    <select
                      id="doc-rec-room-type"
                      value={recRoomType}
                      onChange={(e) => setRecRoomType(e.target.value)}
                      required
                    >
                      <option value="ICU">ICU (Intensive Care Unit)</option>
                      <option value="ICU-STEPDOWN">ICU-Stepdown</option>
                      <option value="GENERAL">General Ward</option>
                      <option value="SEMI_PRIVATE">Semi-Private Room</option>
                      <option value="PRIVATE">Private Room</option>
                    </select>
                  </div>
                  <div className="form-group col-6">
                    <label htmlFor="doc-rec-ward">Preferred Ward / Department (Optional)</label>
                    <input
                      id="doc-rec-ward"
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
                  {loadingRecs ? 'Computing Recommendations...' : 'Get Bed Recommendations'}
                </button>
              </form>
            </div>

            {/* Recommendations Display */}
            {recommendations && (
              <div className="card">
                <h3>Recommended Beds ({recommendations.length} Candidates Found)</h3>
                {recommendations.length === 0 ? (
                  <p className="text-muted" style={{ marginTop: '0.5rem' }}>
                    No available beds match the requested criteria. Consider transferring to adjacent step-down units or review current discharges.
                  </p>
                ) : (
                  <div className="recommendations-container">
                    {recommendations.map((rec) => (
                      <div
                        key={rec.bedId}
                        className={`recommendation-card score-${rec.matchScore}`}
                      >
                        <div className="rec-info">
                          <span
                            className={`rec-score-badge ${
                              rec.matchScore === 100 ? 'exact' : 'clinical'
                            }`}
                          >
                            Score: {rec.matchScore}/100 Match
                          </span>
                          <h4>
                            Bed {rec.bedNumber} — Room {rec.roomNumber} ({rec.roomType})
                          </h4>
                          <p>
                            <strong>Ward:</strong> {rec.wardDepartment} •{' '}
                            <strong>Availability in Room:</strong> {rec.availableBedsInRoom} of{' '}
                            {rec.totalBedsInRoom} beds free
                          </p>
                          <p style={{ marginTop: '0.25rem', color: 'var(--text-primary)' }}>
                            <strong>Clinical Reason:</strong> {rec.recommendationReason}
                          </p>
                        </div>
                        <div className="rec-badge">
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => handleOpenAdmitModal(rec)}
                            title={`Select & admit patient to Bed ${rec.bedNumber}`}
                          >
                            Select & Admit Here
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </section>
        )}

        {/* Tab 4: Appointments & Consultations */}
        {activeTab === 'appointments' && (
          <section className="dashboard-section">
            <div className="section-card">
              <div className="section-card-header">
                <div>
                  <h3>Scheduled Consultations & Patient Appointments</h3>
                  <p>Review patient consultations assigned to your clinical schedule</p>
                </div>
              </div>

              <div className="table-responsive">
                <table className="doctor-table">
                  <thead>
                    <tr>
                      <th>Date & Time</th>
                      <th>Patient Name</th>
                      <th>Patient Code</th>
                      <th>Department</th>
                      <th>Reason for Visit</th>
                      <th>Status</th>
                      <th>Encounter Notes</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {appointments.length === 0 ? (
                      <tr>
                        <td colSpan="8" className="table-empty-cell">
                          No appointments or consultations currently scheduled.
                        </td>
                      </tr>
                    ) : (
                      appointments.map((appt) => (
                        <tr key={appt.id}>
                          <td style={{ fontWeight: 600 }}>
                            {new Date(appt.appointmentDateTime).toLocaleString([], {
                              dateStyle: 'medium',
                              timeStyle: 'short',
                            })}
                          </td>
                          <td>
                            <strong>{appt.patientName}</strong>
                          </td>
                          <td>
                            <code>{appt.patientCode}</code>
                          </td>
                          <td>{appt.department}</td>
                          <td>{appt.reason}</td>
                          <td>
                            <span
                              className={`status-badge ${appt.status}`}
                              style={{
                                background:
                                  appt.status === 'SCHEDULED'
                                    ? '#e0f2fe'
                                    : appt.status === 'COMPLETED'
                                    ? '#dcfce7'
                                    : '#fee2e2',
                                color:
                                  appt.status === 'SCHEDULED'
                                    ? '#0369a1'
                                    : appt.status === 'COMPLETED'
                                    ? '#166534'
                                    : '#991b1b',
                              }}
                            >
                              {appt.status}
                            </span>
                          </td>
                          <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', maxWidth: '200px' }}>
                            {appt.notes || '—'}
                          </td>
                          <td>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                              {appt.status === 'SCHEDULED' && (
                                <>
                                  <button
                                    className="btn btn-primary btn-sm"
                                    onClick={() => {
                                      setCompletingAppt(appt)
                                      setConsultNotes('')
                                    }}
                                    title="Mark complete with clinical consultation notes"
                                  >
                                    Complete
                                  </button>
                                  <button
                                    className="btn btn-secondary btn-sm"
                                    onClick={() => handleCancelDoctorAppt(appt.id)}
                                    title="Cancel consultation"
                                  >
                                    Cancel
                                  </button>
                                </>
                              )}
                              {appt.status !== 'SCHEDULED' && (
                                <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>—</span>
                              )}
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

        {/* ========================================================= */}
        {/* Tab 5: Clinical Prescriptions & Consultation Notes        */}
        {/* ========================================================= */}
        {activeTab === 'consultations' && (
          <section className="section-panel" aria-label="Clinical Prescriptions">
            <div className="card">
              <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3 className="card-title">Recorded Consultations &amp; Medical Prescriptions ({consultations.length})</h3>
                  <p className="card-subtitle">
                    Permanent audit history of diagnoses, clinical assessments, treatments, and drug prescriptions
                  </p>
                </div>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={loadAllData}
                  disabled={loading}
                >
                  &#8635; Refresh Records
                </button>
              </div>

              <div className="table-wrapper">
                <table className="doctor-table">
                  <thead>
                    <tr>
                      <th>Date / Time</th>
                      <th>Patient</th>
                      <th>Diagnosis</th>
                      <th>Treatment Plan</th>
                      <th>Prescription (Rx)</th>
                      <th>Clinical Notes</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {consultations.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="empty-cell">
                          No consultations recorded yet. Finalize an appointment consultation to create clinical records.
                        </td>
                      </tr>
                    ) : (
                      consultations.map((c) => (
                        <tr key={c.id}>
                          <td>
                            <strong>{c.consultationDateTime ? new Date(c.consultationDateTime).toLocaleString() : 'N/A'}</strong>
                          </td>
                          <td>
                            <strong>{c.patientName}</strong>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                              Code: <code>{c.patientCode}</code>
                            </div>
                          </td>
                          <td>
                            <span className="badge badge-primary">{c.diagnosis}</span>
                          </td>
                          <td>{c.treatment || '—'}</td>
                          <td>
                            {c.prescription ? (
                              <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '0.4rem 0.6rem', borderRadius: '4px', fontSize: '0.85rem', color: '#166534', fontFamily: 'monospace' }}>
                                {c.prescription}
                              </div>
                            ) : (
                              '—'
                            )}
                          </td>
                          <td>
                            <div style={{ maxWidth: '240px', fontSize: '0.85rem' }}>
                              {c.notes || '—'}
                            </div>
                          </td>
                          <td>
                            <span className="badge badge-success">{c.status}</span>
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
      </main>

      {/* ========================================================= */}
      {/* Modal: Comprehensive Patient Details (Requirement 4)      */}
      {/* ========================================================= */}
      {selectedPatient && (
        <div
          className="modal-overlay"
          onClick={() => setSelectedPatient(null)}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                <span>👤</span> Inpatient Profile: {selectedPatient.name}
              </h3>
              <button
                className="modal-close-btn"
                onClick={() => setSelectedPatient(null)}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              {/* Demographics & Code */}
              <div>
                <div className="detail-section-title">Patient Identification & Demographics</div>
                <div className="detail-grid">
                  <div className="detail-item">
                    <span className="detail-label">Patient Code / MRN</span>
                    <span className="detail-value">{selectedPatient.patientCode}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Status</span>
                    <div>
                      <span className={`status-badge ${selectedPatient.status}`}>
                        {selectedPatient.status}
                      </span>
                    </div>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Age / Gender</span>
                    <span className="detail-value">
                      {selectedPatient.age} years • {selectedPatient.gender}
                    </span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Admission Timestamp</span>
                    <span className="detail-value">{formatDate(selectedPatient.admissionDate)}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Expected Discharge Target</span>
                    <span
                      className="detail-value"
                      style={{ color: selectedPatient.expectedDischargeDate ? '#059669' : 'inherit' }}
                    >
                      {selectedPatient.expectedDischargeDate
                        ? `⏳ ${formatDate(selectedPatient.expectedDischargeDate)}`
                        : 'Under Clinical Evaluation'}
                    </span>
                  </div>
                  {selectedPatient.dischargeDate && (
                    <div className="detail-item">
                      <span className="detail-label">Discharge Timestamp</span>
                      <span className="detail-value">{formatDate(selectedPatient.dischargeDate)}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Diagnosis */}
              <div>
                <div className="detail-section-title">Clinical Diagnosis</div>
                <div className="detail-box-diagnosis">
                  {selectedPatient.diagnosis || 'No clinical diagnosis recorded.'}
                </div>
              </div>

              {/* Bed Assignment */}
              <div>
                <div className="detail-section-title">Current Bed & Room Assignment</div>
                {selectedPatient.bedNumber ? (
                  <div className="bed-assignment-card">
                    <div className="bed-assignment-info">
                      <h4>
                        Bed {selectedPatient.bedNumber} • Room {selectedPatient.roomNumber}
                      </h4>
                      <p>
                        Type: {selectedPatient.roomType || 'Standard'} • Ward:{' '}
                        {selectedPatient.wardDepartment}
                      </p>
                    </div>
                    <span className="status-badge OCCUPIED">OCCUPIED</span>
                  </div>
                ) : (
                  <p className="text-muted">No bed currently assigned to this patient.</p>
                )}
              </div>

              {/* Expected Discharge Planning Section */}
              <div>
                <div className="detail-section-title">Expected Discharge Management</div>
                <div className="discharge-target-card">
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: '0.5rem',
                    }}
                  >
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      Current Target:
                    </span>
                    <strong
                      style={{
                        fontSize: '0.9rem',
                        color: selectedPatient.expectedDischargeDate ? '#059669' : 'var(--text-primary)',
                      }}
                    >
                      {selectedPatient.expectedDischargeDate
                        ? formatDate(selectedPatient.expectedDischargeDate)
                        : 'Not Scheduled / Under Evaluation'}
                    </strong>
                  </div>
                  {selectedPatient.status === 'ADMITTED' ? (
                    <form onSubmit={handleUpdateExpectedDischarge} className="discharge-target-form">
                      <input
                        type="datetime-local"
                        value={expectedDischargeInput}
                        onChange={(e) => setExpectedDischargeInput(e.target.value)}
                        className="discharge-target-input"
                        required
                        aria-label="Expected discharge date"
                      />
                      <button
                        type="submit"
                        className="btn btn-primary btn-sm"
                        disabled={updatingExpectedDischarge}
                      >
                        {updatingExpectedDischarge ? 'Updating...' : 'Update Target Date'}
                      </button>
                    </form>
                  ) : (
                    <div
                      style={{
                        fontSize: '0.85rem',
                        color: 'var(--text-secondary)',
                        fontStyle: 'italic',
                      }}
                    >
                      Patient is {selectedPatient.status.toLowerCase()}. Expected discharge target can only be modified while admitted.
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              {selectedPatient.status === 'ADMITTED' && (
                <button
                  className="btn btn-danger"
                  onClick={() =>
                    handleDischargeFromModal(
                      selectedPatient.id,
                      selectedPatient.name,
                      selectedPatient.bedNumber
                    )
                  }
                  disabled={dischargingPatient}
                >
                  {dischargingPatient ? 'Discharging...' : 'Discharge Patient'}
                </button>
              )}
              <button
                className="btn btn-secondary"
                onClick={() => setSelectedPatient(null)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* Modal: Patient Admission to Recommended Bed               */}
      {/* ========================================================= */}
      {admitModalBed && (
        <div
          className="modal-overlay"
          onClick={handleCloseAdmitModal}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                <span>🏥</span> Admit Patient to Bed {admitModalBed.bedNumber}
              </h3>
              <button
                className="modal-close-btn"
                onClick={handleCloseAdmitModal}
                disabled={submittingAdmission}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitAdmission}>
              <div className="modal-body">
                {/* Pre-selected Target Bed Info */}
                <div className="bed-assignment-card">
                  <div className="bed-assignment-info">
                    <h4>
                      Bed {admitModalBed.bedNumber} • Room {admitModalBed.roomNumber}
                    </h4>
                    <p>
                      Type: {admitModalBed.roomType} • Ward: {admitModalBed.wardDepartment}
                    </p>
                    <p style={{ marginTop: '0.25rem', fontSize: '0.8rem', color: '#15803d' }}>
                      Match Score: {admitModalBed.matchScore}/100 ({admitModalBed.recommendationReason})
                    </p>
                  </div>
                  <span className="status-badge AVAILABLE">Target Bed</span>
                </div>

                <div className="detail-section-title">Patient Demographics & Clinical Information</div>

                <div className="form-row">
                  <div className="form-group col-6">
                    <label htmlFor="admit-patient-code">Patient MRN / Code *</label>
                    <input
                      id="admit-patient-code"
                      type="text"
                      placeholder="e.g. PAT-201, MRN-8840"
                      value={admitPatientCode}
                      onChange={(e) => setAdmitPatientCode(e.target.value)}
                      disabled={submittingAdmission}
                      required
                    />
                  </div>
                  <div className="form-group col-6">
                    <label htmlFor="admit-patient-name">Patient Full Name *</label>
                    <input
                      id="admit-patient-name"
                      type="text"
                      placeholder="e.g. Eleanor Vance"
                      value={admitPatientName}
                      onChange={(e) => setAdmitPatientName(e.target.value)}
                      disabled={submittingAdmission}
                      required
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group col-6">
                    <label htmlFor="admit-patient-age">Age (Years) *</label>
                    <input
                      id="admit-patient-age"
                      type="number"
                      min="1"
                      max="130"
                      placeholder="e.g. 54"
                      value={admitPatientAge}
                      onChange={(e) => setAdmitPatientAge(e.target.value)}
                      disabled={submittingAdmission}
                      required
                    />
                  </div>
                  <div className="form-group col-6">
                    <label htmlFor="admit-patient-gender">Gender *</label>
                    <select
                      id="admit-patient-gender"
                      value={admitPatientGender}
                      onChange={(e) => setAdmitPatientGender(e.target.value)}
                      disabled={submittingAdmission}
                      required
                    >
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="admit-patient-diagnosis">Primary Clinical Diagnosis *</label>
                  <input
                    id="admit-patient-diagnosis"
                    type="text"
                    placeholder="e.g. Acute Coronary Syndrome, Post-Op Monitoring"
                    value={admitPatientDiagnosis}
                    onChange={(e) => setAdmitPatientDiagnosis(e.target.value)}
                    disabled={submittingAdmission}
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={handleCloseAdmitModal}
                  disabled={submittingAdmission}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submittingAdmission}
                >
                  {submittingAdmission ? 'Admitting Patient...' : 'Confirm Admission'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Complete Appointment Consultation */}
      {completingAppt && (
        <div
          className="modal-overlay"
          onClick={() => setCompletingAppt(null)}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                <span>🩺</span> Complete Consultation: {completingAppt.patientName}
              </h3>
              <button
                className="modal-close-btn"
                onClick={() => setCompletingAppt(null)}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCompleteAppt}>
              <div className="modal-body">
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
                  Patient: <strong>{completingAppt.patientName}</strong> (<code>{completingAppt.patientCode}</code>) &bull; Chief Complaint: {completingAppt.reason || 'General Visit'}
                </p>
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label htmlFor="consult-diagnosis" style={{ fontWeight: 600 }}>Clinical Diagnosis *</label>
                  <input
                    id="consult-diagnosis"
                    type="text"
                    className="form-control"
                    placeholder="e.g. Acute Bronchitis, Type 2 Diabetes, Migraine"
                    value={consultDiagnosis}
                    onChange={(e) => setConsultDiagnosis(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label htmlFor="consult-treatment" style={{ fontWeight: 600 }}>Treatment Plan / Care Protocol</label>
                  <input
                    id="consult-treatment"
                    type="text"
                    className="form-control"
                    placeholder="e.g. Hydration, bed rest, nebulization, dietary restriction"
                    value={consultTreatment}
                    onChange={(e) => setConsultTreatment(e.target.value)}
                  />
                </div>
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label htmlFor="consult-rx" style={{ fontWeight: 600 }}>Prescription (Rx) &amp; Medication Dosages</label>
                  <textarea
                    id="consult-rx"
                    rows="3"
                    className="form-control"
                    placeholder="e.g. Amoxicillin 500mg PO TID x 7 days; Paracetamol 650mg PO Q6H PRN fever"
                    value={consultPrescription}
                    onChange={(e) => setConsultPrescription(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="consult-notes" style={{ fontWeight: 600 }}>Doctor Notes &amp; Follow-up Advice</label>
                  <textarea
                    id="consult-notes"
                    rows="2"
                    className="form-control"
                    placeholder="Enter physician assessment, follow-up instructions, or warnings..."
                    value={consultNotes}
                    onChange={(e) => setConsultNotes(e.target.value)}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setCompletingAppt(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submittingComplete}
                >
                  {submittingComplete ? 'Saving Records...' : 'Finalize & Record Prescription'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

