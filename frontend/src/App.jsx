// src/App.jsx
import React from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import AuthWrapper from './components/auth/AuthWrapper';
import Header from './components/Header';
import DocumentUploader from './components/DocumentUploader';
import './App.css';

// Protected App Content (only shown when authenticated)
function AppContent() {
  const { currentUser } = useAuth();

  // Show auth forms if user is not logged in
  if (!currentUser) {
    return <AuthWrapper />;
  }

  // Show main app if user is logged in
  return (
    <>
      <Header />
      <DocumentUploader />
    </>
  );
}

// Main App Component
function App() {
  return (
    <div className="App">
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </div>
  );
}

export default App;
