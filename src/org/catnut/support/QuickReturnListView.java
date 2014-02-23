/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

/**
 * a list view that can compute the current scroll Y.
 */
public class QuickReturnListView extends ListView {
	private int itemCount;
	private int[] itemsOffsetY;
	private int scrollRange;
	private boolean isScrollRangeComputed;

	public QuickReturnListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public int getScrollRange() {
		return scrollRange;
	}

	public boolean isScrollRangeComputed() {
		return isScrollRangeComputed;
	}

	public void computeScrollRange() {
		scrollRange = 0;
		itemCount = getAdapter().getCount();
		if (itemsOffsetY == null) {
			itemsOffsetY = new int[itemCount];
		}
		for (int i = 0; i < itemCount; i++) {
			View view = getAdapter().getView(i, null, this);
			view.measure(
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
			);
			itemsOffsetY[i] = scrollRange;
			scrollRange += view.getMeasuredHeight();
		}
		isScrollRangeComputed = true;
	}

	public int getComputedScrollY() {
		int pos = getFirstVisiblePosition();
		return itemsOffsetY[pos] - getChildAt(0).getTop();
	}
}
