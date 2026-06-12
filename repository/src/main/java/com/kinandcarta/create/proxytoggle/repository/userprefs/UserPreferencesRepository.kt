package com.kinandcarta.create.proxytoggle.repository.userprefs

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val isNightMode: Flow<Boolean>
    val proxyScope: Flow<ProxyScope>
    val proxyNetworkSsid: Flow<String>

    suspend fun toggleTheme()
    suspend fun setProxyScope(proxyScope: ProxyScope)
    suspend fun setProxyNetworkSsid(ssid: String)
}
