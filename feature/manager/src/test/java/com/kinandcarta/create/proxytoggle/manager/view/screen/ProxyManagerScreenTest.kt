package com.kinandcarta.create.proxytoggle.manager.view.screen

import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import com.kinandcarta.create.proxytoggle.manager.viewmodel.ProxyManagerViewModel
import com.kinandcarta.create.proxytoggle.repository.userprefs.ProxyScope
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ProxyManagerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @MockK
    private lateinit var mockViewModel: ProxyManagerViewModel

    private var testState = mutableStateOf<ProxyManagerViewModel.UiState>(
        ProxyManagerViewModel.UiState.Disconnected(
            addressState = ProxyManagerViewModel.TextFieldState(""),
            portState = ProxyManagerViewModel.TextFieldState(""),
            pastProxies = emptyList()
        )
    )
    private var testNetworkScopeState = mutableStateOf(
        ProxyManagerViewModel.NetworkScopeUiState(ssid = "58group")
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockViewModel.uiState } answers { testState }
        every { mockViewModel.networkScopeState } answers { testNetworkScopeState }
        every { mockViewModel.onUserInteraction(any()) } just Runs
        every { mockViewModel.onForceFocusExecuted() } just Runs
    }

    @Test
    fun `switch theme - WHEN button clicked, THEN viewModel's SwitchThemeClicked called`() {
        launchComposable()
        composeRule.onNodeWithContentDescription("Switch theme")
            .performClick()

        verify {
            mockViewModel.onUserInteraction(
                ProxyManagerViewModel.UserInteraction.SwitchThemeClicked
            )
        }
    }

    @Test
    fun `show information - WHEN button clicked, THEN viewModel's SwitchThemeClicked called`() {
        launchComposable()

        composeRule.onNodeWithContentDescription("Information")
            .performClick()

        val infoSubstring = "Uninstalling the app with the proxy enabled"

        composeRule.onNodeWithText(infoSubstring, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `toggle button - WHEN button clicked, THEN viewModel's ToggleProxyClicked called`() {
        launchComposable()

        composeRule.onNodeWithTag(TestTags.PROXY_TOGGLE_BUTTON)
            .performScrollTo()
            .performClick()

        verify {
            mockViewModel.onUserInteraction(
                ProxyManagerViewModel.UserInteraction.ToggleProxyClicked
            )
        }
    }

    @Test
    fun `address text - WHEN changed, THEN viewModel's AddressChanged called w new text`() {
        launchComposable(address = "192")

        composeRule.onNodeWithText("192")
            .performScrollTo()
            .performTextInput(".")

        verify {
            mockViewModel.onUserInteraction(
                ProxyManagerViewModel.UserInteraction.AddressChanged("192.")
            )
        }
    }

    @Test
    fun `port text - WHEN changed, THEN viewModel's PortChanged called w new text`() {
        launchComposable(port = "808")

        composeRule.onNodeWithText("808")
            .performScrollTo()
            .performTextInput("0")

        verify {
            mockViewModel.onUserInteraction(
                ProxyManagerViewModel.UserInteraction.PortChanged("8080")
            )
        }
    }

    @Test
    fun `address text - WHEN forceFocus, THEN address focused and viewModel's onForceFocusExecuted called`() {
        launchComposable(address = "192")

        givenUiState(address = "192", forceFocusAddress = true)
        composeRule.onNodeWithText("192")
            .assertIsFocused()

        verify { mockViewModel.onForceFocusExecuted() }
    }

    @Test
    fun `port text - WHEN forceFocus, THEN port focused and viewModel's onForceFocusExecuted called`() {
        launchComposable(port = "808")

        givenUiState(port = "808", forceFocusPort = true)
        composeRule.onNodeWithText("808")
            .assertIsFocused()

        verify { mockViewModel.onForceFocusExecuted() }
    }

    @Test
    fun `select recent IP from dropDown - WHEN address selected THEN viewModel gets RecentAddressSelected`() {
        val pastProxies = (1..3).map {
            Proxy(it.toString(), it.toString())
        }
        val duplicateAddress = Proxy("1", "99")
        val proxyToSelect = pastProxies[1]

        launchComposable(pastProxies = pastProxies + duplicateAddress)

        composeRule.onNodeWithTag(TestTags.RECENT_IPS_DROPDOWN_BUTTON)
            .performScrollTo()
            .performClick()

        pastProxies.map { it.address }.forEach {
            composeRule.onNodeWithText(it)
                .assertIsDisplayed()
        }
        composeRule.onAllNodesWithText(duplicateAddress.address)
            .assertCountEquals(1)

        composeRule.onNodeWithText(proxyToSelect.address)
            .performClick()

        pastProxies.map { it.address }.forEach {
            composeRule.onAllNodesWithText(it)
                .assertCountEquals(0)
        }

        verify {
            mockViewModel.onUserInteraction(
                ProxyManagerViewModel.UserInteraction.RecentAddressSelected(proxyToSelect.address)
            )
        }
    }

    @Test
    fun `specific ssid - WHEN selected THEN viewModel gets ProxyScopeSelected`() {
        launchComposable()

        composeRule.onNodeWithText("Specific SSID")
            .performScrollTo()
            .performClick()

        verify {
            mockViewModel.onUserInteraction(
                ProxyManagerViewModel.UserInteraction.ProxyScopeSelected(ProxyScope.SPECIFIC_SSID)
            )
        }
    }

    @Test
    fun `specific ssid text - WHEN changed THEN viewModel gets ProxyNetworkSsidChanged`() {
        launchComposable(proxyScope = ProxyScope.SPECIFIC_SSID, ssid = "58group")

        composeRule.onNodeWithText("58group")
            .performScrollTo()
            .performTextInput("-office")

        verify {
            mockViewModel.onUserInteraction(
                ProxyManagerViewModel.UserInteraction.ProxyNetworkSsidChanged("58group-office")
            )
        }
    }

    @Suppress("LongParameterList")
    private fun launchComposable(
        proxyEnabled: Boolean = false,
        address: String = "",
        port: String = "",
        forceFocusAddress: Boolean = false,
        forceFocusPort: Boolean = false,
        pastProxies: List<Proxy> = emptyList(),
        proxyScope: ProxyScope = ProxyScope.ALL_NETWORKS,
        ssid: String = "58group"
    ) {
        givenUiState(
            proxyEnabled = proxyEnabled,
            address = address,
            port = port,
            forceFocusAddress = forceFocusAddress,
            forceFocusPort = forceFocusPort,
            pastProxies = pastProxies,
            proxyScope = proxyScope,
            ssid = ssid
        )

        composeRule.setContent {
            ProxyManagerScreen(
                viewModel = mockViewModel,
                useVerticalLayout = true
            )
        }
    }

    @Suppress("LongParameterList")
    private fun givenUiState(
        proxyEnabled: Boolean = false,
        address: String = "",
        port: String = "",
        forceFocusAddress: Boolean = false,
        forceFocusPort: Boolean = false,
        pastProxies: List<Proxy> = emptyList(),
        proxyScope: ProxyScope = ProxyScope.ALL_NETWORKS,
        ssid: String = "58group"
    ) {
        val addressState = ProxyManagerViewModel.TextFieldState(
            text = address,
            forceFocus = forceFocusAddress
        )
        val portState = ProxyManagerViewModel.TextFieldState(
            text = port,
            forceFocus = forceFocusPort
        )

        testState.value = if (proxyEnabled) {
            ProxyManagerViewModel.UiState.Connected(addressState, portState)
        } else {
            ProxyManagerViewModel.UiState.Disconnected(
                addressState = addressState,
                portState = portState,
                pastProxies = pastProxies
            )
        }
        testNetworkScopeState.value = ProxyManagerViewModel.NetworkScopeUiState(
            proxyScope = proxyScope,
            ssid = ssid
        )
    }
}
