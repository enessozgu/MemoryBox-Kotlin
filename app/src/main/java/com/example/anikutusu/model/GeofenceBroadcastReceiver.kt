package com.example.anikutusu.model

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

// BroadcastReceiver to handle geofence transitions (e.g., user entering a marked location)
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    // Called automatically when a geofence transition (like ENTER) occurs
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // Extract geofencing event from the received intent
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            // If the geofencing event is invalid or has an error, exit early
            return
        }

        // Get the type of transition (ENTER, EXIT, DWELL)
        val geofenceTransition = geofencingEvent.geofenceTransition

        // If user has entered the geofence area
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Trigger a notification to alert the user
            sendNotification(context, "Anı Kutusu", "Yakınlardasınız! İşaretli anıya geldiniz.")
        }

        // You can also handle EXIT or DWELL events if needed
    }

    // Helper function to send a notification to the user
    private fun sendNotification(context: Context, title: String, message: String) {
        val channelId = "geofence_channel"

        // Get the system's notification manager service
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel if running on Android 8.0 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Geofence Bildirimleri", // Channel name (can be localized)
                NotificationManager.IMPORTANCE_HIGH // High priority for alerting the user
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Build the actual notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_map) // Default Android icon (can be replaced with your own)
            .setContentTitle(title) // Title of the notification
            .setContentText(message) // Body text of the notification
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensure it pops up
            .setAutoCancel(true) // Dismiss when clicked
            .build()

        // Show the notification
        notificationManager.notify(1, notification)
    }
}
