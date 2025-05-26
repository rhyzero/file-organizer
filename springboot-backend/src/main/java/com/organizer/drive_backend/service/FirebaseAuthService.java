package com.organizer.drive_backend.service;

import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

@Service
public class FirebaseAuthService {
    public String verifyTokenAndGetUID(String idToken) throws Exception {
        try {
            //Verify the Firebase ID token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return decodedToken.getUid(); //Return Firebase UID
        } catch (Exception e) {
            throw new Exception("Invalid Firebase token: " + e.getMessage());
        }
    }
}
