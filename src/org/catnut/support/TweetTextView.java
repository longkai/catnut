/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.content.Context;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * 自定义的微博文本view，目的是为了在{@link android.widget.ListView}中点击微博的时候能够传递到整个层级视图
 * <p/>
 * for detail please see: http://stackoverflow.com/questions/21891008/a-textview-in-a-listview-s-row-cannot-click-after-setting-descendantfocusabilit/21909839#21909839
 *
 * @author longkai
 */
public class TweetTextView extends TextView {

	public TweetTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Object text = this.getText();
		if (text instanceof Spanned) {
			Spannable buffer = Spannable.Factory.getInstance().newSpannable((CharSequence) text);

			int action = event.getAction();

			if (action == MotionEvent.ACTION_UP
				|| action == MotionEvent.ACTION_DOWN) {
				int x = (int) event.getX();
				int y = (int) event.getY();

				x -= this.getTotalPaddingLeft();
				y -= this.getTotalPaddingTop();

				x += this.getScrollX();
				y += this.getScrollY();

				Layout layout = this.getLayout();
				int line = layout.getLineForVertical(y);
				int off = layout.getOffsetForHorizontal(line, x);

				ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

				if (link.length != 0) {
					if (action == MotionEvent.ACTION_UP) {
						link[0].onClick(this);
					} else if (action == MotionEvent.ACTION_DOWN) {
						Selection.setSelection(
							buffer,
							buffer.getSpanStart(link[0]),
							buffer.getSpanEnd(link[0])
						);
					}
					return true;
				}
			}
		}
		return false;
	}
}
