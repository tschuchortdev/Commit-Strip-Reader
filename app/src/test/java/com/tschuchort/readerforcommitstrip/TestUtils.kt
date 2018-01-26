package com.tschuchort.readerforcommitstrip

import org.junit.Assert.fail


inline fun <reified T : Throwable> expect(crossinline f: () -> Unit) {
    try {
        f()
        fail("expected exception ${T::class.simpleName} but none was thrown")
    }
    catch(e: Throwable) {
        if(e !is T) {
            fail("expected exception ${T::class.simpleName} but caught ${e::class.simpleName}")
            throw(e)
        }
    }
}
