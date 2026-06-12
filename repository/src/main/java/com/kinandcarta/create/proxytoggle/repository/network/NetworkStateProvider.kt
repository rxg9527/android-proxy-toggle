package com.kinandcarta.create.proxytoggle.repository.network

import kotlinx.coroutines.flow.StateFlow

interface NetworkStateProvider {
    val networkState: StateFlow<NetworkState>
    fun hasLocationPermission(): Boolean
    fun refresh()
}
