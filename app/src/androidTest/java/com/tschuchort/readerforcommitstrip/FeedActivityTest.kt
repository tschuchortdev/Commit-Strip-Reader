package com.tschuchort.readerforcommitstrip

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import com.tschuchort.readerforcommitstrip.feed.FeedContract
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock


@LargeTest
@RunWith(AndroidJUnit4::class)
class FeedActivityTest : BaseActivityTest() {

	private val testComic = Comic("Test Comic", "0/0/1970", null,
			"http://www.commitstrip.com/en/2017/09/21/project-scope/",
			"https://www.commitstrip.com/wp-content/uploads/2017/09/Strip-Dans-la-scope-650-finalenglish.jpg")

	private val testState = FeedContract.State(
			comics = List(10) { testComic }
					.mapIndexed { index, comic ->
						comic.copy(title = "${comic.title} $index")
					},
			feedOrientation = Orientation.VERTICAL,
			loading = false,
			internetConnected = true,
			refreshing = false,
			selectedComic = null
	)


	@Mock
	lateinit var mockPresenter: Presenter

	/*@Test
	fun testShareDialogShown() {
		doOnUi {
			activity.doSideEffect(ViewEffect.Share(testComic))
		}

		//intending(hasAction(ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_OK, null))
		//intended(hasAction(ACTION_CHOOSER))
	}*/

	@Test
	fun testNoInternetWarningShown() {
		verifyState(testState.copy(internetConnected = false))
	}

	@Test
	fun testRefreshingShown() {
		verifyState(testState.copy(refreshing = true, selectedComic = testComic))
	}

	@Test
	fun testShareSaveDialogShown() {
		verifyState(testState.copy(selectedComic = testComic))
	}

	private fun verifyState(state: State) {
		doOnUi {
			activity.render(state)
		}

		assertStateDisplayed(state)
	}

	/**
	 * checks that all state is displayed correctly, not only the view currently being tested
	 */
	private fun assertStateDisplayed(state: State) {
		val bottomSheetSave = onView(withText(getResources().getString(R.string.dialog_option_save)))
		val bottomSheetShare = onView(withText(getResources().getString(R.string.dialog_option_share)))

		if (state.selectedComic != null) {
			// check save/share bottom sheet
			bottomSheetSave
					.inRoot(isDialog())
					.check(matches(isDisplayed()))

			bottomSheetShare
					.inRoot(isDialog())
					.check(matches(isDisplayed()))
		}
		else {
			bottomSheetSave.check(doesNotExist())
			bottomSheetShare.check(doesNotExist())

			// check refreshing
			onView(withId(R.id.swipe_refresh_layout)).check(matches(
					if (state.refreshing)
						isRefreshing()
					else
						not(isRefreshing())
			))

			// check internet warning
			onView(withId(R.id.no_internet_warning)).check(matches(
					if (state.internetConnected)
						not(isDisplayed())
					else
						isDisplayed()
			))
		}
	}
}