import { useCallback, useEffect, useState } from 'react'
import './PatientDashboard.css'

export default function PatientDashboard({ user, onLogout }) {
  const [patientData, setPatientData] = useState(null)
  const [appointments, setAppointments] = useState([])
  const [doctors, setDoctors] = useState([])
  const [consultations, setConsultations] = useState([])
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState(null)

  // Booking modal state
  const [showBookModal, setShowBookModal] = useState(false)
  const [selectedDoctorId, setSelectedDoctorId] = useState('')
  const [selectedDept, setSelectedDept] = useState('General Medicine')
  const [appointmentDateTime, setAppointmentDateTime] = useState('')
  const [reason, setReason] = useState('')
  const [submittingBooking, setSubmittingBooking] = useState(false)

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type })
    setTimeout(() => {
      setToast(null)
    }, 4000)
  }, [])

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      const [patientRes, apptRes, docRes, consultRes] = await Promise.all([
        fetch('/api/patients/me', { credentials: 'include' }),
        fetch('/api/appointments/my', { credentials: 'include' }),
        fetch('/api/appointments/doctors', { credentials: 'include' }),
        fetch('/api/consultations/my', { credentials: 'include' }),
      ])

      if (patientRes.ok) {
        const pJson = await patientRes.json()
        setPatientData(pJson.data || null)
      } else {
        setPatientData(null)
      }

      if (apptRes.ok) {
        const aJson = await apptRes.json()
        setAppointments(aJson.data || [])
      }

      if (docRes.ok) {
        const dJson = await docRes.json()
        if (dJson.success && dJson.data) {
          setDoctors(dJson.data)
          if (dJson.data.length > 0 && !selectedDoctorId) {
            setSelectedDoctorId(String(dJson.data[0].id))
          }
        }
      }

      if (consultRes.ok) {
        const cJson = await consultRes.json()
        setConsultations(cJson.data || [])
      }
    } catch (err) {
      showToast('Error loading patient data: ' + err.message, 'error')
    } finally {
      setLoading(false)
    }
  }, [selectedDoctorId, showToast])

  useEffect(() => {
    let ignore = false

    const initLoad = async () => {
      try {
        const [patientRes, apptRes, docRes, consultRes] = await Promise.all([
          fetch('/api/patients/me', { credentials: 'include' }),
          fetch('/api/appointments/my', { credentials: 'include' }),
          fetch('/api/appointments/doctors', { credentials: 'include' }),
          fetch('/api/consultations/my', { credentials: 'include' }),
        ])

        if (ignore) return

        if (patientRes.ok) {
          const pJson = await patientRes.json()
          setPatientData(pJson.data || null)
        } else {
          setPatientData(null)
        }

        if (apptRes.ok) {
          const aJson = await apptRes.json()
          setAppointments(aJson.data || [])
        }

        if (docRes.ok) {
          const dJson = await docRes.json()
          if (dJson.success && dJson.data) {
            setDoctors(dJson.data)
            if (dJson.data.length > 0) {
              setSelectedDoctorId(String(dJson.data[0].id))
            }
          }
        }

        if (consultRes.ok) {
          const cJson = await consultRes.json()
          setConsultations(cJson.data || [])
        }
      } catch (err) {
        if (!ignore) {
          showToast('Error loading patient data: ' + err.message, 'error')
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
  }, [showToast])

  const handleBookAppointment = async (e) => {
    e.preventDefault()
    if (!selectedDoctorId) {
      showToast('Please select a doctor', 'error')
      return
    }
    if (!appointmentDateTime) {
      showToast('Please select date and time', 'error')
      return
    }
    if (!reason.trim()) {
      showToast('Please enter reason for visit', 'error')
      return
    }

    setSubmittingBooking(true)
    try {
      const res = await fetch('/api/appointments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          doctorId: Number(selectedDoctorId),
          appointmentDateTime,
          department: selectedDept,
          reason: reason.trim(),
        }),
      })

      const data = await res.json()
      if (res.ok && data.success) {
        showToast('Appointment booked successfully!', 'success')
        setShowBookModal(false)
        setReason('')
        setAppointmentDateTime('')
        loadData()
      } else {
        showToast(data.message || 'Failed to book appointment', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setSubmittingBooking(false)
    }
  }

  const handleCancelAppointment = async (id) => {
    if (!window.confirm('Are you sure you want to cancel this appointment?')) {
      return
    }

    try {
      const res = await fetch(`/api/appointments/${id}/cancel`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ reason: 'Cancelled by patient' }),
      })
      const data = await res.json()
      if (res.ok && data.success) {
        showToast('Appointment cancelled', 'info')
        loadData()
      } else {
        showToast(data.message || 'Failed to cancel appointment', 'error')
      }
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const handleLogout = async () => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      })
    } catch {
      // ignore
    } finally {
      if (onLogout) onLogout()
    }
  }

  return (
    <div className="app-container">
      {/* Toast Notification */}
      {toast && (
        <div className={`toast toast-${toast.type}`} role="alert">
          {toast.message}
        </div>
      )}

      {/* Main Content */}
      <main className="main-content" style={{ marginLeft: 0, width: '100%' }}>
        {/* Topbar */}
        <header className="topbar">
          <div className="page-title">
            <h1>🏥 Patient Portal</h1>
            <p>Personal Admission Records, Bed Assignment & Appointment Schedule</p>
          </div>

          <div className="topbar-actions">
            <div className="user-badge">
              <span className="user-avatar">👤</span>
              <div className="user-details">
                <span className="user-name">
                  {user?.fullName || user?.username || 'Patient'}
                </span>
                <span className="user-role-tag">ROLE_PATIENT</span>
              </div>
            </div>

            <button
              className="btn btn-secondary"
              onClick={loadData}
              disabled={loading}
              title="Refresh patient records"
            >
              <span>🔄</span> Refresh
            </button>

            <button
              className="btn btn-secondary"
              onClick={handleLogout}
              title="Sign out"
            >
              <span>🚪</span> Logout
            </button>
          </div>
        </header>

        <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
          {/* Clinical Record Status Card */}
          <div className="patient-portal-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <h2 style={{ margin: 0, fontSize: '1.4rem', color: 'var(--text-primary)' }}>
                  Clinical Admission & Bed Status
                </h2>
                <p style={{ margin: '0.25rem 0 0 0', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  Your official hospital inpatient / outpatient assignment
                </p>
              </div>
              {patientData && (
                <span className={patientData.status === 'ADMITTED' ? 'badge-admitted' : 'badge-discharged'}>
                  ● {patientData.status}
                </span>
              )}
            </div>

            {patientData ? (
              <div className="patient-info-grid">
                <div className="info-item">
                  <span className="label">Patient MRN / Code</span>
                  <span className="value highlight">{patientData.patientCode}</span>
                </div>
                <div className="info-item">
                  <span className="label">Full Name</span>
                  <span className="value">{patientData.name}</span>
                </div>
                <div className="info-item">
                  <span className="label">Age & Gender</span>
                  <span className="value">{patientData.age} yrs / {patientData.gender}</span>
                </div>
                <div className="info-item">
                  <span className="label">Assigned Bed</span>
                  <span className="value highlight">
                    {patientData.bedNumber ? `Bed ${patientData.bedNumber}` : 'None (Discharged / Outpatient)'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="label">Room & Care Level</span>
                  <span className="value">
                    {patientData.roomNumber ? `Room ${patientData.roomNumber} (${patientData.roomType || 'Standard'})` : 'N/A'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="label">Ward / Department</span>
                  <span className="value">{patientData.wardDepartment || 'General Medicine'}</span>
                </div>
                <div className="info-item">
                  <span className="label">Daily Bed Rate</span>
                  <span className="value highlight">
                    {patientData.dailyRate ? `$${patientData.dailyRate} / day` : 'Standard Rate'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="label">Admission Date</span>
                  <span className="value">
                    {patientData.admissionDate ? new Date(patientData.admissionDate).toLocaleString() : 'N/A'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="label">Expected Discharge</span>
                  <span className="value" style={{ color: patientData.expectedDischargeDate ? '#10b981' : 'inherit' }}>
                    {patientData.expectedDischargeDate ? new Date(patientData.expectedDischargeDate).toLocaleString() : 'Under Clinical Evaluation'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="label">Diagnosis</span>
                  <span className="value">{patientData.diagnosis || 'Clinical evaluation'}</span>
                </div>
              </div>
            ) : (
              <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                {loading ? (
                  <span>Loading clinical records...</span>
                ) : (
                  <div>
                    <p style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                      No Clinical Patient Record Linked
                    </p>
                    <p style={{ fontSize: '0.9rem' }}>
                      This account is currently registered as an outpatient without an active inpatient admission record.
                      You can schedule consultations and doctor appointments below.
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Appointments Card */}
          <div className="patient-portal-card">
            <div className="appointment-header-row">
              <div>
                <h2 style={{ margin: 0, fontSize: '1.4rem', color: 'var(--text-primary)' }}>
                  My Doctor Appointments & Consultations
                </h2>
                <p style={{ margin: '0.25rem 0 0 0', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  Manage and schedule medical visits with hospital specialists
                </p>
              </div>

              <button
                type="button"
                className="btn-book"
                onClick={() => setShowBookModal(true)}
              >
                <span>📅</span> Book New Appointment
              </button>
            </div>

            <div className="table-responsive" style={{ marginTop: '1rem' }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date & Time</th>
                    <th>Doctor</th>
                    <th>Department</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th>Clinical Notes</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.length === 0 ? (
                    <tr>
                      <td colSpan="7" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
                        No appointments scheduled. Click &quot;Book New Appointment&quot; above to schedule a visit.
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
                        <td>Dr. {appt.doctorName}</td>
                        <td>{appt.department}</td>
                        <td>{appt.reason}</td>
                        <td>
                          <span
                            className={
                              appt.status === 'SCHEDULED'
                                ? 'badge-scheduled'
                                : appt.status === 'COMPLETED'
                                ? 'badge-completed'
                                : 'badge-cancelled'
                            }
                          >
                            ● {appt.status}
                          </span>
                        </td>
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', maxWidth: '250px' }}>
                          {appt.notes || '—'}
                        </td>
                        <td>
                          {appt.status === 'SCHEDULED' ? (
                            <button
                              type="button"
                              className="btn-cancel-appt"
                              onClick={() => handleCancelAppointment(appt.id)}
                            >
                              Cancel
                            </button>
                          ) : (
                            <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>—</span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Medical Consultations & Prescriptions Card */}
          <div className="patient-portal-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <h2 style={{ margin: 0, fontSize: '1.4rem', color: 'var(--text-primary)' }}>
                  My Medical Prescriptions &amp; Clinical Notes
                </h2>
                <p style={{ margin: '0.25rem 0 0 0', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  Official diagnoses, care plans, and prescribed medications from hospital physicians
                </p>
              </div>
              <span style={{ background: '#dbeafe', color: '#1e40af', padding: '0.35rem 0.75rem', borderRadius: '12px', fontSize: '0.85rem', fontWeight: 600 }}>
                {consultations.length} Record{consultations.length !== 1 ? 's' : ''}
              </span>
            </div>

            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date &amp; Time</th>
                    <th>Attending Doctor</th>
                    <th>Clinical Diagnosis</th>
                    <th>Treatment Plan</th>
                    <th>Prescription (Rx) &amp; Dosage</th>
                    <th>Physician Notes</th>
                  </tr>
                </thead>
                <tbody>
                  {consultations.length === 0 ? (
                    <tr>
                      <td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
                        No medical consultations or prescriptions recorded yet.
                      </td>
                    </tr>
                  ) : (
                    consultations.map((c) => (
                      <tr key={c.id}>
                        <td style={{ fontWeight: 600 }}>
                          {c.consultationDateTime ? new Date(c.consultationDateTime).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : 'N/A'}
                        </td>
                        <td>Dr. {c.doctorName || 'Hospital Physician'}</td>
                        <td>
                          <span style={{ background: '#eff6ff', color: '#1d4ed8', padding: '0.25rem 0.5rem', borderRadius: '4px', fontWeight: 600, fontSize: '0.85rem' }}>
                            {c.diagnosis}
                          </span>
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
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', maxWidth: '250px' }}>
                          {c.notes || '—'}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Book Appointment Modal */}
        {showBookModal && (
          <div className="modal-backdrop">
            <div className="modal-dialog">
              <div className="modal-header">
                <h3>Book Doctor Appointment</h3>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowBookModal(false)}
                >
                  ×
                </button>
              </div>

              <form onSubmit={handleBookAppointment}>
                <div className="modal-body">
                  <div className="form-group">
                    <label>Select Doctor *</label>
                    <select
                      className="form-control"
                      value={selectedDoctorId}
                      onChange={(e) => setSelectedDoctorId(e.target.value)}
                      required
                    >
                      {doctors.map((d) => (
                        <option key={d.id} value={d.id}>
                          Dr. {d.fullName} (@{d.username})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Department / Specialty</label>
                    <select
                      className="form-control"
                      value={selectedDept}
                      onChange={(e) => setSelectedDept(e.target.value)}
                    >
                      <option value="General Medicine">General Medicine</option>
                      <option value="Cardiology">Cardiology</option>
                      <option value="Critical Care / ICU">Critical Care / ICU</option>
                      <option value="Neurology">Neurology</option>
                      <option value="Orthopedics">Orthopedics</option>
                      <option value="Emergency Care">Emergency Care</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Date & Time *</label>
                    <input
                      type="datetime-local"
                      className="form-control"
                      value={appointmentDateTime}
                      onChange={(e) => setAppointmentDateTime(e.target.value)}
                      min={new Date().toISOString().slice(0, 16)}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Reason for Visit *</label>
                    <textarea
                      className="form-control"
                      rows="3"
                      placeholder="Describe symptoms, reason for consultation, or follow-up..."
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowBookModal(false)}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={submittingBooking}
                  >
                    {submittingBooking ? 'Booking...' : 'Confirm Appointment'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
