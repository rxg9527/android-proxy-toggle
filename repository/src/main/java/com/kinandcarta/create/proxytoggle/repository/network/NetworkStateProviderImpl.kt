package com.kinandcarta.create.proxytoggle.repository.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkStateProvider {

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val _networkState = MutableStateFlow(readNetworkState())
    override val networkState = _networkState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refresh()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            refresh()
        }

        override fun onLost(network: Network) {
            refresh()
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun hasLocationPermission(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            locationPermission == PackageManager.PERMISSION_GRANTED
    }

    override fun refresh() {
        _networkState.value = readNetworkState()
    }

    private fun readNetworkState(): NetworkState {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        return NetworkState(
            isWifi = isWifi,
            ssid = if (isWifi) wifiManager.connectionInfo.ssid.cleanSsid() else null
        )
    }

    private fun String?.cleanSsid(): String? {
        return this
            ?.takeUnless { it == UNKNOWN_SSID }
            ?.removeSurrounding("\"")
    }

    companion object {
        private const val UNKNOWN_SSID = "<unknown ssid>"
    }
}
