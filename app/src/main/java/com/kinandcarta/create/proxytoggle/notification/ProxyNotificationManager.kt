package com.kinandcarta.create.proxytoggle.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kinandcarta.create.proxytoggle.R
import com.kinandcarta.create.proxytoggle.core.common.intent.getAppLaunchIntent
import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun showPersistentProxyNotification(proxy: Proxy) {
        if (proxy.isEnabled.not() || hasNotificationPermission().not()) {
            return
        }

        createNotificationChannelIfNeeded()

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.kinandcarta.create.proxytoggle.core.ui.R.drawable.ic_power)
                .setContentTitle(context.getString(R.string.proxy_notification_title))
                .setContentText(
                    context.getString(
                        R.string.proxy_notification_message,
                        proxy.toString()
                    )
                )
                .setContentIntent(createContentIntent())
                .addAction(
                    0,
                    context.getString(R.string.proxy_notification_disable_action),
                    createDisableIntent()
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setAutoCancel(false)
                .build()
        )
    }

    fun dismissPersistentProxyNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.proxy_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.proxy_notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createContentIntent(): PendingIntent? {
        val launchIntent = getAppLaunchIntent(context) ?: return null
        return PendingIntent.getActivity(
            context,
            CONTENT_INTENT_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDisableIntent(): PendingIntent {
        val disableIntent = Intent(context, ProxyNotificationActionReceiver::class.java).apply {
            action = ProxyNotificationActionReceiver.ACTION_DISABLE_PROXY
        }
        return PendingIntent.getBroadcast(
            context,
            DISABLE_INTENT_REQUEST_CODE,
            disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        internal const val CHANNEL_ID = "proxy_status"
        internal const val NOTIFICATION_ID = 1001
        private const val CONTENT_INTENT_REQUEST_CODE = 1002
        private const val DISABLE_INTENT_REQUEST_CODE = 1003
    }
}
