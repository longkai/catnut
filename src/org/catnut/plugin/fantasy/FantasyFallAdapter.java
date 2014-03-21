/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.fantasy;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import org.catnut.ui.HelloActivity;
import org.catnut.util.CatnutUtils;

/**
 * 瀑布流适配
 *
 * @author longkai
 */
public class FantasyFallAdapter extends CursorAdapter implements View.OnTouchListener {

	private static final int N = 5; // 5种随机颜色

	private ColorDrawable[] mColors;
	private float mFactor;

	public FantasyFallAdapter(Context context) {
		super(context, null, 0);
		int width = CatnutUtils.getScreenWidth(context);
		mFactor = width >> 1; // 缩放为屏幕宽度的一半
		mColors = new ColorDrawable[N];
		Resources res = context.getResources();
		mColors[0] = new ColorDrawable(res.getColor(android.R.color.holo_orange_light));
		mColors[1] = new ColorDrawable(res.getColor(android.R.color.holo_purple));
		mColors[2] = new ColorDrawable(res.getColor(android.R.color.holo_green_light));
		mColors[3] = new ColorDrawable(res.getColor(android.R.color.holo_red_light));
		mColors[4] = new ColorDrawable(res.getColor(android.R.color.holo_blue_light));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final ImageView view = new ImageView(context);
		view.setAdjustViewBounds(true);
		view.setScaleType(ImageView.ScaleType.FIT_CENTER);
		view.setOnTouchListener(this);

		ViewHolder holder = new ViewHolder();
		holder.urlIndex = cursor.getColumnIndex(Photo.image_url);
		holder.nameIndex = cursor.getColumnIndex(Photo.name);
		holder.widthIndex = cursor.getColumnIndex(Photo.width);
		holder.heightIndex = cursor.getColumnIndex(Photo.height);

		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		final ImageView imageView = (ImageView) view;
		ViewHolder holder = (ViewHolder) imageView.getTag();
		int width = cursor.getInt(holder.widthIndex);
		int height = cursor.getInt(holder.heightIndex);
		final String url = cursor.getString(cursor.getColumnIndex(Photo.image_url));
		Picasso.with(context)
				.load(url)
				.placeholder(mColors[((int) (Math.random() * N))])
				.resize((int) mFactor, (int) (mFactor * height / width))
				.into(imageView);

		final String name = cursor.getString(holder.nameIndex);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// clear
				imageView.getDrawable().clearColorFilter();
				imageView.invalidate();
				// intent
				Intent intent = new Intent(context, HelloActivity.class);
				intent.setAction(HelloActivity.ACTION_FROM_GRID);
				intent.putExtra(Photo.image_url, url);
				intent.putExtra(Photo.name, name);
				context.startActivity(intent);
			}
		});
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return CatnutUtils.imageOverlay(v, event);
	}

	private static class ViewHolder {
		int urlIndex;
		int widthIndex;
		int heightIndex;
		int nameIndex;
	}
}
