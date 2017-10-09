package com.tschuchort.readerforcommitstrip

import io.reactivex.Observable
import io.reactivex.Single

interface SystemManager {
	fun observeInternetConnectivity(): Observable<Boolean>
	val isInternetConnected: Single<Boolean>
}