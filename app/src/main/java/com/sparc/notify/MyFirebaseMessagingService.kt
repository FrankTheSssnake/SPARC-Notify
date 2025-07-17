package com.sparc.notify

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("FCM", "New token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i("FCM", "Message received from: ${remoteMessage.from}")
        Log.i("FCM", "Message data: ${remoteMessage.data}")
        val type = remoteMessage.data["type"] ?: ""
        val code = remoteMessage.from?.removePrefix("/topics/") ?: ""
        if (shouldShowNotification(this, code, type)) {
            remoteMessage.notification?.let {
                Log.i("FCM", "Notification title: ${it.title}")
                Log.i("FCM", "Notification body: ${it.body}")
                showNotification(it.title, it.body, type)
            }
        } else {
            Log.i("FCM", "Notification for code=$code and type=$type filtered out by user preferences.")
        }
    }

    private fun shouldShowNotification(context: Context, code: String, type: String): Boolean {
        val prefs = context.getSharedPreferences("codes", Context.MODE_PRIVATE)
        val json = prefs.getString("patient_codes", null) ?: return false
        val obj = JSONObject(json)
        if (!obj.has(code)) return false
        val arr = obj.getJSONArray(code)
        for (i in 0 until arr.length()) {
            if (arr.getString(i).equals(type, ignoreCase = true)) return true
        }
        return false
    }

    private fun showNotification(title: String?, body: String?, type: String?) {
        val channelId = if (type.equals("EMERGENCY", ignoreCase = true)) {
            "emergency_channel"
        } else {
            "default_channel"
        }
        val soundUri = if (type.equals("EMERGENCY", ignoreCase = true)) {
            android.net.Uri.parse("android.resource://" + packageName + "/" + R.raw.emergency)
        } else {
            android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelName = if (type.equals("EMERGENCY", ignoreCase = true)) {
                "Emergency Notifications"
            } else {
                "Default Notifications"
            }
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, channelName, importance)
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.lockscreenVisibility = androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
            channel.setSound(soundUri, null)
            val manager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)
        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: "SPARC Notify")
            .setContentText(body ?: "")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(androidx.core.app.NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}