package com.kinandcarta.create.proxytoggle.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kinandcarta.create.proxytoggle.core.common.stub.Stubs.PROXY
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ProxyNotificationManagerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var subject: ProxyNotificationManager

    @Before
    fun setUp() {
        subject = ProxyNotificationManager(context)
        subject.dismissPersistentProxyNotification()
    }

    @After
    fun tearDown() {
        subject.dismissPersistentProxyNotification()
    }

    @Test
    fun `showPersistentProxyNotification() - posts ongoing notification with proxy details`() {
        subject.showPersistentProxyNotification(PROXY)

        val notification = shadowOf(notificationManager).allNotifications.single()

        assertThat(notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            .isEqualTo("Proxy enabled")
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            .isEqualTo("Current proxy: ${PROXY}")
        assertThat(notification.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
        assertThat(notification.actions.single().title.toString()).isEqualTo("Disable proxy")
    }

    @Test
    fun `dismissPersistentProxyNotification() - clears posted notification`() {
        subject.showPersistentProxyNotification(PROXY)

        subject.dismissPersistentProxyNotification()

        assertThat(shadowOf(notificationManager).allNotifications).isEmpty()
    }
}
