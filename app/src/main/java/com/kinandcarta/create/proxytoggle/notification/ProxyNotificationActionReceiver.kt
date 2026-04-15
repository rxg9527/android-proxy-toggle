package com.kinandcarta.create.proxytoggle.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kinandcarta.create.proxytoggle.repository.devicesettings.DeviceSettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProxyNotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var deviceSettingsManager: DeviceSettingsManager

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_DISABLE_PROXY) {
            deviceSettingsManager.disableProxy()
        }
    }

    companion object {
        const val ACTION_DISABLE_PROXY =
            "com.kinandcarta.create.proxytoggle.notification.DISABLE_PROXY"
    }
}
