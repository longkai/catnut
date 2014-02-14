/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.toolbox.ImageLoader;
import tingting.chen.R;
import tingting.chen.metadata.User;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.util.TingtingUtils;

import java.util.regex.Matcher;

/**
 * 用户列表
 *
 * @author longkai
 */
public class UsersAdapter extends CursorAdapter {

	private ImageLoader mImageLoader;

	public UsersAdapter(Context context) {
		super(context, null, 0);
		mImageLoader = TingtingApp.getTingtingApp().getImageLoader();
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
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder holder = new ViewHolder();
		View view = LayoutInflater.from(context).inflate(R.layout.following_row, parent, false);
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
		if (TingtingUtils.getBoolean(cursor, User.verified)) {
			holder.verified.setVisibility(View.VISIBLE);
		} else {
			holder.verified.setVisibility(View.GONE);
		}

		String desc = cursor.getString(holder.descriptionIndex);
		if (!TextUtils.isEmpty(desc)) {
			holder.description.setText(desc);
			Linkify.addLinks(holder.description, TweetAdapter.WEB_URL, null, null, urlFilter);
			TingtingUtils.removeLinkUnderline(holder.description);
		} else {
			holder.description.setText(context.getText(R.string.no_description));
		}
	}

	private Linkify.TransformFilter urlFilter = new Linkify.TransformFilter() {
		@Override
		public String transformUrl(Matcher match, String url) {
			return url;
		}
	};
}
