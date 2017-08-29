package com.tschuchort.readerforcommitstrip

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import android.view.Menu


/*abstract class BaseActivity<V : Contract.View<*,*,*>> : AppCompatActivity, V {

	protected abstract var presenter: Contract.Presenter<*,*,V,*>

	@CallSuper
	override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
		if(!presenter.viewIsAttached)
			presenter.attechView(this)

		return super.onPrepareOptionsMenu(menu)
	}

	@CallSuper
	override fun onSaveInstanceState(outState: Bundle?) {
		super.onSaveInstanceState(outState)
		presenter.onSaveInstanceState(outState)
	}

	@CallSuper
	override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
		super.onRestoreInstanceState(savedInstanceState)
		presenter.onRestoreInstanceState(savedInstanceState)
	}

	@CallSuper
	override fun onStop() {
		super.onStop()
		presenter.detachView(isFinishing)
	}
}*/