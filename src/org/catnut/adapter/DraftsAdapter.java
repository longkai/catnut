/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.metadata.Draft;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

/**
 * 草稿适配器
 *
 * @author longkai
 */
public class DraftsAdapter extends CursorAdapter {

	private LayoutInflater mInflater;
	private TweetImageSpan mTweetImageSpan;
	private int mScreenWidth;

	public DraftsAdapter(Context context) {
		super(context, null, 0);
		mInflater = LayoutInflater.from(context);
		mTweetImageSpan = new TweetImageSpan(context);
		mScreenWidth = CatnutUtils.getScreenWidth(context);
	}

	private static class ViewHolder {
		TweetTextView text;
		int textIndex;
		TextView createAt;
		int createAtIndex;
		ImageView imageView;
		int picIndex;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.draft_row, null);
		ViewHolder holder = new ViewHolder();
		holder.text = (TweetTextView) view.findViewById(R.id.text);
		holder.createAt = (TextView) view.findViewById(R.id.create_at);
		holder.textIndex = cursor.getColumnIndex(Draft.STATUS);
		holder.createAtIndex = cursor.getColumnIndex(Draft.CREATE_AT);
		holder.imageView = (ImageView) view.findViewById(R.id.image);
		holder.imageView.setAdjustViewBounds(true);
		holder.picIndex = cursor.getColumnIndex(Draft.PIC);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.text.setText(cursor.getString(holder.textIndex));
		CatnutUtils.vividTweet(holder.text, mTweetImageSpan);
		CharSequence createAt = DateUtils.getRelativeTimeSpanString(cursor.getLong(holder.createAtIndex));
		holder.createAt.setText(createAt);
		String pic = cursor.getString(holder.picIndex);
		if (!TextUtils.isEmpty(pic)) {
			Picasso.with(context)
					.load(Uri.parse(pic))
					.resize(mScreenWidth, (int) (mScreenWidth * Constants.GOLDEN_RATIO))
					.placeholder(R.drawable.error)
					.error(R.drawable.error)
					.into(holder.imageView);
			holder.imageView.setVisibility(View.VISIBLE);
		} else {
			holder.imageView.setVisibility(View.GONE);
		}
	}
}
