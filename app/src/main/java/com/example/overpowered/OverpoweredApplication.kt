package com.example.overpowered

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging

class OverpoweredApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Enable Firestore offline persistence
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings


        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                saveTokenToFirestore(token)
            }
    }

    private fun saveTokenToFirestore(token: String) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            // Save token locally until sign-in happens
            getSharedPreferences("fcm", MODE_PRIVATE)
                .edit()
                .putString("pendingToken", token)
                .apply()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
    }

}