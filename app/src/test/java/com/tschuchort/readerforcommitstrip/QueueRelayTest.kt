@file:Suppress("IllegalIdentifier")
package com.tschuchort.readerforcommitstrip

import android.test.suitebuilder.annotation.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class QueueRelayTest {

    private val relay = QueueRelay<Any?>()

    @Test(expected = IllegalArgumentException::class)
    fun `throws when accepting null with subscriber`() {
        relay.subscribe()
        relay.accept(null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when accepting null without subscriber`() {
        relay.accept(null)
    }

    @Test
    fun `queues values when no one is subscribed`() {
        relay.accept(1)
        relay.accept(2)

        relay.test().assertValues(1,2)
    }

    @Test
    fun `doesn't emit queued items twice`() {
        relay.accept(1)
        relay.accept(2)

        relay.test().assertValues(1,2)
        relay.test().assertEmpty()
    }

    @Test
    fun `emits values immediately when somebody is subscribed`() {
        val observer1 = relay.test()
        val observer2 = relay.test()

        relay.accept(1)
        relay.accept(2)

        observer1.assertValues(1,2)
        observer2.assertValues(1,2)
    }

    @Test
    fun `emits queued values before new emissions`() {
        relay.accept(1)
        relay.accept(2)

        val observer = relay.test()

        relay.accept(3)
        relay.accept(4)

        observer.assertValues(1,2,3,4)
    }

    @Test
    fun `doesn't emit values again to second observer after first observer unsubscribes`() {
        val disposable = relay.subscribe()

        relay.accept(1)
        relay.accept(2)

        disposable.dispose()

        relay.test().assertNoValues()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `queueSize can't be negative`() {
        QueueRelay<Any>(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `queueSize can't be 0`() {
        QueueRelay<Any>(0)
    }

    @Test(expected = RuntimeException::class)
    fun `queueSize is enforced`() {
        val r = QueueRelay<Any>(2)
        r.accept(1)
        r.accept(2)
        r.accept(3)
    }

    @Test
    fun `queue overflow drops latest`() {
        val r = QueueRelay<Any>(2, QueueRelay.OverflowStrategy.DROP_LATEST)
        r.accept(1)
        r.accept(2)
        r.accept(3)

        r.test().assertValues(1,2)
    }

    @Test
    fun `queue overflow drops oldest`() {
        val r = QueueRelay<Any>(2, QueueRelay.OverflowStrategy.DROP_OLDEST)
        r.accept(1)
        r.accept(2)
        r.accept(3)

        r.test().assertValues(2,3)
    }

    @Test
    fun `has no observers when no one subscribed`() {
        assertFalse(relay.hasObservers())
    }

    @Test
    fun `has observer when subscribed to`() {
        relay.subscribe()
        assertTrue(relay.hasObservers())
    }

    @Test
    fun `has observer when emptying queue`() {
        for(i in 0..10000)
            relay.accept(i)

        relay.subscribe()
        assertTrue(relay.hasObservers())
    }

    @Test
    fun `when a second observer subscribes while emptying the queue only the first one will get items`() {
        relay.accept(1)
        relay.accept(2)
        relay.accept(3)
        relay.accept(4)
        relay.accept(5)

        relay.test().assertValues(1,2,3,4,5)
        relay.test().assertEmpty()
    }
}