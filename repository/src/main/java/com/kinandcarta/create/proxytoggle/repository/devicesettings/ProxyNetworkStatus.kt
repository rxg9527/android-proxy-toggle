package com.kinandcarta.create.proxytoggle.repository.devicesettings

data class ProxyNetworkStatus(
    val hasDesiredProxy: Boolean = false,
    val isNetworkMatched: Boolean = true,
    val requiresLocationPermission: Boolean = false,
    val currentSsid: String? = null
)
