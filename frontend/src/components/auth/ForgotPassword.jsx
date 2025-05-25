import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import './Auth.css';

const ForgotPassword = ({ onBackToLogin }) => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const { resetPassword } = useAuth();

  async function handleSubmit(e) {
    e.preventDefault();

    try {
      setMessage('');
      setError('');
      setLoading(true);
      //Use resetPassword function from AuthContext
      await resetPassword(email);
      setMessage('Check your inbox for password reset instructions');
    } catch (error) {
      console.error('Password reset error:', error);
      setError('Failed to reset password: ' + error.message);
    }
    setLoading(false);
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Password Reset</h2>
        <p className="auth-subtitle">Enter your email to reset your password</p>

        {error && <div className="error-message">{error}</div>}
        {message && <div className="success-message">{message}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="Enter your email"
            />
          </div>

          <button type="submit" disabled={loading} className="auth-btn primary">
            {loading ? 'Sending...' : 'Send Reset Email'}
          </button>
        </form>

        <div className="auth-switch">
          <p>
            Remember your password?{' '}
            <button type="button" onClick={onBackToLogin} className="link-btn">
              Back to Login
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
