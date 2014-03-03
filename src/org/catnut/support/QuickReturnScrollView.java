/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * a customized scroll view can be used for quick return tricks.
 */
public class QuickReturnScrollView extends ScrollView {
	private Callbacks callbacks;

	public QuickReturnScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if (callbacks != null) {
			callbacks.onScrollChanged(t);
		}
	}

	@Override
	public int computeVerticalScrollRange() {
		return super.computeVerticalScrollRange();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (callbacks != null) {
			switch (ev.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					callbacks.onDownMotionEvent();
					break;
				case MotionEvent.ACTION_UP:
					// fall through
				case MotionEvent.ACTION_CANCEL:
					callbacks.onUpOrCancelMotionEvent();
					break;
				default:
					break;
			}
		}
		return super.onTouchEvent(ev);
	}

	public void setCallbacks(Callbacks callbacks) {
		this.callbacks = callbacks;
	}

	public static interface Callbacks {
		int STATE_ON_SCREEN = 0;
		int STATE_OFF_SCREEN = 1;
		int STATE_RETURNING = 2;

		void onScrollChanged(int scrollY);

		void onDownMotionEvent();

		void onUpOrCancelMotionEvent();
	}
}
