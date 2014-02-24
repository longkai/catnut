/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.toolbox.ImageLoader;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.ui.ProfileActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;

/**
 * 评论界面列表适配器
 *
 * @author longkai
 */
public class CommentsAdapter extends CursorAdapter {

	private ImageLoader mImageLoader;
	private TweetImageSpan mImageSpan;

	public CommentsAdapter(Context context) {
		super(context, null, 0);
		mImageLoader = CatnutApp.getTingtingApp().getImageLoader();
		mImageSpan = new TweetImageSpan(context);
	}

	private static class ViewHolder {
		ImageView avatar;
		int avatarIndex;
		TextView screenName;
		int screenNameIndex;
		int remarkIndex;
		TextView createAt;
		int createAtIndex;
		TweetTextView text;
		int textIndex;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = LayoutInflater.from(context).inflate(R.layout.comment_row, null);
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) view.findViewById(R.id.avatar);
		holder.avatarIndex = cursor.getColumnIndex(User.profile_image_url);
		holder.screenName = (TextView) view.findViewById(R.id.screen_name);
		holder.screenNameIndex = cursor.getColumnIndex(User.screen_name);
		holder.remarkIndex = cursor.getColumnIndex(User.remark);
		holder.createAt = (TextView) view.findViewById(R.id.create_at);
		holder.createAtIndex = cursor.getColumnIndex(Status.created_at);
		holder.text = (TweetTextView) view.findViewById(R.id.text);
		holder.textIndex = cursor.getColumnIndex(Status.columnText);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		mImageLoader.get(cursor.getString(holder.avatarIndex),
				ImageLoader.getImageListener(holder.avatar, R.drawable.error, R.drawable.error));
		// 点击头像查看该用户主页
		final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
		final String screenName = cursor.getString(holder.screenNameIndex);
		holder.avatar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, ProfileActivity.class);
				intent.putExtra(Constants.ID, id);
				intent.putExtra(User.screen_name, screenName);
				context.startActivity(intent);
			}
		});
		String remark = cursor.getString(holder.remarkIndex);
		holder.screenName.setText(TextUtils.isEmpty(remark) ? screenName : remark);
		String date = cursor.getString(holder.createAtIndex);
		holder.createAt.setText(DateUtils.getRelativeTimeSpanString(DateTime.getTimeMills(date)));
		holder.text.setText(cursor.getString(holder.textIndex));
		CatnutUtils.vividTweet(holder.text, mImageSpan);
	}
}
