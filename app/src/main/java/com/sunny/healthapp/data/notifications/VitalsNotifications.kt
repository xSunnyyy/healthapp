package com.sunny.healthapp.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.sunny.healthapp.MainActivity

object VitalsNotifications {

    private const val CHANNEL_ID = "vitals.daily"
    private const val CHANNEL_NAME = "Vitals daily check-ins"
    const val ID_MORNING = 1001
    const val ID_GOAL = 1002
    const val ID_BEDTIME = 1003

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morning briefing, evening goal nudge, bedtime reminder."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun post(
        context: Context,
        id: Int,
        title: String,
        body: String,
    ) {
        ensureChannel(context)
        val manager = context.getSystemService<NotificationManager>() ?: return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        manager.notify(id, notification)
    }
}
