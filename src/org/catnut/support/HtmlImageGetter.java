/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.widget.TextView;
import org.catnut.R;
import org.catnut.util.CatnutUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 抓取textview html中的图片
 *
 * @author longkai
 */
public class HtmlImageGetter implements Html.ImageGetter {

	private TextView textView;
	private Drawable defaultDrawable;

	public HtmlImageGetter(TextView htmlText, Drawable defaultDrawable) {
		textView = htmlText;
		this.defaultDrawable = defaultDrawable == null ?
				textView.getResources().getDrawable(R.drawable.error) : defaultDrawable;
	}

	@Override
	public Drawable getDrawable(String source) {
		String dir;
		try {
			dir = CatnutUtils.mkdir(textView.getContext(), "zhihu");
		} catch (Exception e) {
			// sd card not found!
			return null;
		}
		String item = Uri.parse(source).getLastPathSegment();
		File image = new File(dir + File.separator + item);
		try {
			if (image.exists()) {
				Drawable drawable = Drawable.createFromPath(image.getPath());
				if (drawable != null) {
					float scalingFactor =
							(float) textView.getMeasuredWidth() / drawable.getIntrinsicWidth();
					drawable.setBounds(0, 0, textView.getMeasuredWidth(),
							(int) (drawable.getIntrinsicHeight()*scalingFactor));
				}
				return drawable;
			} else {
				URLDrawable urlDrawable = new URLDrawable(defaultDrawable);
				new AsyncThread(urlDrawable).execute(source, image.getPath());
				return urlDrawable;
			}
		} catch (Exception e) {
			return null;
		}
	}

	private class AsyncThread extends AsyncTask<String, Integer, Drawable> {
		private URLDrawable _drawable;

		public AsyncThread(URLDrawable drawable){
			_drawable = drawable;
		}

		@Override
		protected Drawable doInBackground(String... strings) {
			Drawable drawable = null;
			InputStream inputStream = null;
			FileOutputStream out = null;

			try {
				URL url = new URL(strings[0]);
				inputStream = url.openStream();
				// save locally
				out = new FileOutputStream(new File(strings[1]));
				byte buffer[] = new byte[1024];
				int tmp;
				while ((tmp = inputStream.read(buffer)) != -1) {
					out.write(buffer, 0, tmp);
				}
				drawable = Drawable.createFromStream(inputStream, null);
			} catch (IOException e) {
			} finally {
				CatnutUtils.closeIO(out, inputStream);
			}
			return drawable;
		}

		@Override
		protected void onPostExecute(Drawable result) {
			float scalingFactor =
					(float) textView.getMeasuredWidth() / _drawable.getIntrinsicWidth();
			_drawable.setBounds(0, 0, textView.getMeasuredWidth(),
					(int) (_drawable.getIntrinsicHeight()*scalingFactor));
			textView.setText(textView.getText());
		}
	}

	public static class URLDrawable extends BitmapDrawable {

		private Drawable drawable;

		public URLDrawable(Drawable defaultDraw){
			if (defaultDraw != null) {
				setDrawable(defaultDraw);
			}
		}

		private void setDrawable(Drawable ndrawable){
			drawable = ndrawable;
			drawable.setBounds(0, 0,
					drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight()
			);
			setBounds(0, 0, drawable.getIntrinsicWidth(), drawable
					.getIntrinsicHeight());
		}

		@Override
		public void draw(Canvas canvas) {
			if (drawable != null) {
				drawable.draw(canvas);
			}
		}
	}
}
