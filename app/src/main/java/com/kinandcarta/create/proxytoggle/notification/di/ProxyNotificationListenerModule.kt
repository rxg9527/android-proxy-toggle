package com.kinandcarta.create.proxytoggle.notification.di

import com.kinandcarta.create.proxytoggle.core.common.proxyupdate.ProxyUpdateListener
import com.kinandcarta.create.proxytoggle.notification.ProxyNotificationUpdateListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface ProxyNotificationListenerModule {

    @Binds
    @IntoSet
    fun bindProxyNotificationUpdateListener(
        listener: ProxyNotificationUpdateListener
    ): ProxyUpdateListener
}
