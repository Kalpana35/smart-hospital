/**
 * SmartHospital - Login Handler
 * Connects to POST /api/auth/login and manages role-based redirects.
 * Does NOT store passwords in browser storage.
 */

document.addEventListener('DOMContentLoaded', async () => {
    // If the browser already has an active session, redirect to the dashboard
    try {
        const res = await fetch('/api/auth/me');
        if (res.ok) {
            const json = await res.json();
            if (json.data && json.data.role === 'ROLE_ADMIN') {
                window.location.replace('/');
            }
        }
    } catch {
        // Unauthenticated session, stay on login page
    }
});

async function handleLogin(e) {
    e.preventDefault();

    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const submitBtn = document.getElementById('btn-submit');
    const alertBox = document.getElementById('login-alert');

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    if (!username || !password) {
        showAlert('Please enter both username and password.', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerText = 'Signing In...';
    alertBox.style.display = 'none';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        // Always clear password field immediately from memory and DOM
        passwordInput.value = '';

        const result = await response.json();

        if (response.ok && result.success) {
            showAlert('Login successful! Redirecting...', 'info');
            // Redirect admin to dashboard
            setTimeout(() => {
                window.location.href = '/';
            }, 300);
        } else {
            // Display sanitized, safe error message
            showAlert(result.message || 'Invalid username or password. Please try again.', 'error');
            passwordInput.focus();
        }
    } catch (err) {
        showAlert('Unable to reach authentication server. Please check your network connection.', 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerText = 'Sign In';
    }
}

function showAlert(message, type) {
    const alertBox = document.getElementById('login-alert');
    alertBox.className = `login-alert ${type}`;
    alertBox.innerText = message;
    alertBox.style.display = 'block';
}

