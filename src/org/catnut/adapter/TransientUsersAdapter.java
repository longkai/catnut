/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.support.TransientUser;

import java.util.List;

/**
 * 瞬时用户列表适配
 *
 * @author longkai
 */
public class TransientUsersAdapter extends ArrayAdapter<TransientUser> {

	private LayoutInflater mInflater;

	public TransientUsersAdapter(Context context, List<TransientUser> users) {
		super(context, R.layout.friend_row, users);
		mInflater = LayoutInflater.from(context);
	}

	private static class ViewHolder {
		ImageView avatar;
		TextView screenName;
		// TextView remark;
		ImageView verified;
		ImageView toggleFollowing;
		TextView location;
		TextView description;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		ViewHolder holder;
		if (view == null) {
			view = mInflater.inflate(R.layout.friend_row, null);
			holder = new ViewHolder();
			holder.avatar = (ImageView) view.findViewById(R.id.avatar);
			holder.screenName = (TextView) view.findViewById(R.id.nick);
			holder.verified = (ImageView) view.findViewById(R.id.verified);
			holder.toggleFollowing = (ImageView) view.findViewById(R.id.toggle_following);
			holder.location = (TextView) view.findViewById(R.id.location);
			holder.description = (TextView) view.findViewById(R.id.description);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		TransientUser user = getItem(position);
		Picasso.with(getContext())
				.load(user.avatarUrl)
				.placeholder(R.drawable.error)
				.error(R.drawable.error)
				.into(holder.avatar);
		holder.screenName.setText(user.screenName);
		// 是否加V
		if (user.verified) {
			holder.verified.setVisibility(View.VISIBLE);
		} else {
			holder.verified.setVisibility(View.GONE);
		}
		holder.location.setText(user.location);
		holder.description.setText(TextUtils.isEmpty(user.description)
				? getContext().getText(R.string.no_description) : user.description);
		return view;
	}
}
