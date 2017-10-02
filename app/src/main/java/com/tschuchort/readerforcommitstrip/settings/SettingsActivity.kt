package com.tschuchort.readerforcommitstrip.settings


import android.os.Bundle
import android.preference.PreferenceActivity
import com.tschuchort.readerforcommitstrip.R

class SettingsActivity : PreferenceActivity() {
	@SuppressWarnings("deprecation")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		addPreferencesFromResource(R.xml.preferences)
	}
}