/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import org.catnut.R;
import org.catnut.support.TweetImageSpan;

import java.io.IOException;
import java.io.InputStream;

/**
 * 表情网格适配
 *
 * @author longkai
 */
public class EmotionsAdapter extends BaseAdapter {

	private static final String TAG = "EmotionsAdapter";

	private Context mContext;
	private String[] mKeys;

	public EmotionsAdapter(Context context) {
		mContext = context;
		mKeys = new String[TweetImageSpan.EMOTIONS.size()];
		TweetImageSpan.EMOTIONS.keySet().toArray(mKeys);
	}

	@Override
	public int getCount() {
		return mKeys.length;
	}

	@Override
	public Object getItem(int position) {
		return mKeys[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		if (convertView == null) {  // if it's not recycled, initialize some attributes
			imageView = new ImageView(mContext);
		} else {
			imageView = (ImageView) convertView;
		}

		InputStream inputStream = null;
		try {
			inputStream = mContext.getAssets()
					.open(TweetImageSpan.EMOTIONS_DIR + TweetImageSpan.EMOTIONS.get(mKeys[position]));
			imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
		} catch (IOException e) {
			imageView.setImageResource(R.drawable.error);
			Log.e(TAG, "load emotion fail!", e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					Log.e(TAG, "close input stream error!", e);
				}
			}
		}
		return imageView;
	}
}
