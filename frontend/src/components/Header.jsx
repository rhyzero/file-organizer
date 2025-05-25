import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import './Auth/Auth.css';

const Header = () => {
  const { currentUser, logout } = useAuth();

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  if (!currentUser) {
    return null;
  }

  return (
    <header className="app-header">
      <div className="header-brand">Document Classifier</div>
      <div className="user-info">
        <span className="user-name">
          Welcome, {currentUser.displayName || currentUser.email}
        </span>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </div>
    </header>
  );
};

export default Header;
