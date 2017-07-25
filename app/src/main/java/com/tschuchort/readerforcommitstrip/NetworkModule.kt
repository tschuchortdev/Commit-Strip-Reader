package com.tschuchort.readerforcommitstrip

import com.squareup.moshi.JsonQualifier
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
open class NetworkModule {

	@Provides
	@Singleton
	fun provideHttpClient() = OkHttpClient.Builder().build()!!

	@Provides
	@Singleton
	@XmlConverter
	fun provideXmlConverter(): Converter.Factory = SimpleXmlConverterFactory.create()

	@Provides
	@Singleton
	@JsonConverter
	fun provideJsonConverter(): Converter.Factory = MoshiConverterFactory.create()

	@Provides
	@Singleton
	fun provideWebService(httpClient: OkHttpClient, @XmlConverter xmlConverter: Converter.Factory): CommitStripWebService
			= Retrofit.Builder()
			.client(httpClient)
			.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
			.addConverterFactory(xmlConverter)
			.baseUrl("http://www.commitstrip.com/")
			.build()
			.create(CommitStripWebService::class.java)

	@Provides
	@Singleton
	fun provideComicRepository(repo: ComicRepositoryImpl): ComicRepository = repo
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class XmlConverter

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonConverter
