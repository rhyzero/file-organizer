import React, { createContext, useContext, useEffect, useState } from 'react';
import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  sendPasswordResetEmail,
  updateProfile,
  getIdToken as getFirebaseIdToken,
} from 'firebase/auth';
import { auth } from '../services/firebase';

const AuthContext = createContext();

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  //Sign up function
  function signup(email, password, displayName) {
    return createUserWithEmailAndPassword(auth, email, password).then(
      (result) => {
        //Update the user's display name
        if (displayName) {
          return updateProfile(result.user, {
            displayName: displayName,
          });
        }
      }
    );
  }

  //Login function
  function login(email, password) {
    return signInWithEmailAndPassword(auth, email, password);
  }

  //Logout function
  function logout() {
    return signOut(auth);
  }

  //Reset password function
  function resetPassword(email) {
    return sendPasswordResetEmail(auth, email);
  }

  //Get current user's ID token (for backend authentication)
  async function getIdToken() {
    if (currentUser) {
      try {
        const token = await getFirebaseIdToken(currentUser);
        return token;
      } catch (error) {
        console.error('Error getting ID token:', error);
        return null;
      }
    }
    console.warn('No current user available for token');
    return null;
  }

  // Clear error function
  function clearError() {
    setError('');
  }

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user);
      setLoading(false);
    });

    return unsubscribe;
  }, []);

  const value = {
    currentUser,
    login,
    signup,
    logout,
    resetPassword,
    getIdToken,
    error,
    clearError,
    loading,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
}
