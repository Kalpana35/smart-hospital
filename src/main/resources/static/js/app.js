/**
 * SmartHospital - Frontend Dashboard Client
 * Communicates with Spring Boot REST APIs (/api/rooms, /api/beds, /api/patients, /api/allocations)
 */

class SmartHospitalApp {
    constructor() {
        this.rooms = [];
        this.beds = [];
        this.patients = [];

        this.init();
    }

    async init() {
        const authenticated = await this.checkAuth();
        if (!authenticated) return;

        this.initEventListeners();
        await this.refreshAll();
    }

    // -------------------------------------------------------------
    // Authentication & Session Management
    // -------------------------------------------------------------
    async checkAuth() {
        try {
            const response = await fetch('/api/auth/me');
            if (response.status === 401 || !response.ok) {
                window.location.replace('/login.html');
                return false;
            }
            const json = await response.json();
            if (!json.success || !json.data) {
                window.location.replace('/login.html');
                return false;
            }

            const currentUser = json.data;
            const nameEl = document.getElementById('current-user-name');
            const roleEl = document.getElementById('current-user-role');
            if (nameEl) nameEl.innerText = currentUser.fullName || currentUser.username;
            if (roleEl) roleEl.innerText = currentUser.role;

            return true;
        } catch {
            window.location.replace('/login.html');
            return false;
        }
    }

    async handleLogout() {
        try {
            await fetch('/api/auth/logout', { method: 'POST' });
        } catch (err) {
            console.error('Logout error:', err);
        } finally {
            window.location.replace('/login.html');
        }
    }

    async apiFetch(url, options = {}) {
        const response = await fetch(url, options);
        if (response.status === 401) {
            window.location.replace('/login.html');
            throw new Error('Session expired. Redirecting to login...');
        }
        return response;
    }

    // -------------------------------------------------------------
    // Core Navigation & Event Handling
    // -------------------------------------------------------------
    initEventListeners() {
        // Tab switching
        document.querySelectorAll('.nav-item').forEach(button => {
            button.addEventListener('click', (e) => {
                const targetTab = button.getAttribute('data-tab');
                this.switchTab(targetTab);
            });
        });
    }

    switchTab(tabId) {
        document.querySelectorAll('.nav-item').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.view-section').forEach(sec => sec.classList.remove('active'));

        const targetButton = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
        const targetSection = document.getElementById(`view-${tabId}`);

        if (targetButton && targetSection) {
            targetButton.classList.add('active');
            targetSection.classList.add('active');

            // Update topbar title
            const titles = {
                'overview': { title: 'Overview & Live Bed Matrix', desc: 'Real-time status of hospital rooms, capacity, and bed occupancies' },
                'rooms-beds': { title: 'Room & Bed Administration', desc: 'Create and configure hospital rooms and beds inventory' },
                'patients': { title: 'Patient Admissions & Discharges', desc: 'Admit incoming patients, assign beds, and handle discharges' },
                'recommendation': { title: 'Smart Bed Recommendation Engine', desc: 'Intelligently match patients to optimal available beds by care level' }
            };

            if (titles[tabId]) {
                document.getElementById('current-view-title').innerText = titles[tabId].title;
                document.getElementById('current-view-desc').innerText = titles[tabId].desc;
            }
        }
    }

    // -------------------------------------------------------------
    // Data Loading & API Calls
    // -------------------------------------------------------------
    async refreshAll() {
        try {
            await Promise.all([
                this.loadRooms(),
                this.loadBeds(),
                this.loadPatients()
            ]);

            this.updateKPIs();
            this.populateWardFilter();
            this.populateRoomSelects();
            this.populateAvailableBedSelect();
            this.renderRoomsAndBeds();
            this.renderBedsInventoryTable();
            this.renderPatientsTables();
        } catch (error) {
            this.showToast(error.message || 'Error communicating with backend server', 'error');
        }
    }

    async loadRooms() {
        const response = await this.apiFetch('/api/rooms');
        if (!response.ok) throw new Error('Failed to fetch rooms');
        const json = await response.json();
        this.rooms = json.data || [];
    }

    async loadBeds() {
        const response = await this.apiFetch('/api/beds');
        if (!response.ok) throw new Error('Failed to fetch beds');
        const json = await response.json();
        this.beds = json.data || [];
    }

    async loadPatients() {
        const response = await this.apiFetch('/api/patients');
        if (!response.ok) throw new Error('Failed to fetch patients');
        const json = await response.json();
        this.patients = json.data || [];
    }

    // -------------------------------------------------------------
    // KPI Metrics Calculation
    // -------------------------------------------------------------
    updateKPIs() {
        document.getElementById('stat-total-rooms').innerText = this.rooms.length;
        document.getElementById('stat-total-beds').innerText = this.beds.length;

        const available = this.beds.filter(b => b.status === 'AVAILABLE').length;
        const occupied = this.beds.filter(b => b.status === 'OCCUPIED').length;
        const maintenance = this.beds.filter(b => b.status === 'MAINTENANCE').length;
        const activePatients = this.patients.filter(p => p.status === 'ADMITTED').length;

        document.getElementById('stat-available-beds').innerText = available;
        document.getElementById('stat-occupied-beds').innerText = occupied;
        document.getElementById('stat-maintenance-beds').innerText = maintenance;
        document.getElementById('stat-active-patients').innerText = activePatients;
    }

    // -------------------------------------------------------------
    // View 1: Live Room & Bed Matrix Rendering
    // -------------------------------------------------------------
    renderRoomsAndBeds() {
        const container = document.getElementById('rooms-container');
        const selectedWard = document.getElementById('filter-ward').value;
        const selectedStatus = document.getElementById('filter-status').value;

        let filteredRooms = this.rooms;
        if (selectedWard) {
            filteredRooms = filteredRooms.filter(r => r.wardDepartment.toLowerCase() === selectedWard.toLowerCase());
        }

        if (filteredRooms.length === 0) {
            container.innerHTML = `<div class="card text-center text-muted full-width">No rooms found matching filter criteria.</div>`;
            return;
        }

        container.innerHTML = filteredRooms.map(room => {
            // Find beds belonging to this room
            let roomBeds = this.beds.filter(b => b.roomId === room.id);

            if (selectedStatus) {
                roomBeds = roomBeds.filter(b => b.status === selectedStatus);
            }

            const bedChips = roomBeds.length > 0
                ? roomBeds.map(bed => `
                    <div class="bed-chip status-${bed.status}" 
                         title="Bed #${bed.bedNumber} - ${bed.status}. Click to change status." 
                         onclick="app.quickStatusPrompt(${bed.id}, '${bed.bedNumber}', '${bed.status}')">
                        <span>🛏️</span>
                        <span>${this.escapeHtml(bed.bedNumber)}</span>
                    </div>
                `).join('')
                : `<span class="text-muted" style="font-size: 0.8rem;">No beds match criteria</span>`;

            return `
                <div class="room-card">
                    <div class="room-card-header">
                        <div>
                            <div class="room-number-badge">Room ${this.escapeHtml(room.roomNumber)}</div>
                            <div class="room-meta">${this.escapeHtml(room.roomType)} • ${this.escapeHtml(room.wardDepartment)}</div>
                        </div>
                        <span class="room-capacity-badge">${room.availableBeds}/${room.totalBeds} Available</span>
                    </div>
                    <div class="bed-chips-container">
                        ${bedChips}
                    </div>
                </div>
            `;
        }).join('');
    }

    populateWardFilter() {
        const select = document.getElementById('filter-ward');
        const currentVal = select.value;
        const wards = [...new Set(this.rooms.map(r => r.wardDepartment).filter(Boolean))].sort();

        select.innerHTML = '<option value="">All Wards</option>' +
            wards.map(w => `<option value="${this.escapeHtml(w)}">${this.escapeHtml(w)}</option>`).join('');

        select.value = currentVal;
    }

    populateRoomSelects() {
        const select = document.getElementById('bed-room-select');
        select.innerHTML = '<option value="">Select Target Room</option>' +
            this.rooms.map(r => `
                <option value="${r.id}">Room ${this.escapeHtml(r.roomNumber)} (${this.escapeHtml(r.roomType)} - ${this.escapeHtml(r.wardDepartment)})</option>
            `).join('');
    }

    populateAvailableBedSelect() {
        const select = document.getElementById('admission-bed-select');
        const availableBeds = this.beds.filter(b => b.status === 'AVAILABLE');

        select.innerHTML = '<option value="">Select an available bed...</option>' +
            availableBeds.map(b => `
                <option value="${b.id}">Bed ${this.escapeHtml(b.bedNumber)} - Room ${this.escapeHtml(b.roomNumber)} (${this.escapeHtml(b.wardDepartment)})</option>
            `).join('');
    }

    // -------------------------------------------------------------
    // View 2: Room & Bed Admin Operations
    // -------------------------------------------------------------
    async handleCreateRoom(e) {
        e.preventDefault();
        const payload = {
            roomNumber: document.getElementById('room-number').value.trim(),
            roomType: document.getElementById('room-type').value,
            wardDepartment: document.getElementById('room-ward').value.trim()
        };

        try {
            const res = await this.apiFetch('/api/rooms', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to create room');
            }

            this.showToast(`Room ${data.data.roomNumber} created successfully!`, 'success');
            document.getElementById('create-room-form').reset();
            this.refreshAll();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    }

    async handleCreateBed(e) {
        e.preventDefault();
        const payload = {
            roomId: parseInt(document.getElementById('bed-room-select').value),
            bedNumber: document.getElementById('bed-number').value.trim(),
            status: document.getElementById('bed-status').value
        };

        try {
            const res = await this.apiFetch('/api/beds', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to create bed');
            }

            this.showToast(`Bed ${data.data.bedNumber} added successfully!`, 'success');
            document.getElementById('create-bed-form').reset();
            this.refreshAll();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    }

    async updateBedStatus(bedId, newStatus) {
        try {
            const res = await this.apiFetch(`/api/beds/${bedId}/status?status=${encodeURIComponent(newStatus)}`, {
                method: 'PATCH'
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to update bed status');
            }

            this.showToast(`Bed ${data.data.bedNumber} status updated to ${newStatus}`, 'success');
            this.refreshAll();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    }

    quickStatusPrompt(bedId, bedNumber, currentStatus) {
        const nextStatus = prompt(`Change status for Bed ${bedNumber}?\nEnter AVAILABLE, OCCUPIED, or MAINTENANCE:`, currentStatus);
        if (nextStatus && ['AVAILABLE', 'OCCUPIED', 'MAINTENANCE'].includes(nextStatus.toUpperCase())) {
            this.updateBedStatus(bedId, nextStatus.toUpperCase());
        } else if (nextStatus !== null) {
            this.showToast('Invalid status entered. Must be AVAILABLE, OCCUPIED, or MAINTENANCE.', 'error');
        }
    }

    renderBedsInventoryTable() {
        const tbody = document.getElementById('beds-inventory-table');
        if (this.beds.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">No beds registered yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.beds.map(bed => `
            <tr>
                <td><strong>#${bed.id}</strong></td>
                <td>${this.escapeHtml(bed.bedNumber)}</td>
                <td>Room ${this.escapeHtml(bed.roomNumber)}</td>
                <td>${this.escapeHtml(bed.roomType || (this.rooms && this.rooms.find(r => r.id === bed.roomId)?.roomType) || 'Standard')}</td>
                <td>${this.escapeHtml(bed.wardDepartment)}</td>
                <td><span class="status-badge ${bed.status}">${bed.status}</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="app.updateBedStatus(${bed.id}, 'AVAILABLE')" ${bed.status === 'AVAILABLE' || bed.status === 'OCCUPIED' ? 'disabled' : ''} title="${bed.status === 'OCCUPIED' ? 'Cannot set OCCUPIED bed to AVAILABLE while patient is admitted' : 'Available'}">Available</button>
                    <button class="btn btn-secondary btn-sm" onclick="app.updateBedStatus(${bed.id}, 'MAINTENANCE')" ${bed.status === 'MAINTENANCE' || bed.status === 'OCCUPIED' ? 'disabled' : ''} title="${bed.status === 'OCCUPIED' ? 'Cannot set OCCUPIED bed to MAINTENANCE while patient is admitted' : 'Maintenance'}">Maintenance</button>
                </td>
            </tr>
        `).join('');
    }

    // -------------------------------------------------------------
    // View 3: Patient Admission & Discharge Operations
    // -------------------------------------------------------------
    async handleAdmitPatient(e) {
        e.preventDefault();
        const payload = {
            patientCode: document.getElementById('patient-code').value.trim(),
            name: document.getElementById('patient-name').value.trim(),
            age: parseInt(document.getElementById('patient-age').value),
            gender: document.getElementById('patient-gender').value,
            diagnosis: document.getElementById('patient-diagnosis').value.trim(),
            bedId: parseInt(document.getElementById('admission-bed-select').value)
        };

        try {
            const res = await this.apiFetch('/api/patients/admit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to admit patient');
            }

            this.showToast(`Patient ${data.data.name} admitted to Bed ${data.data.bedNumber}!`, 'success');
            document.getElementById('admit-patient-form').reset();
            this.refreshAll();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    }

    async handleDischargePatient(patientId, patientName, bedNumber) {
        if (!confirm(`Are you sure you want to discharge patient ${patientName}? Bed ${bedNumber} will become AVAILABLE.`)) {
            return;
        }

        try {
            const res = await this.apiFetch(`/api/patients/${patientId}/discharge`, {
                method: 'POST'
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to discharge patient');
            }

            this.showToast(`Patient ${data.data.name} discharged successfully. Bed released!`, 'success');
            this.refreshAll();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    }

    renderPatientsTables() {
        const activeTable = document.getElementById('active-patients-table');
        const dischargedTable = document.getElementById('discharged-patients-table');

        const activePatients = this.patients.filter(p => p.status === 'ADMITTED');
        const dischargedPatients = this.patients.filter(p => p.status === 'DISCHARGED');

        // Active inpatients
        if (activePatients.length === 0) {
            activeTable.innerHTML = `<tr><td colspan="9" class="text-center text-muted">No active inpatients at this time.</td></tr>`;
        } else {
            activeTable.innerHTML = activePatients.map(p => `
                <tr>
                    <td><strong>#${p.id}</strong></td>
                    <td><code>${this.escapeHtml(p.patientCode)}</code></td>
                    <td><strong>${this.escapeHtml(p.name)}</strong></td>
                    <td>${p.age} / ${p.gender}</td>
                    <td>${this.escapeHtml(p.diagnosis || '-')}</td>
                    <td><span class="status-badge OCCUPIED">${this.escapeHtml(p.bedNumber || 'Assigned')}</span></td>
                    <td>Room ${this.escapeHtml(p.roomNumber || '-')} (${this.escapeHtml(p.wardDepartment || '-')})</td>
                    <td>${this.formatDateTime(p.admissionDate)}</td>
                    <td>
                        <button class="btn btn-danger btn-sm" onclick="app.handleDischargePatient(${p.id}, '${this.escapeHtml(p.name)}', '${this.escapeHtml(p.bedNumber || '')}')">
                            Discharge
                        </button>
                    </td>
                </tr>
            `).join('');
        }

        // Discharged history
        if (dischargedPatients.length === 0) {
            dischargedTable.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No discharged patients on record.</td></tr>`;
        } else {
            dischargedTable.innerHTML = dischargedPatients.map(p => `
                <tr>
                    <td>#${p.id}</td>
                    <td><code>${this.escapeHtml(p.patientCode)}</code></td>
                    <td>${this.escapeHtml(p.name)}</td>
                    <td>${this.escapeHtml(p.diagnosis || '-')}</td>
                    <td>${this.escapeHtml(p.bedNumber || 'Released')}</td>
                    <td>${this.formatDateTime(p.admissionDate)}</td>
                    <td>${this.formatDateTime(p.dischargeDate)}</td>
                    <td><span class="status-badge DISCHARGED">DISCHARGED</span></td>
                </tr>
            `).join('');
        }
    }

    // -------------------------------------------------------------
    // View 4: Smart Bed Recommendation Engine
    // -------------------------------------------------------------
    async handleRecommendBeds(e) {
        e.preventDefault();
        const payload = {
            roomType: document.getElementById('rec-room-type').value,
            wardDepartment: document.getElementById('rec-ward').value.trim()
        };

        const container = document.getElementById('recommendation-results');
        container.innerHTML = `<p class="text-muted">Evaluating available hospital beds...</p>`;

        try {
            const res = await this.apiFetch('/api/allocations/recommend', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to fetch recommendations');
            }

            const recs = data.data || [];
            if (recs.length === 0) {
                container.innerHTML = `<div class="card text-center text-muted">No suitable available beds found matching this care requirement.</div>`;
                return;
            }

            container.innerHTML = recs.map((rec, index) => {
                const isExact = rec.matchScore === 100;
                return `
                    <div class="recommendation-card score-${rec.matchScore}">
                        <div class="rec-info">
                            <span class="rec-score-badge ${isExact ? 'exact' : 'clinical'}">
                                ${isExact ? '⭐ 100% Exact Match' : '⚕️ 75% Cross-Ward Clinical Match'}
                            </span>
                            <h4>Bed ${this.escapeHtml(rec.bedNumber)} — Room ${this.escapeHtml(rec.roomNumber)} (${this.escapeHtml(rec.roomType)})</h4>
                            <p><strong>Ward:</strong> ${this.escapeHtml(rec.wardDepartment)}</p>
                            <p class="text-muted"><em>${this.escapeHtml(rec.matchReason)}</em></p>
                        </div>
                        <div>
                            <button class="btn btn-primary" onclick="app.selectRecommendedBed(${rec.bedId})">
                                Select & Admit Here
                            </button>
                        </div>
                    </div>
                `;
            }).join('');

        } catch (err) {
            container.innerHTML = `<div class="card text-center text-danger">${err.message}</div>`;
            this.showToast(err.message, 'error');
        }
    }

    selectRecommendedBed(bedId) {
        this.switchTab('patients');
        const bedSelect = document.getElementById('admission-bed-select');
        bedSelect.value = bedId;
        this.showToast('Bed selected in Admission form!', 'info');
        document.getElementById('patient-code').focus();
    }

    // -------------------------------------------------------------
    // Toast Notification System
    // -------------------------------------------------------------
    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerText = message;

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    // Helper functions
    escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    formatDateTime(dtStr) {
        if (!dtStr) return '-';
        try {
            const d = new Date(dtStr);
            return d.toLocaleString();
        } catch {
            return dtStr;
        }
    }
}

// Instantiate application on page load
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new SmartHospitalApp();
});

