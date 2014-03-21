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
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.metadata.User;
import org.catnut.util.CatnutUtils;

/**
 * 用户列表
 *
 * @author longkai
 */
public class UsersAdapter extends CursorAdapter {

	private LayoutInflater mInflater;

	public UsersAdapter(Context context) {
		super(context, null, 0);
		mInflater = LayoutInflater.from(context);
	}

	private static class ViewHolder {
		ImageView avatar;
		int avatarIndex;
		TextView screenName;
		int screenNameIndex;
		int remarkIndex;
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
		View view = mInflater.inflate(R.layout.friend_row , parent, false);
		holder.avatar = (ImageView) view.findViewById(R.id.avatar);
		holder.avatarIndex = cursor.getColumnIndex(User.profile_image_url);
		holder.screenName = (TextView) view.findViewById(R.id.nick);
		holder.screenNameIndex = cursor.getColumnIndex(User.screen_name);
		holder.remarkIndex = cursor.getColumnIndex(User.remark);
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
		Picasso.with(context)
				.load(cursor.getString(holder.avatarIndex))
				.placeholder(R.drawable.error)
				.error(R.drawable.error)
				.into(holder.avatar);
		String remark = cursor.getString(holder.remarkIndex);
		holder.screenName.setText(TextUtils.isEmpty(remark)?cursor.getString(holder.screenNameIndex): remark);
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
