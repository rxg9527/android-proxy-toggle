package com.kinandcarta.create.proxytoggle.repository.devicesettings

import android.content.Context
import android.provider.Settings
import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import com.kinandcarta.create.proxytoggle.core.common.proxyupdate.ProxyUpdateNotifier
import com.kinandcarta.create.proxytoggle.repository.appdata.AppDataRepository
import com.kinandcarta.create.proxytoggle.repository.network.NetworkState
import com.kinandcarta.create.proxytoggle.repository.network.NetworkStateProvider
import com.kinandcarta.create.proxytoggle.repository.proxymapper.ProxyMapper
import com.kinandcarta.create.proxytoggle.repository.userprefs.ProxyScope
import com.kinandcarta.create.proxytoggle.repository.userprefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSettingsManagerImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val proxyMapper: ProxyMapper,
    private val proxyUpdateNotifier: ProxyUpdateNotifier,
    private val appDataRepository: AppDataRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val networkStateProvider: NetworkStateProvider
) : DeviceSettingsManager {

    private val contentResolver by lazy { context.contentResolver }
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _proxySetting = MutableStateFlow(Proxy.Disabled)
    override val proxySetting = _proxySetting.asStateFlow()

    private val _proxyNetworkStatus = MutableStateFlow(ProxyNetworkStatus())
    override val proxyNetworkStatus = _proxyNetworkStatus.asStateFlow()

    init {
        updateProxyData()
        coroutineScope.launch {
            combine(
                appDataRepository.desiredProxy,
                userPreferencesRepository.proxyScope,
                userPreferencesRepository.proxyNetworkSsid,
                networkStateProvider.networkState
            ) { desiredProxy, proxyScope, proxyNetworkSsid, networkState ->
                ProxyApplicationState(
                    desiredProxy = desiredProxy,
                    proxyScope = proxyScope,
                    proxyNetworkSsid = proxyNetworkSsid,
                    networkState = networkState
                )
            }.collect { proxyApplicationState ->
                if (proxyApplicationState.desiredProxy.isEnabled) {
                    applyProxyForCurrentNetwork(proxyApplicationState)
                } else {
                    _proxyNetworkStatus.value = ProxyNetworkStatus()
                }
            }
        }
    }

    override suspend fun enableProxy(proxy: Proxy) {
        appDataRepository.saveDesiredProxy(proxy)
        appDataRepository.saveProxy(proxy)
        applyProxyForCurrentNetwork(
            ProxyApplicationState(
                desiredProxy = proxy,
                proxyScope = userPreferencesRepository.proxyScope.first(),
                proxyNetworkSsid = userPreferencesRepository.proxyNetworkSsid.first(),
                networkState = networkStateProvider.networkState.value
            )
        )
    }

    override fun disableProxy() {
        coroutineScope.launch {
            appDataRepository.clearDesiredProxy()
        }
        _proxyNetworkStatus.value = ProxyNetworkStatus()
        writeProxy(Proxy.Disabled)
        updateProxyData()
    }

    override fun refreshProxy() {
        networkStateProvider.refresh()
        coroutineScope.launch {
            applyProxyForCurrentNetwork(
                ProxyApplicationState(
                    desiredProxy = appDataRepository.desiredProxy.first(),
                    proxyScope = userPreferencesRepository.proxyScope.first(),
                    proxyNetworkSsid = userPreferencesRepository.proxyNetworkSsid.first(),
                    networkState = networkStateProvider.networkState.value
                )
            )
        }
    }

    private fun applyProxyForCurrentNetwork(proxyApplicationState: ProxyApplicationState) {
        val desiredProxy = proxyApplicationState.desiredProxy
        val shouldApplyProxy = desiredProxy.isEnabled &&
            proxyApplicationState.matchesCurrentNetwork()
        val proxyToWrite = if (shouldApplyProxy) desiredProxy else Proxy.Disabled

        _proxyNetworkStatus.value = ProxyNetworkStatus(
            hasDesiredProxy = desiredProxy.isEnabled,
            isNetworkMatched = desiredProxy.isEnabled.not() || shouldApplyProxy,
            requiresLocationPermission = desiredProxy.isEnabled &&
                proxyApplicationState.proxyScope == ProxyScope.SPECIFIC_SSID &&
                networkStateProvider.hasLocationPermission().not(),
            currentSsid = proxyApplicationState.networkState.ssid
        )
        writeProxy(proxyToWrite)
        updateProxyData()
    }

    private fun ProxyApplicationState.matchesCurrentNetwork(): Boolean {
        return when (proxyScope) {
            ProxyScope.ALL_NETWORKS -> true
            ProxyScope.WIFI_ONLY -> networkState.isWifi
            ProxyScope.SPECIFIC_SSID ->
                networkState.isWifi &&
                    proxyNetworkSsid.isNotBlank() &&
                    networkState.ssid == proxyNetworkSsid
        }
    }

    private fun writeProxy(proxy: Proxy) {
        Settings.Global.putString(
            contentResolver,
            Settings.Global.HTTP_PROXY,
            proxy.toString()
        )
    }

    private fun updateProxyData() {
        val proxySetting = Settings.Global.getString(contentResolver, Settings.Global.HTTP_PROXY)
        _proxySetting.value = proxyMapper.from(proxySetting)
        proxyUpdateNotifier.notifyProxyChanged()
    }

    private data class ProxyApplicationState(
        val desiredProxy: Proxy,
        val proxyScope: ProxyScope,
        val proxyNetworkSsid: String,
        val networkState: NetworkState
    )
}
