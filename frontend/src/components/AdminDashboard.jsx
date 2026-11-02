import { useCallback, useEffect, useState } from 'react'
import { validateStrongPassword } from '../utils/passwordPolicy'
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

  // Staff Management state
  const [staffList, setStaffList] = useState([])
  const [loadingStaff, setLoadingStaff] = useState(false)
  const [staffFullName, setStaffFullName] = useState('')
  const [staffUsername, setStaffUsername] = useState('')
  const [staffEmail, setStaffEmail] = useState('')
  const [staffRole, setStaffRole] = useState('ROLE_DOCTOR')
  const [staffTempPassword, setStaffTempPassword] = useState('')
  const [staffSpecialization, setStaffSpecialization] = useState('')
  const [staffQualifications, setStaffQualifications] = useState('')
  const [staffMedRegNo, setStaffMedRegNo] = useState('')
  const [creatingStaff, setCreatingStaff] = useState(false)
  const [togglingStaffId, setTogglingStaffId] = useState(null)

  // Bed Allocation History state
  const [allocationHistory, setAllocationHistory] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Department Capacity state
  const [departments, setDepartments] = useState([])
  const [loadingDepartments, setLoadingDepartments] = useState(false)

  // Cleaning & Sanitization state
  const [cleaningQueue, setCleaningQueue] = useState([])
  const [cleaningStats, setCleaningStats] = useState(null)
  const [loadingCleaning, setLoadingCleaning] = useState(false)
  const [assigningBedId, setAssigningBedId] = useState(null)
  const [cleanerAssignName, setCleanerAssignName] = useState('')
  const [completingBedId, setCompletingBedId] = useState(null)
  const [completionNotes, setCompletionNotes] = useState('')

  // Predictive Discharges state
  const [predictiveDischarges, setPredictiveDischarges] = useState([])
  const [loadingPredictive, setLoadingPredictive] = useState(false)

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

  const loadStaff = useCallback(async () => {
    try {
      setLoadingStaff(true)
      const res = await apiFetch('/api/auth/staff')
      if (res.ok) {
        const json = await res.json()
        setStaffList(json.data || [])
      }
    } catch {
      // non-blocking
    } finally {
      setLoadingStaff(false)
    }
  }, [apiFetch])

  const loadAllocationHistory = useCallback(async () => {
    try {
      setLoadingHistory(true)
      const res = await apiFetch('/api/allocations/history')
      if (res.ok) {
        const json = await res.json()
        setAllocationHistory(json.data || [])
      }
    } catch {
      // non-blocking
    } finally {
      setLoadingHistory(false)
    }
  }, [apiFetch])

  const loadDepartments = useCallback(async () => {
    try {
      setLoadingDepartments(true)
      const res = await apiFetch('/api/departments')
      if (res.ok) {
        const json = await res.json()
        setDepartments(json.data || [])
      }
    } catch {
      // non-blocking
    } finally {
      setLoadingDepartments(false)
    }
  }, [apiFetch])

  const loadCleaningData = useCallback(async () => {
    try {
      setLoadingCleaning(true)
      const [qRes, sRes] = await Promise.all([
        apiFetch('/api/cleaning/queue'),
        apiFetch('/api/cleaning/statistics'),
      ])
      if (qRes.ok) {
        const qJson = await qRes.json()
        setCleaningQueue(qJson.data || [])
      }
      if (sRes.ok) {
        const sJson = await sRes.json()
        setCleaningStats(sJson.data || null)
      }
    } catch {
      // non-blocking
    } finally {
      setLoadingCleaning(false)
    }
  }, [apiFetch])

  const loadPredictiveDischarges = useCallback(async () => {
    try {
      setLoadingPredictive(true)
      const res = await apiFetch('/api/allocations/predictive-discharges')
      if (res.ok) {
        const json = await res.json()
        setPredictiveDischarges(json.data || [])
      }
    } catch {
      // non-blocking
    } finally {
      setLoadingPredictive(false)
    }
  }, [apiFetch])

  const handleAssignCleaner = async (e) => {
    e.preventDefault()
    if (!assigningBedId || !cleanerAssignName.trim()) {
      showToast('Cleaner name or staff ID is required', 'error')
      return
    }
    try {
      const res = await apiFetch(`/api/cleaning/${assigningBedId}/assign`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cleanerName: cleanerAssignName.trim() }),
      })
      const json = await res.json()
      if (res.ok && json.success) {
        showToast('Cleaner successfully assigned to bed!', 'success')
        setAssigningBedId(null)
        setCleanerAssignName('')
        loadAllData()
      } else {
        showToast(json.message || 'Failed to assign cleaner', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const handleCompleteCleaning = async (e) => {
    e.preventDefault()
    if (!completingBedId) return
    try {
      const res = await apiFetch(`/api/cleaning/${completingBedId}/complete`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cleanerName: 'Assigned Cleaner', notes: completionNotes.trim() || undefined }),
      })
      const json = await res.json()
      if (res.ok && json.success) {
        showToast(json.message || 'Bed sanitized and ready for patient admission!', 'success')
        setCompletingBedId(null)
        setCompletionNotes('')
        loadAllData()
      } else {
        showToast(json.message || 'Failed to complete cleaning', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const loadAllData = useCallback(async () => {
    try {
      setLoading(true)
      const [roomsRes, bedsRes, patientsRes, analyticsRes, doctorsRes, staffRes, histRes, deptRes, cleanQRes, cleanSRes, predRes] = await Promise.all([
        apiFetch('/api/rooms'),
        apiFetch('/api/beds'),
        apiFetch('/api/patients'),
        apiFetch('/api/analytics/summary'),
        apiFetch('/api/auth/doctors/pending'),
        apiFetch('/api/auth/staff'),
        apiFetch('/api/allocations/history'),
        apiFetch('/api/departments'),
        apiFetch('/api/cleaning/queue'),
        apiFetch('/api/cleaning/statistics'),
        apiFetch('/api/allocations/predictive-discharges'),
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

        if (staffRes && staffRes.ok) {
          const sJson = await staffRes.json()
          setStaffList(sJson.data || [])
        }

        if (histRes && histRes.ok) {
          const hJson = await histRes.json()
          setAllocationHistory(hJson.data || [])
        }

        if (deptRes && deptRes.ok) {
          const deptJson = await deptRes.json()
          setDepartments(deptJson.data || [])
        }

        if (cleanQRes && cleanQRes.ok) {
          const cqJson = await cleanQRes.json()
          setCleaningQueue(cqJson.data || [])
        }

        if (cleanSRes && cleanSRes.ok) {
          const csJson = await cleanSRes.json()
          setCleaningStats(csJson.data || null)
        }

        if (predRes && predRes.ok) {
          const pJson = await predRes.json()
          setPredictiveDischarges(pJson.data || [])
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
        const [roomsRes, bedsRes, patientsRes, analyticsRes, doctorsRes, staffRes, histRes, deptRes, cleanQRes, cleanSRes, predRes] = await Promise.all([
          apiFetch('/api/rooms'),
          apiFetch('/api/beds'),
          apiFetch('/api/patients'),
          apiFetch('/api/analytics/summary'),
          apiFetch('/api/auth/doctors/pending'),
          apiFetch('/api/auth/staff'),
          apiFetch('/api/allocations/history'),
          apiFetch('/api/departments'),
          apiFetch('/api/cleaning/queue'),
          apiFetch('/api/cleaning/statistics'),
          apiFetch('/api/allocations/predictive-discharges'),
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

          if (staffRes && staffRes.ok) {
            const sJson = await staffRes.json()
            setStaffList(sJson.data || [])
          }

          if (histRes && histRes.ok) {
            const hJson = await histRes.json()
            setAllocationHistory(hJson.data || [])
          }

          if (deptRes && deptRes.ok) {
            const deptJson = await deptRes.json()
            setDepartments(deptJson.data || [])
          }

          if (cleanQRes && cleanQRes.ok) {
            const cqJson = await cleanQRes.json()
            setCleaningQueue(cqJson.data || [])
          }

          if (cleanSRes && cleanSRes.ok) {
            const csJson = await cleanSRes.json()
            setCleaningStats(csJson.data || null)
          }

          if (predRes && predRes.ok) {
            const pJson = await predRes.json()
            setPredictiveDischarges(pJson.data || [])
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

  // Dynamic KPI Metrics
  const totalRooms = rooms.length
  const totalBeds = beds.length
  const availableBeds = beds.filter((b) => b.status === 'AVAILABLE').length
  const occupiedBeds = beds.filter((b) => b.status === 'OCCUPIED').length
  const maintenanceBeds = beds.filter((b) => b.status === 'MAINTENANCE').length
  const dirtyBeds = beds.filter((b) => b.status === 'DIRTY').length
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

  const handleCreateStaff = async (e) => {
    e.preventDefault()
    if (!staffFullName.trim()) {
      showToast('Staff Full Name is required', 'error')
      return
    }
    if (!staffUsername.trim()) {
      showToast('Username is required', 'error')
      return
    }
    const pwdErrors = validateStrongPassword(staffTempPassword)
    if (pwdErrors.length > 0) {
      showToast(`Temporary password must meet security policy: ${pwdErrors.join(', ')} (e.g. Reception@123)`, 'error')
      return
    }
    if (staffRole === 'ROLE_DOCTOR' && !staffSpecialization.trim()) {
      showToast('Specialization is required for doctor accounts', 'error')
      return
    }

    setCreatingStaff(true)
    try {
      const payload = {
        fullName: staffFullName.trim(),
        username: staffUsername.trim(),
        email: staffEmail.trim() || undefined,
        role: staffRole,
        temporaryPassword: staffTempPassword,
        specialization: staffRole === 'ROLE_DOCTOR' ? staffSpecialization.trim() : undefined,
        qualifications: staffQualifications.trim() || undefined,
        medicalRegistrationNumber: staffMedRegNo.trim() || undefined,
      }
      const res = await apiFetch('/api/auth/staff', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast(`Staff account for "${staffFullName.trim()}" created successfully!`, 'success')
        setStaffFullName('')
        setStaffUsername('')
        setStaffEmail('')
        setStaffTempPassword('')
        setStaffSpecialization('')
        setStaffQualifications('')
        setStaffMedRegNo('')
        loadStaff()
      } else {
        showToast(data.message || 'Failed to create staff account', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setCreatingStaff(false)
    }
  }

  const handleToggleStaffStatus = async (staffId, staffName) => {
    setTogglingStaffId(staffId)
    try {
      const res = await apiFetch(`/api/auth/users/${staffId}/toggle-status`, {
        method: 'PATCH',
      })
      const data = await res.json()
      if (res.ok && data.success) {
        const newStatus = data.data.enabled ? 'activated' : 'deactivated'
        showToast(`Account for "${staffName}" has been ${newStatus}.`, 'info')
        loadStaff()
      } else {
        showToast(data.message || 'Failed to toggle account status', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setTogglingStaffId(null)
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
    staff: {
      title: 'Hospital Staff & Roles Administration',
      desc: 'Provision doctor and receptionist accounts with temporary passwords and control access status',
    },
    allocations: {
      title: 'Bed Allocation Audit Trail & History',
      desc: 'Comprehensive compliance log of all algorithm-recommended and manually overridden bed allocations',
    },
    departments: {
      title: 'Department Structure & 100-Bed Capacity Matrix',
      desc: 'Five core hospital clinical departments, ward hierarchy, and room-to-bed allocation',
    },
    cleaning: {
      title: 'Bed Sanitization & Cleaning Queue',
      desc: 'Track discharged dirty beds, clinical turnaround priorities, cleaner assignments, and completion',
    },
    'predictive-discharges': {
      title: 'Predictive Discharges & Pending Available Beds',
      desc: 'Track admitted inpatients with upcoming expected discharge dates for proactive bed planning',
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
          <button
            className={`nav-item ${activeTab === 'staff' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('staff')
              loadStaff()
            }}
          >
            <span>👥</span> Staff Accounts ({staffList.length})
          </button>
          <button
            className={`nav-item ${activeTab === 'allocations' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('allocations')
              loadAllocationHistory()
            }}
          >
            <span>📜</span> Allocation History ({allocationHistory.length})
          </button>
          <button
            className={`nav-item ${activeTab === 'departments' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('departments')
              loadDepartments()
            }}
          >
            <span>🏢</span> Departments ({departments.length})
          </button>
          <button
            className={`nav-item ${activeTab === 'cleaning' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('cleaning')
              loadCleaningData()
            }}
          >
            <span>🧹</span> Cleaning Queue
            {cleaningQueue.length > 0 && (
              <span
                style={{
                  marginLeft: 'auto',
                  backgroundColor: '#f59e0b',
                  color: '#ffffff',
                  fontSize: '0.72rem',
                  fontWeight: '700',
                  padding: '0.15rem 0.5rem',
                  borderRadius: '9999px',
                }}
              >
                {cleaningQueue.length}
              </span>
            )}
          </button>
          <button
            className={`nav-item ${activeTab === 'predictive-discharges' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('predictive-discharges')
              loadPredictiveDischarges()
            }}
          >
            <span>⏳</span> Predictive Discharges ({predictiveDischarges.length})
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
            <div className="kpi-icon amber">🧹</div>
            <div>
              <div className="kpi-label">Dirty (Sanitizing)</div>
              <div className="kpi-value amber-text">{dirtyBeds}</div>
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

        {/* Tab 7: Staff Management */}
        {activeTab === 'staff' && (
          <section>
            {/* Create Staff Form Card */}
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <div className="card-header">
                <h3>Provision New Hospital Staff Account</h3>
                <p className="form-desc">
                  Create Doctor or Front-Desk Receptionist accounts with temporary passwords. Staff must update their password on first login.
                </p>
              </div>
              <div className="card-body">
                <form onSubmit={handleCreateStaff}>
                  <div className="form-grid">
                    <div className="form-group">
                      <label className="form-label">Full Name *</label>
                      <input
                        type="text"
                        className="form-input"
                        placeholder="e.g. Dr. Emily Watson or Jane Reception"
                        value={staffFullName}
                        onChange={(e) => setStaffFullName(e.target.value)}
                        required
                      />
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                        Staff member's actual display name (e.g. Dr. Emily Watson).
                      </span>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Account Role *</label>
                      <select
                        className="form-select"
                        value={staffRole}
                        onChange={(e) => setStaffRole(e.target.value)}
                      >
                        <option value="ROLE_DOCTOR">Doctor (ROLE_DOCTOR)</option>
                        <option value="ROLE_RECEPTIONIST">Receptionist (ROLE_RECEPTIONIST)</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Username *</label>
                      <input
                        type="text"
                        className="form-input"
                        placeholder="e.g. dr.emily or reception.front"
                        value={staffUsername}
                        onChange={(e) => setStaffUsername(e.target.value)}
                        required
                      />
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                        Unique login ID used to sign into the system (e.g. dr.emily).
                      </span>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Temporary Password (Strong Password Policy) *</label>
                      <input
                        type="password"
                        className="form-input"
                        placeholder="e.g. Reception@123"
                        value={staffTempPassword}
                        onChange={(e) => setStaffTempPassword(e.target.value)}
                        required
                      />
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                        Must have &ge; 6 characters, uppercase (A-Z), lowercase (a-z), number (0-9), and special character (e.g. Reception@123).
                      </span>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Email Address</label>
                      <input
                        type="email"
                        className="form-input"
                        placeholder="staff@smarthospital.org"
                        value={staffEmail}
                        onChange={(e) => setStaffEmail(e.target.value)}
                      />
                    </div>

                    {staffRole === 'ROLE_DOCTOR' && (
                      <>
                        <div className="form-group">
                          <label className="form-label">Specialization *</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. Cardiology, Neurology, ICU"
                            value={staffSpecialization}
                            onChange={(e) => setStaffSpecialization(e.target.value)}
                            required={staffRole === 'ROLE_DOCTOR'}
                          />
                        </div>

                        <div className="form-group">
                          <label className="form-label">Qualifications</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. MBBS, MD, FRCS"
                            value={staffQualifications}
                            onChange={(e) => setStaffQualifications(e.target.value)}
                          />
                        </div>

                        <div className="form-group">
                          <label className="form-label">Medical Registration Number</label>
                          <input
                            type="text"
                            className="form-input"
                            placeholder="e.g. MED-REG-2026-99"
                            value={staffMedRegNo}
                            onChange={(e) => setStaffMedRegNo(e.target.value)}
                          />
                        </div>
                      </>
                    )}
                  </div>

                  <div style={{ marginTop: '1.25rem', display: 'flex', justifyContent: 'flex-end' }}>
                    <button type="submit" className="btn btn-primary" disabled={creatingStaff}>
                      {creatingStaff ? 'Creating Account...' : '&#43; Provision Staff Account'}
                    </button>
                  </div>
                </form>
              </div>
            </div>

            {/* Staff Accounts Table */}
            <div className="card">
              <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3>Hospital Staff Roster ({staffList.length})</h3>
                  <p className="form-desc">Manage system access and active privileges for doctors and receptionists</p>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={loadStaff} disabled={loadingStaff}>
                  &#8635; Refresh Roster
                </button>
              </div>
              <div className="card-body">
                {staffList.length === 0 ? (
                  <p className="empty-text">No staff accounts found.</p>
                ) : (
                  <div className="table-responsive">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Full Name</th>
                          <th>Role</th>
                          <th>Username</th>
                          <th>Email / Reg #</th>
                          <th>Created</th>
                          <th>Status</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {staffList.map((st) => (
                          <tr key={st.id}>
                            <td><strong>{st.fullName}</strong></td>
                            <td>
                              <span
                                className={`badge ${
                                  st.role === 'ROLE_ADMIN'
                                    ? 'badge-primary'
                                    : st.role === 'ROLE_DOCTOR'
                                    ? 'badge-success'
                                    : 'badge-secondary'
                                }`}
                              >
                                {st.role}
                              </span>
                            </td>
                            <td><code>{st.username}</code></td>
                            <td>
                              <div>{st.email || '—'}</div>
                              {st.medicalRegistrationNumber && (
                                <small style={{ color: 'var(--text-secondary)' }}>Reg: {st.medicalRegistrationNumber}</small>
                              )}
                            </td>
                            <td>{st.createdAt ? new Date(st.createdAt).toLocaleDateString() : 'N/A'}</td>
                            <td>
                              <span className={`badge ${st.enabled ? 'badge-success' : 'badge-danger'}`}>
                                {st.enabled ? 'Active' : 'Disabled'}
                              </span>
                            </td>
                            <td>
                              {st.username.toLowerCase() !== user?.username?.toLowerCase() && (
                                <button
                                  className={`btn ${st.enabled ? 'btn-danger' : 'btn-primary'} btn-sm`}
                                  onClick={() => handleToggleStaffStatus(st.id, st.fullName)}
                                  disabled={togglingStaffId === st.id}
                                >
                                  {togglingStaffId === st.id ? 'Updating...' : st.enabled ? 'Deactivate' : 'Activate'}
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
          </section>
        )}

        {/* Tab 8: Bed Allocation Audit History */}
        {activeTab === 'allocations' && (
          <section>
            <div className="card">
              <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3>Bed Allocation &amp; Optimization Audit Trail ({allocationHistory.length})</h3>
                  <p className="form-desc">
                    Comprehensive compliance log tracking algorithmic bed assignments, clinical overrides, and stay lifecycles
                  </p>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={loadAllocationHistory} disabled={loadingHistory}>
                  &#8635; Refresh Audit Trail
                </button>
              </div>
              <div className="card-body">
                {allocationHistory.length === 0 ? (
                  <p className="empty-text">No bed allocation history recorded yet.</p>
                ) : (
                  <div className="table-responsive">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Allocated At</th>
                          <th>Bed &amp; Room</th>
                          <th>Ward</th>
                          <th>Patient</th>
                          <th>Allocated By</th>
                          <th>Method</th>
                          <th>Override Reason</th>
                          <th>Released At</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {allocationHistory.map((h) => (
                          <tr key={h.id}>
                            <td>
                              <strong>{h.allocatedAt ? new Date(h.allocatedAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : 'N/A'}</strong>
                            </td>
                            <td>
                              <strong>{h.bedNumber}</strong> (Room {h.roomNumber})
                            </td>
                            <td>{h.wardDepartment}</td>
                            <td>
                              <strong>{h.patientName}</strong>
                              <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{h.patientCode}</div>
                            </td>
                            <td><code>{h.allocatedBy}</code></td>
                            <td>
                              <span
                                className={`badge ${
                                  h.allocationType === 'RECOMMENDED' ? 'badge-success' : 'badge-warning'
                                }`}
                              >
                                {h.allocationType}
                              </span>
                            </td>
                            <td style={{ maxWidth: '200px', fontSize: '0.85rem' }}>
                              {h.overrideReason || (h.allocationType === 'RECOMMENDED' ? 'Automated Match' : '—')}
                            </td>
                            <td>
                              {h.releasedAt ? new Date(h.releasedAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : '—'}
                            </td>
                            <td>
                              <span className={`badge ${h.active ? 'badge-success' : 'badge-secondary'}`}>
                                {h.active ? 'Active Stay' : 'Released'}
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
          </section>
        )}

        {/* Tab 9: Department Structure & Capacity */}
        {activeTab === 'departments' && (
          <section>
            <div className="card">
              <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3>Hospital Clinical Departments &amp; 100-Bed Matrix ({departments.length})</h3>
                  <p className="form-desc">
                    Comprehensive hierarchy: 5 clinical departments, specialized ward equipment, rooms, and live bed capacities
                  </p>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={loadDepartments} disabled={loadingDepartments}>
                  🔄 Refresh Departments
                </button>
              </div>
              <div className="card-body">
                {departments.length === 0 ? (
                  <p className="empty-text">Loading department hierarchy...</p>
                ) : (
                  <div className="department-grid">
                    {departments.map((dept) => (
                      <div key={dept.id} className="department-card">
                        <div>
                          <div className="dept-header">
                            <div>
                              <h4 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>{dept.name}</h4>
                              <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                                Head: <strong>{dept.headOfDepartment || 'Clinical Chief'}</strong>
                              </div>
                            </div>
                            <span className="dept-code-tag">{dept.code}</span>
                          </div>
                          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: '0.5rem 0' }}>
                            {dept.description || 'Specialized clinical care unit.'}
                          </p>

                          <div className="dept-stats-row">
                            <div className="dept-stat-item">
                              <div className="dept-stat-value">{dept.totalRooms}</div>
                              <div className="dept-stat-label">Rooms</div>
                            </div>
                            <div className="dept-stat-item">
                              <div className="dept-stat-value">{dept.totalBeds}</div>
                              <div className="dept-stat-label">Total Beds</div>
                            </div>
                            <div className="dept-stat-item">
                              <div className="dept-stat-value green-text">{dept.availableBeds}</div>
                              <div className="dept-stat-label">Available</div>
                            </div>
                            <div className="dept-stat-item">
                              <div className="dept-stat-value red-text">{dept.occupiedBeds}</div>
                              <div className="dept-stat-label">Occupied</div>
                            </div>
                          </div>

                          <div style={{ marginTop: '0.5rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.25rem' }}>
                              <span>Occupancy Rate</span>
                              <strong>{dept.occupancyRate}%</strong>
                            </div>
                            <div style={{ width: '100%', height: '8px', background: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                              <div
                                style={{
                                  width: `${Math.min(dept.occupancyRate, 100)}%`,
                                  height: '100%',
                                  background: dept.occupancyRate > 85 ? '#ef4444' : dept.occupancyRate > 60 ? '#f59e0b' : '#10b981',
                                }}
                              />
                            </div>
                          </div>
                        </div>

                        <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid #f1f5f9', display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                          {dept.code === 'ICU' && <span className="badge-clinical badge-icu">Vital Monitoring • Ventilators</span>}
                          {dept.code === 'ISO' && <span className="badge-clinical badge-iso">Negative Pressure • HEPA Containment</span>}
                          {dept.code === 'EMERG' && <span className="badge-clinical badge-overflow">Triage &amp; Resuscitation • High Priority</span>}
                          {dept.code === 'SURG' && <span className="badge-clinical badge-icu">Post-Op Telemetry • Recovery</span>}
                          {dept.code === 'GEN' && <span className="badge-clinical badge-budget-yes">Standard Care • Step-down</span>}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </section>
        )}

        {/* Tab 10: Cleaning & Sanitization Queue */}
        {activeTab === 'cleaning' && (
          <section>
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3>Turnaround Performance &amp; Sanitization Metrics</h3>
                  <p className="form-desc">Real-time statistics for hospital bed sanitization and turnaround time</p>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={loadCleaningData} disabled={loadingCleaning}>
                  🔄 Refresh Queue
                </button>
              </div>
              <div className="card-body">
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                  <div style={{ padding: '1rem', background: '#fffbeb', borderRadius: '8px', border: '1px solid #fef3c7' }}>
                    <div style={{ fontSize: '0.8rem', color: '#b45309', fontWeight: 600 }}>DIRTY BEDS IN QUEUE</div>
                    <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#92400e' }}>
                      {cleaningStats ? cleaningStats.totalDirtyBeds : cleaningQueue.filter(q => q.status === 'DIRTY').length}
                    </div>
                  </div>
                  <div style={{ padding: '1rem', background: '#eff6ff', borderRadius: '8px', border: '1px solid #dbeafe' }}>
                    <div style={{ fontSize: '0.8rem', color: '#1e40af', fontWeight: 600 }}>IN-PROGRESS SANITIZATION</div>
                    <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#1e3a8a' }}>
                      {cleaningStats ? cleaningStats.inProgressCount : cleaningQueue.filter(q => q.status === 'IN_PROGRESS').length}
                    </div>
                  </div>
                  <div style={{ padding: '1rem', background: '#f0fdf4', borderRadius: '8px', border: '1px solid #dcfce7' }}>
                    <div style={{ fontSize: '0.8rem', color: '#166534', fontWeight: 600 }}>COMPLETED TODAY</div>
                    <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#14532d' }}>
                      {cleaningStats ? cleaningStats.completedToday : 0}
                    </div>
                  </div>
                  <div style={{ padding: '1rem', background: '#faf5ff', borderRadius: '8px', border: '1px solid #f3e8ff' }}>
                    <div style={{ fontSize: '0.8rem', color: '#6b21a8', fontWeight: 600 }}>AVG TURNAROUND TIME</div>
                    <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#581c87' }}>
                      {cleaningStats && cleaningStats.averageTurnaroundMinutes > 0 ? `${cleaningStats.averageTurnaroundMinutes} mins` : '20 mins (Est.)'}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <h3>Live Bed Cleaning Queue ({cleaningQueue.length})</h3>
                <p className="form-desc">
                  Prioritized by clinical acuity (ICU/Emergency Priority 1, Surgical/Isolation Priority 2, General Priority 3). Discharged beds are strictly unallocatable until confirmed cleaned.
                </p>
              </div>
              <div className="card-body">
                {cleaningQueue.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: '#64748b' }}>
                    <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>✨</div>
                    <p style={{ fontWeight: 600, color: '#1e293b' }}>All Hospital Beds Cleaned &amp; Sanitized</p>
                    <p style={{ fontSize: '0.85rem' }}>No discharged dirty beds waiting in the sanitization queue.</p>
                  </div>
                ) : (
                  <div className="table-responsive">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Priority</th>
                          <th>Bed Number</th>
                          <th>Room</th>
                          <th>Department</th>
                          <th>Dirty Since</th>
                          <th>Status</th>
                          <th>Assigned Cleaner</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {cleaningQueue.map((item) => (
                          <tr key={item.id}>
                            <td>
                              <span className={`priority-pill priority-${item.priority || 3}`}>
                                {item.priority === 1 ? '🚨 Urgent (P1)' : item.priority === 2 ? '⚠️ High (P2)' : 'Standard (P3)'}
                              </span>
                            </td>
                            <td><strong>{item.bedNumber}</strong></td>
                            <td>Room {item.roomNumber}</td>
                            <td>{item.departmentName}</td>
                            <td>{item.dirtyAt ? new Date(item.dirtyAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recently'}</td>
                            <td>
                              <span className={`status-badge ${item.status}`}>
                                {item.status}
                              </span>
                            </td>
                            <td>
                              {item.assignedTo ? (
                                <strong>👤 {item.assignedTo}</strong>
                              ) : (
                                <span style={{ color: '#94a3b8' }}>Unassigned</span>
                              )}
                            </td>
                            <td>
                              <div style={{ display: 'flex', gap: '0.4rem' }}>
                                <button
                                  className="btn btn-secondary btn-sm"
                                  onClick={() => {
                                    setAssigningBedId(item.bedId)
                                    setCleanerAssignName(item.assignedTo || '')
                                  }}
                                >
                                  Assign Cleaner
                                </button>
                                <button
                                  className="btn btn-primary btn-sm"
                                  onClick={() => {
                                    setCompletingBedId(item.bedId)
                                    setCompletionNotes('Deep sanitized with hospital disinfectant and fresh linens.')
                                  }}
                                >
                                  Complete Cleaning
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
            </div>
          </section>
        )}

        {/* Tab 11: Predictive Discharges & Pending Available Beds */}
        {activeTab === 'predictive-discharges' && (
          <section>
            <div className="card">
              <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3>Predictive Discharges &amp; Pending Available Beds ({predictiveDischarges.length})</h3>
                  <p className="form-desc">
                    Inpatients with expected discharge dates tracked within 48 hours for advance capacity planning. (Strict invariant: beds remain occupied and cannot be allocated until discharge &amp; sanitization).
                  </p>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={loadPredictiveDischarges} disabled={loadingPredictive}>
                  🔄 Refresh List
                </button>
              </div>
              <div className="card-body">
                {predictiveDischarges.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: '#64748b' }}>
                    <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>⏳</div>
                    <p style={{ fontWeight: 600, color: '#1e293b' }}>No Upcoming Expected Discharges</p>
                    <p style={{ fontSize: '0.85rem' }}>No active inpatients are scheduled for discharge in the next 48 hours.</p>
                  </div>
                ) : (
                  <div className="table-responsive">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Patient MRN</th>
                          <th>Patient Name</th>
                          <th>Department / Ward</th>
                          <th>Room &amp; Bed</th>
                          <th>Daily Rate</th>
                          <th>Admission Date</th>
                          <th>Expected Discharge Target</th>
                          <th>Predictive Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {predictiveDischarges.map((p) => (
                          <tr key={p.id}>
                            <td><strong>{p.patientCode}</strong></td>
                            <td><strong>{p.name}</strong></td>
                            <td>{p.wardDepartment || 'General'}</td>
                            <td>
                              <strong>Bed {p.bedNumber}</strong> (Room {p.roomNumber})
                            </td>
                            <td>{p.dailyRate ? `$${p.dailyRate}/day` : 'Standard'}</td>
                            <td>{p.admissionDate ? new Date(p.admissionDate).toLocaleDateString() : 'N/A'}</td>
                            <td>
                              <span style={{ color: '#059669', fontWeight: 600 }}>
                                ⏳ {p.expectedDischargeDate ? new Date(p.expectedDischargeDate).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : 'Target not set'}
                              </span>
                            </td>
                            <td>
                              <span className="badge badge-warning" title="Bed cannot be allocated until patient is discharged and sanitization is completed">
                                PENDING_AVAILABLE (Occupied)
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
          </section>
        )}
      </main>

      {/* Assign Cleaner Modal */}
      {assigningBedId && (
        <div className="modal-backdrop">
          <div className="modal-dialog">
            <div className="modal-header">
              <h3>Assign Sanitation Staff to Bed</h3>
              <button type="button" className="btn-close" onClick={() => setAssigningBedId(null)}>×</button>
            </div>
            <form onSubmit={handleAssignCleaner}>
              <div className="modal-body">
                <div className="form-group">
                  <label>Staff Cleaner Name or ID *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Cleaner Alice / Staff ID #504"
                    value={cleanerAssignName}
                    onChange={(e) => setCleanerAssignName(e.target.value)}
                    required
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setAssigningBedId(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Assign Cleaner</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Complete Cleaning Modal */}
      {completingBedId && (
        <div className="modal-backdrop">
          <div className="modal-dialog">
            <div className="modal-header">
              <h3>Confirm Bed Cleaning &amp; Sanitization</h3>
              <button type="button" className="btn-close" onClick={() => setCompletingBedId(null)}>×</button>
            </div>
            <form onSubmit={handleCompleteCleaning}>
              <div className="modal-body">
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                  Confirming completion will transition this bed to <strong>AVAILABLE</strong> status and update the hospital capacity matrix.
                </p>
                <div className="form-group">
                  <label>Inspection Notes / Sanitization Log</label>
                  <textarea
                    className="form-control"
                    rows="3"
                    value={completionNotes}
                    onChange={(e) => setCompletionNotes(e.target.value)}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setCompletingBedId(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Confirm Cleaned &amp; Ready</button>
              </div>
            </form>
          </div>
        </div>
      )}

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
