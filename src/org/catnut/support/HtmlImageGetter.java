/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.widget.TextView;

import java.io.File;

/**
 * 抓取textview html中的图片
 *
 * @author longkai
 */
public class HtmlImageGetter implements Html.ImageGetter {

	private TextView textView;
	private String location;

	public HtmlImageGetter(TextView htmlText, String location) {
		textView = htmlText;
		this.location = location;
	}

	@Override
	public Drawable getDrawable(String source) {
		String item = Uri.parse(source).getLastPathSegment();
		File image = new File(location + File.separator + item);
		try {
			if (image.exists()) {
				Drawable drawable = Drawable.createFromPath(image.getPath());
				if (drawable != null) {
					float scalingFactor =
							(float) textView.getMeasuredWidth() / drawable.getIntrinsicWidth();
					drawable.setBounds(0, 0, textView.getMeasuredWidth(),
							(int) (drawable.getIntrinsicHeight() * scalingFactor));
				}
				return drawable;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
}
