/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.toolbox.ImageLoader;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.User;
import org.catnut.util.CatnutUtils;

/**
 * 用户列表
 *
 * @author longkai
 */
public class UsersAdapter extends CursorAdapter {

	private ImageLoader mImageLoader;

	public UsersAdapter(Context context) {
		super(context, null, 0);
		mImageLoader = CatnutApp.getTingtingApp().getImageLoader();
	}

	private static class ViewHolder {

		ImageView avatar;
		int avatarIndex;
		TextView nick;
		int nickIndex;
		TextView location;
		int locationIndex;
		TextView description;
		int descriptionIndex;
		ImageView verified;
		int verifiedIndex;

		ImageView toggleFollowing;
		int followingIndex;
		int follow_meIndex;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder holder = new ViewHolder();
		View view = LayoutInflater.from(context).inflate(R.layout.friend_row , parent, false);
		holder.avatar = (ImageView) view.findViewById(R.id.avatar);
		holder.avatarIndex = cursor.getColumnIndex(User.profile_image_url);
		holder.nick = (TextView) view.findViewById(R.id.nick);
		holder.nickIndex = cursor.getColumnIndex(User.screen_name);
		holder.location = (TextView) view.findViewById(R.id.location);
		holder.locationIndex = cursor.getColumnIndex(User.location);
		holder.description = (TextView) view.findViewById(R.id.description);
		holder.descriptionIndex = cursor.getColumnIndex(User.description);
		holder.verified = (ImageView) view.findViewById(R.id.verified);
		holder.verifiedIndex = cursor.getColumnIndex(User.verified);
		holder.toggleFollowing = (ImageView) view.findViewById(R.id.toggle_following);
		holder.followingIndex = cursor.getColumnIndex(User.following);
		holder.follow_meIndex = cursor.getColumnIndex(User.follow_me);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		mImageLoader.get(cursor.getString(holder.avatarIndex),
			ImageLoader.getImageListener(holder.avatar, R.drawable.error, R.drawable.error));
		holder.nick.setText(cursor.getString(holder.nickIndex));
		holder.location.setText(cursor.getString(holder.locationIndex));
		if (CatnutUtils.getBoolean(cursor, User.verified)) {
			holder.verified.setVisibility(View.VISIBLE);
		} else {
			holder.verified.setVisibility(View.GONE);
		}

		String desc = cursor.getString(holder.descriptionIndex);
		if (!TextUtils.isEmpty(desc)) {
			holder.description.setText(desc);
		} else {
			holder.description.setText(context.getText(R.string.no_description));
		}
		holder.toggleFollowing.setVisibility(View.VISIBLE);
		if (cursor.getInt(holder.followingIndex) == 1) {
			holder.toggleFollowing.setImageResource(R.drawable.btn_inline_following);
		} else {
			holder.toggleFollowing.setImageResource(R.drawable.btn_inline_follow);
		}
	}
}
