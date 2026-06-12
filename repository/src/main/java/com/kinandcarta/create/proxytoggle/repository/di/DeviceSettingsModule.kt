package com.kinandcarta.create.proxytoggle.repository.di

import com.kinandcarta.create.proxytoggle.repository.devicesettings.DeviceSettingsManager
import com.kinandcarta.create.proxytoggle.repository.devicesettings.DeviceSettingsManagerImpl
import com.kinandcarta.create.proxytoggle.repository.network.NetworkStateProvider
import com.kinandcarta.create.proxytoggle.repository.network.NetworkStateProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DeviceSettingsModule {

    @Binds
    fun bindDeviceSettingsManager(
        deviceSettingsManager: DeviceSettingsManagerImpl
    ): DeviceSettingsManager

    @Binds
    fun bindNetworkStateProvider(
        networkStateProvider: NetworkStateProviderImpl
    ): NetworkStateProvider
}
