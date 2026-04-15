package com.kinandcarta.create.proxytoggle.notification

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinandcarta.create.proxytoggle.core.common.proxy.Proxy
import com.kinandcarta.create.proxytoggle.core.common.stub.Stubs.PROXY
import com.kinandcarta.create.proxytoggle.core.common.stub.Stubs.VALID_PROXY
import com.kinandcarta.create.proxytoggle.repository.proxymapper.ProxyMapper
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ProxyNotificationUpdateListenerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @MockK
    private lateinit var mockProxyMapper: ProxyMapper

    @MockK(relaxed = true)
    private lateinit var mockNotificationManager: ProxyNotificationManager

    private lateinit var subject: ProxyNotificationUpdateListener

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        subject = ProxyNotificationUpdateListener(
            context = context,
            proxyMapper = mockProxyMapper,
            proxyNotificationManager = mockNotificationManager
        )
    }

    @After
    fun tearDown() {
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, null)
    }

    @Test
    fun `onProxyUpdate() - WHEN proxy enabled THEN show persistent notification`() {
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, VALID_PROXY)
        io.mockk.every { mockProxyMapper.from(VALID_PROXY) } returns PROXY
        io.mockk.every { mockNotificationManager.showPersistentProxyNotification(PROXY) } just runs

        subject.onProxyUpdate()

        verify { mockNotificationManager.showPersistentProxyNotification(PROXY) }
    }

    @Test
    fun `onProxyUpdate() - WHEN proxy disabled THEN dismiss persistent notification`() {
        io.mockk.every { mockProxyMapper.from(null) } returns Proxy.Disabled
        io.mockk.every { mockNotificationManager.dismissPersistentProxyNotification() } just runs

        subject.onProxyUpdate()

        verify { mockNotificationManager.dismissPersistentProxyNotification() }
    }
}
