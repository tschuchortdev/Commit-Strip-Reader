package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.AppModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface FeedComponent {
}