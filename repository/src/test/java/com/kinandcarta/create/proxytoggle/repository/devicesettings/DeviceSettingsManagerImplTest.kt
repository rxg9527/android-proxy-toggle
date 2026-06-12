package com.kinandcarta.create.proxytoggle.repository.devicesettings

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import com.kinandcarta.create.proxytoggle.core.common.proxyupdate.ProxyUpdateNotifier
import com.kinandcarta.create.proxytoggle.core.common.stub.Stubs.PROXY
import com.kinandcarta.create.proxytoggle.core.common.stub.Stubs.PROXY_DISABLED
import com.kinandcarta.create.proxytoggle.core.common.stub.Stubs.VALID_PROXY
import com.kinandcarta.create.proxytoggle.repository.appdata.AppDataRepository
import com.kinandcarta.create.proxytoggle.repository.network.NetworkState
import com.kinandcarta.create.proxytoggle.repository.network.NetworkStateProvider
import com.kinandcarta.create.proxytoggle.repository.proxymapper.ProxyMapper
import com.kinandcarta.create.proxytoggle.repository.userprefs.ProxyScope
import com.kinandcarta.create.proxytoggle.repository.userprefs.UserPreferencesRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DeviceSettingsManagerImplTest {

    @MockK
    private lateinit var mockProxyMapper: ProxyMapper

    @RelaxedMockK
    private lateinit var mockProxyUpdateNotifier: ProxyUpdateNotifier

    @RelaxedMockK
    private lateinit var mockAppDataRepository: AppDataRepository

    @RelaxedMockK
    private lateinit var mockUserPreferencesRepository: UserPreferencesRepository

    @MockK
    private lateinit var mockNetworkStateProvider: NetworkStateProvider

    private val desiredProxyFlow = MutableStateFlow(Proxy.Disabled)
    private val proxyScopeFlow = MutableStateFlow(ProxyScope.ALL_NETWORKS)
    private val proxyNetworkSsidFlow = MutableStateFlow("58group")
    private val networkStateFlow = MutableStateFlow(NetworkState())

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var subject: DeviceSettingsManagerImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockProxyMapper.from(VALID_PROXY) } returns PROXY
        every { mockProxyMapper.from(PROXY_DISABLED) } returns Proxy.Disabled
        every { mockProxyMapper.from(null) } returns Proxy.Disabled
        every { mockAppDataRepository.desiredProxy } returns desiredProxyFlow
        every { mockUserPreferencesRepository.proxyScope } returns proxyScopeFlow
        every { mockUserPreferencesRepository.proxyNetworkSsid } returns proxyNetworkSsidFlow
        every { mockNetworkStateProvider.networkState } returns networkStateFlow
        every { mockNetworkStateProvider.hasLocationPermission() } returns true
        every { mockNetworkStateProvider.refresh() } just Runs
        coEvery { mockAppDataRepository.saveDesiredProxy(any()) } coAnswers {
            desiredProxyFlow.value = firstArg()
        }
        coEvery { mockAppDataRepository.clearDesiredProxy() } coAnswers {
            desiredProxyFlow.value = Proxy.Disabled
        }
        excludeRecords {
            mockAppDataRepository.desiredProxy
            mockUserPreferencesRepository.proxyScope
            mockUserPreferencesRepository.proxyNetworkSsid
            mockNetworkStateProvider.networkState
            mockNetworkStateProvider.hasLocationPermission()
        }

        subject = DeviceSettingsManagerImpl(
            context,
            mockProxyMapper,
            mockProxyUpdateNotifier,
            mockAppDataRepository,
            mockUserPreferencesRepository,
            mockNetworkStateProvider
        )
    }

    @After
    fun tearDown() {
        confirmVerified(mockProxyUpdateNotifier, mockAppDataRepository)
    }

    @Test
    fun `initial state is disabled`() {
        assertThat(subject.proxySetting.value).isEqualTo(Proxy.Disabled)
        verify { mockProxyUpdateNotifier.notifyProxyChanged() }
    }

    @Test
    fun `enableProxy() - applies given proxy and proxySetting is updated and proxy is stored`() {
        runTest {
            subject.enableProxy(PROXY)

            assertThat(subject.proxySetting.value).isEqualTo(PROXY)
            coVerify {
                mockProxyUpdateNotifier.notifyProxyChanged()
                mockAppDataRepository.saveDesiredProxy(PROXY)
                mockAppDataRepository.saveProxy(PROXY)
            }
        }
    }

    @Test
    fun `disableProxy() - final state is disabled`() {
        subject.disableProxy()

        assertThat(subject.proxySetting.value).isEqualTo(Proxy.Disabled)
        coVerify(timeout = 500) {
            mockAppDataRepository.clearDesiredProxy()
        }
        verify {
            mockProxyUpdateNotifier.notifyProxyChanged()
        }
    }

    @Test
    fun `enableProxy() - GIVEN wifi only and not wifi THEN stores desired proxy but disables global proxy`() {
        runTest {
            proxyScopeFlow.value = ProxyScope.WIFI_ONLY
            networkStateFlow.value = NetworkState(isWifi = false)

            subject.enableProxy(PROXY)

            assertThat(subject.proxySetting.value).isEqualTo(Proxy.Disabled)
            assertThat(subject.proxyNetworkStatus.value.hasDesiredProxy).isTrue()
            assertThat(subject.proxyNetworkStatus.value.isNetworkMatched).isFalse()
            coVerify {
                mockProxyUpdateNotifier.notifyProxyChanged()
                mockAppDataRepository.saveDesiredProxy(PROXY)
                mockAppDataRepository.saveProxy(PROXY)
            }
        }
    }

    @Test
    fun `enableProxy() - GIVEN specific ssid and current ssid matches THEN global proxy is enabled`() {
        runTest {
            proxyScopeFlow.value = ProxyScope.SPECIFIC_SSID
            proxyNetworkSsidFlow.value = "58group"
            networkStateFlow.value = NetworkState(isWifi = true, ssid = "58group")

            subject.enableProxy(PROXY)

            assertThat(subject.proxySetting.value).isEqualTo(PROXY)
            assertThat(subject.proxyNetworkStatus.value.isNetworkMatched).isTrue()
            coVerify {
                mockProxyUpdateNotifier.notifyProxyChanged()
                mockAppDataRepository.saveDesiredProxy(PROXY)
                mockAppDataRepository.saveProxy(PROXY)
            }
        }
    }

    @Test
    fun `enableProxy() - GIVEN specific ssid and current ssid does not match THEN global proxy is disabled`() {
        runTest {
            proxyScopeFlow.value = ProxyScope.SPECIFIC_SSID
            proxyNetworkSsidFlow.value = "58group"
            networkStateFlow.value = NetworkState(isWifi = true, ssid = "other")

            subject.enableProxy(PROXY)

            assertThat(subject.proxySetting.value).isEqualTo(Proxy.Disabled)
            assertThat(subject.proxyNetworkStatus.value.isNetworkMatched).isFalse()
            coVerify {
                mockProxyUpdateNotifier.notifyProxyChanged()
                mockAppDataRepository.saveDesiredProxy(PROXY)
                mockAppDataRepository.saveProxy(PROXY)
            }
        }
    }
}
