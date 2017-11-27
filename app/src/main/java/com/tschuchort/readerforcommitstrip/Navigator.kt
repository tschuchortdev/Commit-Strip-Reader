package com.tschuchort.readerforcommitstrip

interface Navigator {
	fun showSettings()
	fun showZoomedScreen(comic: Comic)
	fun navigateUp()
}