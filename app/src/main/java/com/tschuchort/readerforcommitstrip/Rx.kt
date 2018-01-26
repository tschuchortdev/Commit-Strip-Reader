package com.tschuchort.readerforcommitstrip

import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.reactivestreams.Publisher
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * dropMap is basically the opposite of switchMap
 *
 * instead of unsubscribing from unfinished observables when a new one comes in,
 * it drops all new emissions until the one currently running is finished
 */
inline fun <T,S> Observable<T>.dropMap(crossinline f: (T) -> Observable<out S>): Observable<out S>
		= toFlowable(io.reactivex.BackpressureStrategy.DROP)
		.flatMap({ f(it).toFlowable(BackpressureStrategy.ERROR) }, 1)
		.toObservable()

/**
 * dropMap is basically the opposite of switchMap
 *
 * instead of unsubscribing from unfinished observables when a new one comes in,
 * it drops all new emissions until the one currently running is finished
 */
inline fun <T,S> Flowable<T>.dropMap(crossinline f: (T) -> Publisher<out S>): Flowable<out S>
		= onBackpressureDrop()
		.flatMap { f(it) }

/**
 * dropMap is basically the opposite of switchMap
 *
 * instead of unsubscribing from unfinished observables when a new one comes in,
 * it drops all new emissions until the one currently running is finished
 */
inline fun <T,S> Observable<T>.dropMapSingle(crossinline f: (T) -> SingleSource<out S>): Observable<out S>
		= toFlowable(io.reactivex.BackpressureStrategy.DROP)
		.flatMapSingle{ f(it) }
		.toObservable()

/**
 * dropMap is basically the opposite of switchMap
 *
 * instead of unsubscribing from unfinished observables when a new one comes in,
 * it drops all new emissions until the one currently running is finished
 */
inline fun <T,S> Flowable<T>.dropMapSingle(crossinline f: (T) -> SingleSource<out S>): Flowable<out S>
		= onBackpressureDrop()
		.flatMapSingle { f(it) }

inline fun <T,S> Flowable<T>.mapParallel(scheduler: Scheduler = Schedulers.computation(), maxConcurrency: Int = 4, crossinline f: (T) -> S): Flowable<S> =
		flatMap({
			Flowable.just(it)
					.subscribeOn(scheduler)
					.map { f(it) }
		}, maxConcurrency)

fun <T,S> Observable<Pair<T,S>>.unzip() = Pair(
		Observable.create<T> { emitter ->
			this@unzip.subscribe({ (fst, _) ->
				emitter.onNext(fst)
			}, { err ->
				emitter.onError(err)
			}, {
				emitter.onComplete()
			})
		},
		Observable.create<S> { emitter ->
			this@unzip.subscribe({ (_, snd) ->
				emitter.onNext(snd)
			}, { err ->
				emitter.onError(err)
			}, {
				emitter.onComplete()
			})
		})

fun <T,S> Flowable<Pair<T,S>>.unzip(firstObserver: Observer<T>, secondObserver: Observer<S>)
		= toObservable().unzip(firstObserver, secondObserver)

fun <T,S> Observable<Pair<T,S>>.unzip(firstObserver: Observer<T>, secondObserver: Observer<S>)
		= unzip().let { (fst, snd) ->
			fst.subscribeWith(firstObserver)
			snd.subscribeWith(secondObserver)
		}!!

fun <T,S,R> Flowable<Triple<T,S,R>>.unzip()
		= toObservable().unzip()

fun <T,S,R> Observable<Triple<T,S,R>>.unzip() = Triple(
		Observable.create<T> { emitter ->
			this@unzip.subscribe({ (fst, _, _) ->
				emitter.onNext(fst)
			}, { err ->
				emitter.onError(err)
			}, {
				emitter.onComplete()
			})
		},
		Observable.create<S> { emitter ->
			this@unzip.subscribe({ (_, snd, _) ->
				emitter.onNext(snd)
			}, { err ->
				emitter.onError(err)
			}, {
				emitter.onComplete()
			})
		},
		Observable.create<R> { emitter ->
			this@unzip.subscribe({ (_, _, thd) ->
				emitter.onNext(thd)
			}, { err ->
				emitter.onError(err)
			}, {
				emitter.onComplete()
			})
		})



fun <T,S,R> Flowable<Triple<T,S,R>>.unzip(firstObserver: Observer<T>, secondObserver: Observer<S>, thirdObserver: Observer<R>)
		= toObservable().unzip(firstObserver, secondObserver, thirdObserver)

fun <T,S,R> Observable<Triple<T,S,R>>.unzip(firstObserver: Observer<T>, secondObserver: Observer<S>, thirdObserver: Observer<R>)
		= unzip().let { (fst, snd, thd) ->
			fst.subscribeWith(firstObserver)
			snd.subscribeWith(secondObserver)
			thd.subscribeWith(thirdObserver)
		}!!

fun <T> Observable<T>.lastOrThrow(throwable: Throwable = IllegalStateException("observable emitted no items"))
		= lastOrError()
		.doOnError { throw throwable }
		.blockingGet()!!

fun <T> Flowable<T>.lastOrThrow(throwable: Throwable = IllegalStateException("observable emitted no items"))
		= lastOrError()
		.doOnError { throw throwable }
		.blockingGet()

fun <T> Single<T>.retryDelayed(delay: Long, timeUnit: TimeUnit = MILLISECONDS, times: Int = 0)
		= toFlowable().retryDelayed(delay, timeUnit, times).lastOrError()!!

fun <T> Observable<T>.retryDelayed(delay: Long, timeUnit: TimeUnit = MILLISECONDS, times: Int = 0)
		= toFlowable(BackpressureStrategy.ERROR).retryDelayed(delay, timeUnit, times).toObservable()!!

fun <T> Flowable<T>.retryDelayed(delay: Long, timeUnit: TimeUnit = MILLISECONDS, times: Int = -1)
		= retryWhen { errors ->
			errors.flatMapIndexed { error, count ->
				if (count < times || times < 0)
					Flowable.timer(delay, timeUnit)
				else
					Flowable.error(error)
			}
		}!!


fun <T> Flowable<T>.indexed(start: Int = 0) = zipWith(Flowable.range(start, Int.MAX_VALUE))
fun <T> Observable<T>.indexed(start: Int = 0) = zipWith(Observable.range(start, Int.MAX_VALUE))

inline fun <T,S> Flowable<T>.flatMapIndexed(crossinline mapper: (T, Int) -> Flowable<S>)
		= indexed().flatMap { (t, count) -> mapper(t, count) }!!

inline fun <T,S> Flowable<T>.flatMapSingleIndexed(crossinline mapper: (T, Int) -> Single<S>)
		= indexed().flatMapSingle { (t, count) -> mapper(t, count) }!!

inline fun <T,S> Flowable<T>.flatMapMaybeIndexed(crossinline mapper: (T, Int) -> Maybe<S>)
		= indexed().flatMapMaybe { (t, count) -> mapper(t, count) }!!

inline fun <T> Flowable<T>.flatCompletableIndexed(crossinline mapper: (T, Int) -> Completable)
		= indexed().flatMapCompletable { (t, count) -> mapper(t, count) }!!

inline fun <T,S> Observable<T>.flatMapIndexed(crossinline mapper: (T, Int) -> Observable<S>)
		= indexed().flatMap { (t, count) -> mapper(t, count) }!!

inline fun <T,S> Observable<T>.flatMapSingleIndexed(crossinline mapper: (T, Int) -> Single<S>)
		= indexed().flatMapSingle { (t, count) -> mapper(t, count) }!!

inline fun <T,S> Observable<T>.flatMapMaybeIndexed(crossinline mapper: (T, Int) -> Maybe<S>)
		= indexed().flatMapMaybe { (t, count) -> mapper(t, count) }!!

inline fun <T> Observable<T>.flatCompletableIndexed(crossinline mapper: (T, Int) -> Completable)
		= indexed().flatMapCompletable { (t, count) -> mapper(t, count) }!!

fun <T> Observable<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!
fun <T> Maybe<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!
fun <T> Single<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!
fun <T> Flowable<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!

/**
 * a BehaviorObservable is an Observable that emits its last value on subscribtion
 * like a BehaviorSubject but without the possibility to call accept() from outside
 */
class BehaviorObservable<T>(initialValue: T, source: Observable<T>) : Observable<T>() {
	private val subject = BehaviorSubject.createDefault(initialValue)

	init {
		source.subscribeWith(subject)
	}

	override fun subscribeActual(observer: Observer<in T>?) {
		subject.subscribeWith(observer)
	}

	val value: T get() = subject.value!!
}

/**
 * a relay that saves all emissions in a queue when no one is subscribed
 * then emits them again as soon as someone subscribes
 */
class QueueRelay<T>(
		private val queueSize: Int = Int.MAX_VALUE,
		private val overflowStrategy: OverflowStrategy = OverflowStrategy.ERROR) : Relay<T>() {

	private val queue: Queue<T> = LinkedList()
	private val relay = PublishRelay.create<T>()
	private var isEmptyingQueue = false

	init {
		require(queueSize > 0)
	}

	override fun subscribeActual(observer: Observer<in T>) {
		if(queue.isNotEmpty() && !isEmptyingQueue) {
			isEmptyingQueue = true

			for(item in queue.pollIterator()) {
				observer.onNext(item)
			}

			isEmptyingQueue = false
		}

		relay.subscribe(observer)
	}

	override fun accept(value: T) {
		require(value != null)

		if(relay.hasObservers())
			relay.accept(value)
		else if(queue.size < queueSize) {
			queue += value
		}
		else when(overflowStrategy) {
			OverflowStrategy.ERROR -> throw RuntimeException("queue size exceeded")
			OverflowStrategy.DROP_OLDEST -> {
				queue.remove()
				queue += value
			}
			OverflowStrategy.DROP_LATEST -> Unit
		}
	}

	override fun hasObservers() = isEmptyingQueue || relay.hasObservers()

	enum class OverflowStrategy { ERROR, DROP_LATEST, DROP_OLDEST }
}