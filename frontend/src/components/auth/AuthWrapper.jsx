import React, { useState } from 'react';
import Login from './Login';
import Signup from './Signup';
import ForgotPassword from './ForgotPassword';

const AuthWrapper = () => {
  //State to track the current page
  const [authMode, setAuthMode] = useState('login'); //'login', 'signup', 'forgot'

  //List of functions to change states when clicking on the corresponding buttons
  const handleToggleSignup = () => {
    setAuthMode('signup');
  };

  const handleToggleLogin = () => {
    setAuthMode('login');
  };

  const handleForgotPassword = () => {
    setAuthMode('forgot');
  };

  const handleBackToLogin = () => {
    setAuthMode('login');
  };

  return (
    <>
      {authMode === 'login' && (
        //If auth state is set to login, render login component
        <Login
          onToggleSignup={handleToggleSignup}
          onForgotPassword={handleForgotPassword}
        />
      )}
      {authMode === 'signup' && <Signup onToggleLogin={handleToggleLogin} />}
      {authMode === 'forgot' && (
        <ForgotPassword onBackToLogin={handleBackToLogin} />
      )}
    </>
  );
};

export default AuthWrapper;
