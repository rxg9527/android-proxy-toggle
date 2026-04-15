package com.kinandcarta.create.proxytoggle.notification

import android.content.Context
import android.provider.Settings
import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import com.kinandcarta.create.proxytoggle.core.common.proxyupdate.ProxyUpdateListener
import com.kinandcarta.create.proxytoggle.repository.proxymapper.ProxyMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ProxyNotificationUpdateListener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proxyMapper: ProxyMapper,
    private val proxyNotificationManager: ProxyNotificationManager
) : ProxyUpdateListener {

    override fun onProxyUpdate() {
        when (val proxy = getCurrentProxy()) {
            Proxy.Disabled -> proxyNotificationManager.dismissPersistentProxyNotification()
            else -> proxyNotificationManager.showPersistentProxyNotification(proxy)
        }
    }

    private fun getCurrentProxy(): Proxy {
        val proxySetting = Settings.Global.getString(
            context.contentResolver,
            Settings.Global.HTTP_PROXY
        )
        return proxyMapper.from(proxySetting)
    }
}
