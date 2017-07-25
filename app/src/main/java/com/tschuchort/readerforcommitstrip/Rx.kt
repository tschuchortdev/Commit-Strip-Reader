package com.tschuchort.readerforcommitstrip

import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher

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

fun <T,S> Observable<Pair<T,S>>.unzip(firstObserver: Observer<T>, secondObserver: Observer<S>)
		= unzip().let { (fst, snd) ->
			fst.subscribeWith(firstObserver)
			snd.subscribeWith(secondObserver)
		}

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

fun <T,S,R> Observable<Triple<T,S,R>>.unzip(firstObserver: Observer<T>, secondObserver: Observer<S>, thirdObserver: Observer<R>)
		= unzip().let { (fst, snd, thd) ->
			fst.subscribeWith(firstObserver)
			snd.subscribeWith(secondObserver)
			thd.subscribeWith(thirdObserver)
		}

fun <T> Observable<T>.lastOrThrow() = this
		.lastOrError()
		.doOnError { throw IllegalStateException("observable emitted no items") }
		.blockingGet()!!

fun <T> Flowable<T>.lastOrThrow() = this
		.lastOrError()
		.doOnError { throw IllegalStateException("flowable emitted no items") }
		.blockingGet()

fun <T> Observable<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!
fun <T> Maybe<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!
fun <T> Single<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!
fun <T> Flowable<T>.onErrorReturn(value: T) = onErrorReturnItem(value)!!