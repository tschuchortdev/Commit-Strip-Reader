package com.tschuchort.readerforcommitstrip

import android.app.Activity
import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.feed.FeedActivity
import com.tschuchort.readerforcommitstrip.feed.FeedContract
import com.tschuchort.readerforcommitstrip.feed.FeedPresenter
import com.tschuchort.readerforcommitstrip.zoom.ZoomActivity
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract
import com.tschuchort.readerforcommitstrip.zoom.ZoomPresenter
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import io.reactivex.Scheduler
import javax.inject.Scope

/**
 * provides activity scoped dependencies
 */
@PerActivity
@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {
    fun exposeFeedPresenter(): FeedContract.Presenter
    fun exposeZoomPresenterFactory(): ZoomContract.Presenter.Factory
    fun provideNavigator(): Navigator
    fun exposeActivity(): Activity

    fun inject(activity: FeedActivity)
    fun inject(activity: ZoomActivity)
}

@AllOpen
@Module
class ActivityModule(private val activity: Activity) {
    @Provides @PerActivity
    fun provideFeedPresenter(presenter: FeedPresenter): FeedContract.Presenter = presenter

    @Provides @PerActivity
    fun provideZoomPresenterFactory(@UiScheduler uiScheduler: Scheduler,
                                    navigator: Navigator,
                                    analytics: FirebaseAnalytics,
                                    storage: LocalStorage,
                                    repo: ComicRepository)
            : ZoomContract.Presenter.Factory
            = object : ZoomContract.Presenter.Factory {

        override fun create(selectedComic: Comic)
                = ZoomPresenter(selectedComic, uiScheduler, navigator, repo, storage)
    }

    @Provides @PerActivity
    fun provideActivity() = activity

    @Provides @PerActivity
    fun provideNavigator(navigatorImpl: NavigatorImpl): Navigator = navigatorImpl
}


@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class PerActivity