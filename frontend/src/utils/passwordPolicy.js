export const validateStrongPassword = (password) => {
  const errors = []
  if (!password || password.length < 6) {
    errors.push('At least 6 characters long')
  }
  if (!/[A-Z]/.test(password || '')) {
    errors.push('At least one uppercase letter (A-Z)')
  }
  if (!/[a-z]/.test(password || '')) {
    errors.push('At least one lowercase letter (a-z)')
  }
  if (!/[0-9]/.test(password || '')) {
    errors.push('At least one number (0-9)')
  }
  if (!/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?`~]/.test(password || '')) {
    errors.push('At least one special character (e.g. @, #, $, !)')
  }
  return errors
}

export const isPasswordStrong = (password) => {
  return validateStrongPassword(password).length === 0
}
