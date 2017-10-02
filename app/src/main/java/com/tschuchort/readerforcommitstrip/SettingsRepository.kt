package com.tschuchort.readerforcommitstrip

import io.reactivex.Observable

interface SettingsRepository {
	val notifyAboutNewComics: Setting<Boolean>

	interface Setting<T> {
		var value: T
		fun observe(): BehaviorObservable<T>
	}
}