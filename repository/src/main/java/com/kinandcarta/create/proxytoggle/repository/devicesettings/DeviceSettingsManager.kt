package com.kinandcarta.create.proxytoggle.repository.devicesettings

import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import kotlinx.coroutines.flow.StateFlow

interface DeviceSettingsManager {
    val proxySetting: StateFlow<Proxy>
    val proxyNetworkStatus: StateFlow<ProxyNetworkStatus>
    suspend fun enableProxy(proxy: Proxy)
    fun disableProxy()
    fun refreshProxy()
}
