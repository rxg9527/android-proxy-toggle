@file:Suppress("LongParameterList", "TooManyFunctions")

package com.kinandcarta.create.proxytoggle.manager.view.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import com.kinandcarta.create.proxytoggle.core.ui.theme.BluishGrey
import com.kinandcarta.create.proxytoggle.core.ui.theme.ProxyToggleTheme
import com.kinandcarta.create.proxytoggle.core.ui.theme.StatusLabelTextStyle
import com.kinandcarta.create.proxytoggle.manager.R
import com.kinandcarta.create.proxytoggle.manager.annotation.DarkAndLightPreview
import com.kinandcarta.create.proxytoggle.manager.annotation.LandscapeDarkAndLightPreview
import com.kinandcarta.create.proxytoggle.manager.view.composable.ProxyToggleAlertDialog
import com.kinandcarta.create.proxytoggle.manager.view.composable.ProxyToggleButton
import com.kinandcarta.create.proxytoggle.manager.view.composable.ProxyToggleIcon
import com.kinandcarta.create.proxytoggle.manager.view.composable.ProxyToggleTextField
import com.kinandcarta.create.proxytoggle.manager.viewmodel.ProxyManagerViewModel
import com.kinandcarta.create.proxytoggle.manager.viewmodel.ProxyManagerViewModel.NetworkScopeUiState
import com.kinandcarta.create.proxytoggle.manager.viewmodel.ProxyManagerViewModel.UiState
import com.kinandcarta.create.proxytoggle.manager.viewmodel.ProxyManagerViewModel.UserInteraction
import com.kinandcarta.create.proxytoggle.repository.userprefs.ProxyScope

@Composable
fun ProxyManagerScreen(
    viewModel: ProxyManagerViewModel = viewModel(),
    useVerticalLayout: Boolean
) {
    val uiState by viewModel.uiState
    val networkScopeState by viewModel.networkScopeState
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onUserInteraction(UserInteraction.LocationPermissionResult)
    }

    ProxyManagerScreenContent(
        useVerticalLayout = useVerticalLayout,
        uiState = uiState,
        networkScopeState = networkScopeState,
        onUserInteraction = viewModel::onUserInteraction,
        onForceFocusExecuted = viewModel::onForceFocusExecuted,
        onRequestLocationPermission = {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (permissionGranted.not()) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    )
}

@Composable
fun ProxyManagerScreenContent(
    useVerticalLayout: Boolean,
    uiState: UiState,
    networkScopeState: NetworkScopeUiState,
    onUserInteraction: (UserInteraction) -> Unit,
    onForceFocusExecuted: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    @OptIn(ExperimentalComposeUiApi::class)
    if (uiState is UiState.Connected) {
        LocalSoftwareKeyboardController.current?.hide()
    }

    var showInfoDialog by rememberSaveable { mutableStateOf(false) }

    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TopIcons(
                onSwitchTheme = { onUserInteraction(UserInteraction.SwitchThemeClicked) },
                onInfoClicked = { showInfoDialog = showInfoDialog.not() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            MainLayout(
                useVerticalLayout = useVerticalLayout,
                buttonAndLabel = {
                    ButtonAndLabel(
                        proxyEnabled = uiState is UiState.Connected,
                        onToggleProxy = { onUserInteraction(UserInteraction.ToggleProxyClicked) }
                    )
                },
                textFields = {
                    TextFields(
                        uiState,
                        networkScopeState,
                        onUserInteraction,
                        onForceFocusExecuted,
                        onRequestLocationPermission
                    )
                }
            )
            if (showInfoDialog) {
                ProxyToggleAlertDialog(
                    message = stringResource(R.string.dialog_message_information),
                    onCloseDialog = { showInfoDialog = false }
                )
            }
        }
    }
}

@Composable
fun TopIcons(
    onSwitchTheme: () -> Unit,
    onInfoClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        ProxyToggleIcon(
            onClick = onSwitchTheme,
            icon = R.drawable.ic_switch_theme,
            contentDescription = R.string.switch_theme
        )
        ProxyToggleIcon(
            onClick = onInfoClicked,
            icon = R.drawable.ic_info,
            contentDescription = R.string.information
        )
    }
}

@Composable
fun MainLayout(
    useVerticalLayout: Boolean,
    buttonAndLabel: @Composable () -> Unit,
    textFields: @Composable () -> Unit
) {
    if (useVerticalLayout) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.xlarge_margin))
                .verticalScroll(rememberScrollState())
        ) {
            buttonAndLabel()
            Spacer(Modifier.height(dimensionResource(R.dimen.default_margin)))
            textFields()
        }
    } else {
        Row(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.xlarge_margin))
        ) {
            Column {
                Spacer(Modifier.height(dimensionResource(R.dimen.default_margin)))
                textFields()
            }
            Spacer(Modifier.width(dimensionResource(R.dimen.large_margin)))
            buttonAndLabel()
        }
    }
}

@Composable
fun ButtonAndLabel(
    proxyEnabled: Boolean,
    onToggleProxy: () -> Unit
) {
    ConstraintLayout {
        val (button, label) = createRefs()
        ProxyToggleButton(
            proxyEnabled = proxyEnabled,
            onClick = onToggleProxy,
            modifier = Modifier
                .constrainAs(button) {
                    top.linkTo(parent.top)
                    absoluteLeft.linkTo(parent.absoluteLeft)
                    absoluteRight.linkTo(parent.absoluteRight)
                }
                .testTag(TestTags.PROXY_TOGGLE_BUTTON)
        )
        Text(
            text = stringResource(
                if (proxyEnabled) R.string.connected else R.string.disconnected
            ).uppercase(),
            color = if (proxyEnabled) MaterialTheme.colorScheme.primary else BluishGrey,
            modifier = Modifier
                .padding(vertical = dimensionResource(R.dimen.default_margin))
                .constrainAs(label) {
                    top.linkTo(button.bottom)
                    absoluteLeft.linkTo(parent.absoluteLeft)
                    absoluteRight.linkTo(parent.absoluteRight)
                }
                .clearAndSetSemantics {},
            style = StatusLabelTextStyle
        )
    }
}

@Composable
private fun RecentAddressesDropdown(
    recentAddresses: List<String>,
    onAddressSelected: (String) -> Unit
) {
    var showDropDown by rememberSaveable { mutableStateOf(false) }
    IconButton(
        onClick = { showDropDown = showDropDown.not() },
        modifier = Modifier.testTag(TestTags.RECENT_IPS_DROPDOWN_BUTTON)
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = stringResource(R.string.recent_ip_addresses_dropdown),
            tint = BluishGrey
        )
    }
    DropdownMenu(
        expanded = showDropDown,
        onDismissRequest = { showDropDown = false }
    ) {
        for (address in recentAddresses) {
            DropdownMenuItem(
                text = { Text(text = address) },
                onClick = {
                    onAddressSelected(address)
                    showDropDown = false
                }
            )
        }
    }
}

@Composable
private fun TextFields(
    uiState: UiState,
    networkScopeState: NetworkScopeUiState,
    onUserInteraction: (UserInteraction) -> Unit,
    onForceFocusExecuted: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val recentAddresses = (uiState as? UiState.Disconnected)
        ?.pastProxies
        ?.map { it.address }
        ?.distinct()
        .orEmpty()

    ProxyToggleTextField(
        label = stringResource(R.string.hint_ip_address),
        state = uiState.addressState,
        onTextChanged = { onUserInteraction(UserInteraction.AddressChanged(it)) },
        enabled = uiState is UiState.Disconnected,
        keyboardOptions = getNumKeyboardOptions(ImeAction.Next),
        onForceFocusExecuted = onForceFocusExecuted,
        trailingContent = if (uiState is UiState.Disconnected && recentAddresses.isNotEmpty()) {
            {
                RecentAddressesDropdown(
                    recentAddresses = recentAddresses,
                    onAddressSelected = {
                        onUserInteraction(UserInteraction.RecentAddressSelected(it))
                    }
                )
            }
        } else {
            null
        }
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.default_margin)))
    ProxyToggleTextField(
        label = stringResource(R.string.hint_port),
        state = uiState.portState,
        onTextChanged = { onUserInteraction(UserInteraction.PortChanged(it)) },
        enabled = uiState is UiState.Disconnected,
        keyboardOptions = getNumKeyboardOptions(ImeAction.Done),
        onForceFocusExecuted = onForceFocusExecuted,
        trailingContent = null
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.default_margin)))
    NetworkScopeFields(
        networkScopeState = networkScopeState,
        controlsEnabled = uiState is UiState.Disconnected,
        onUserInteraction = onUserInteraction,
        onRequestLocationPermission = onRequestLocationPermission
    )
}

@Composable
private fun NetworkScopeFields(
    networkScopeState: NetworkScopeUiState,
    controlsEnabled: Boolean,
    onUserInteraction: (UserInteraction) -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    Text(
        text = stringResource(R.string.proxy_scope_title),
        color = BluishGrey,
        style = MaterialTheme.typography.bodyMedium
    )
    ProxyScopeOption(
        label = stringResource(R.string.proxy_scope_all_networks),
        selected = networkScopeState.proxyScope == ProxyScope.ALL_NETWORKS,
        enabled = controlsEnabled,
        onClick = { onUserInteraction(UserInteraction.ProxyScopeSelected(ProxyScope.ALL_NETWORKS)) }
    )
    ProxyScopeOption(
        label = stringResource(R.string.proxy_scope_wifi_only),
        selected = networkScopeState.proxyScope == ProxyScope.WIFI_ONLY,
        enabled = controlsEnabled,
        onClick = { onUserInteraction(UserInteraction.ProxyScopeSelected(ProxyScope.WIFI_ONLY)) }
    )
    ProxyScopeOption(
        label = stringResource(R.string.proxy_scope_specific_ssid),
        selected = networkScopeState.proxyScope == ProxyScope.SPECIFIC_SSID,
        enabled = controlsEnabled,
        onClick = {
            onUserInteraction(UserInteraction.ProxyScopeSelected(ProxyScope.SPECIFIC_SSID))
            onRequestLocationPermission()
        }
    )
    if (networkScopeState.proxyScope == ProxyScope.SPECIFIC_SSID) {
        Spacer(Modifier.height(dimensionResource(R.dimen.default_margin)))
        ProxyToggleTextField(
            label = stringResource(R.string.proxy_scope_ssid_hint),
            state = ProxyManagerViewModel.TextFieldState(text = networkScopeState.ssid),
            onTextChanged = {
                onUserInteraction(UserInteraction.ProxyNetworkSsidChanged(it))
            },
            enabled = controlsEnabled,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            onForceFocusExecuted = {},
            trailingContent = null,
            modifier = Modifier.testTag(TestTags.PROXY_SCOPE_SPECIFIC_SSID_FIELD)
        )
    }
    networkScopeState.statusMessage?.let {
        Spacer(Modifier.height(dimensionResource(R.dimen.textfield_error_padding)))
        Text(
            text = stringResource(it),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ProxyScopeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = onClick
        )
        Text(
            text = label,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else BluishGrey,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun getNumKeyboardOptions(imeAction: ImeAction): KeyboardOptions {
    return KeyboardOptions.Default.copy(
        autoCorrect = false,
        keyboardType = KeyboardType.Number,
        imeAction = imeAction
    )
}

private val PREVIEW_PAST_PROXIES = listOf(
    Proxy("192.168.1.1", "8080"),
    Proxy("192.168.1.1", "8081"),
    Proxy("10.0.1.1", "8080")
)

@DarkAndLightPreview
@Composable
fun ProxyManagerScreenConnectedPreview() {
    ProxyManagerScreenForPreview(proxyEnabled = true)
}

@DarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedNoProxiesPreview() {
    ProxyManagerScreenForPreview()
}

@DarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedWithPastProxiesPreview() {
    ProxyManagerScreenForPreview(pastProxies = PREVIEW_PAST_PROXIES)
}

@DarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedWithAddressErrorPreview() {
    ProxyManagerScreenForPreview(
        pastProxies = PREVIEW_PAST_PROXIES,
        addressErrorRes = R.string.error_invalid_address
    )
}

@DarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedWithPortErrorPreview() {
    ProxyManagerScreenForPreview(
        pastProxies = PREVIEW_PAST_PROXIES,
        portErrorRes = R.string.error_invalid_port
    )
}

@LandscapeDarkAndLightPreview
@Composable
fun ProxyManagerScreenConnectedPreviewLandscape() {
    ProxyManagerScreenForPreview(
        useVerticalLayout = false,
        proxyEnabled = true
    )
}

@LandscapeDarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedNoProxiesPreviewLandscape() {
    ProxyManagerScreenForPreview(useVerticalLayout = false)
}

@LandscapeDarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedWithPastProxiesPreviewLandscape() {
    ProxyManagerScreenForPreview(
        useVerticalLayout = false,
        pastProxies = PREVIEW_PAST_PROXIES
    )
}

@LandscapeDarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedWithAddressErrorPreviewLandscape() {
    ProxyManagerScreenForPreview(
        useVerticalLayout = false,
        pastProxies = PREVIEW_PAST_PROXIES,
        addressErrorRes = R.string.error_invalid_address
    )
}

@LandscapeDarkAndLightPreview
@Composable
fun ProxyManagerScreenDisconnectedWithPortErrorPreviewLandscape() {
    ProxyManagerScreenForPreview(
        useVerticalLayout = false,
        pastProxies = PREVIEW_PAST_PROXIES,
        portErrorRes = R.string.error_invalid_port
    )
}

@Composable
private fun ProxyManagerScreenForPreview(
    useVerticalLayout: Boolean = true,
    proxyEnabled: Boolean = false,
    pastProxies: List<Proxy> = emptyList(),
    @StringRes addressErrorRes: Int? = null,
    @StringRes portErrorRes: Int? = null
) {
    val proxyForTextFields = when {
        proxyEnabled -> PREVIEW_PAST_PROXIES[0]
        pastProxies.isNotEmpty() -> pastProxies.first()
        else -> Proxy("", "")
    }

    val (addressText, portText) = proxyForTextFields.let { Pair(it.address, it.port) }

    ProxyToggleTheme {
        ProxyManagerScreenContent(
            useVerticalLayout = useVerticalLayout,
            uiState = if (proxyEnabled) {
                UiState.Connected(
                    addressState = ProxyManagerViewModel.TextFieldState(text = addressText),
                    portState = ProxyManagerViewModel.TextFieldState(text = portText)
                )
            } else {
                UiState.Disconnected(
                    addressState = ProxyManagerViewModel.TextFieldState(
                        text = addressText,
                        error = addressErrorRes
                    ),
                    portState = ProxyManagerViewModel.TextFieldState(
                        text = portText,
                        error = portErrorRes
                    ),
                    pastProxies = pastProxies
                )
            },
            networkScopeState = NetworkScopeUiState(ssid = "58group"),
            onUserInteraction = {},
            onForceFocusExecuted = {},
            onRequestLocationPermission = {}
        )
    }
}
