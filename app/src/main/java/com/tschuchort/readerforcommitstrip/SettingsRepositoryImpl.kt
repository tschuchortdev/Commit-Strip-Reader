package com.tschuchort.readerforcommitstrip

import android.content.Context
import android.content.res.Resources
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl
		@Inject constructor(@AppContext ctx: Context, private val rxPrefs: RxSharedPreferences, res: Resources)
	: SettingsRepository {

	init {
		// we need to initialize the shared preferences manually, or else the default values
		// won't be set until the SettingsActivity is started for the first time
		PreferenceManager.setDefaultValues(ctx, R.xml.preferences, false)
	}

	override val notifyAboutNewComics = BooleanPreference(res.getString(R.string.pref_key_notify_about_new_comics))

	inner class BooleanPreference(id: String) : SettingsRepository.Setting<Boolean> {
		private val pref = rxPrefs.getBoolean(id)

		override var value: Boolean
			get() = pref.get()
			set(value) { pref.set(value) }

		override fun observe() = BehaviorObservable(pref.get(), pref.asObservable())
	}
}