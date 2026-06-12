package com.kinandcarta.create.proxytoggle.repository.userprefs

import androidx.datastore.core.DataStore
import com.kinandcarta.create.proxytoggle.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<UserPreferences>
) : UserPreferencesRepository {

    override val isNightMode: Flow<Boolean> by lazy {
        userPreferences.map {
            it.themeMode == UserPreferences.ThemeMode.DARK
        }.distinctUntilChanged()
    }

    override val proxyScope: Flow<ProxyScope> by lazy {
        userPreferences.map {
            it.proxyNetworkScope.toProxyScope()
        }.distinctUntilChanged()
    }

    override val proxyNetworkSsid: Flow<String> by lazy {
        userPreferences.map {
            if (it.proxyNetworkSsidSet) {
                it.proxyNetworkSsid
            } else {
                DEFAULT_PROXY_NETWORK_SSID
            }
        }.distinctUntilChanged()
    }

    override suspend fun toggleTheme() {
        dataStore.updateData { userPreferences ->
            val newThemeMode = if (userPreferences.themeMode == UserPreferences.ThemeMode.DARK) {
                UserPreferences.ThemeMode.LIGHT
            } else {
                UserPreferences.ThemeMode.DARK
            }
            userPreferences.toBuilder().setThemeMode(newThemeMode).build()
        }
    }

    override suspend fun setProxyScope(proxyScope: ProxyScope) {
        dataStore.updateData { userPreferences ->
            userPreferences.toBuilder()
                .setProxyNetworkScope(proxyScope.toProtoProxyScope())
                .build()
        }
    }

    override suspend fun setProxyNetworkSsid(ssid: String) {
        dataStore.updateData { userPreferences ->
            userPreferences.toBuilder()
                .setProxyNetworkSsid(ssid)
                .setProxyNetworkSsidSet(true)
                .build()
        }
    }

    private val userPreferences: Flow<UserPreferences> by lazy {
        dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(UserPreferences.getDefaultInstance())
            } else {
                throw exception
            }
        }
    }

    private fun UserPreferences.ProxyNetworkScope.toProxyScope(): ProxyScope {
        return when (this) {
            UserPreferences.ProxyNetworkScope.WIFI_ONLY -> ProxyScope.WIFI_ONLY
            UserPreferences.ProxyNetworkScope.SPECIFIC_SSID -> ProxyScope.SPECIFIC_SSID
            else -> ProxyScope.ALL_NETWORKS
        }
    }

    private fun ProxyScope.toProtoProxyScope(): UserPreferences.ProxyNetworkScope {
        return when (this) {
            ProxyScope.ALL_NETWORKS -> UserPreferences.ProxyNetworkScope.ALL_NETWORKS
            ProxyScope.WIFI_ONLY -> UserPreferences.ProxyNetworkScope.WIFI_ONLY
            ProxyScope.SPECIFIC_SSID -> UserPreferences.ProxyNetworkScope.SPECIFIC_SSID
        }
    }

    companion object {
        const val DEFAULT_PROXY_NETWORK_SSID = "58group"
    }
}
