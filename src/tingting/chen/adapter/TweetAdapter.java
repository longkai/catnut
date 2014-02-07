/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.adapter;

import android.content.Context;
import android.database.Cursor;
import android.media.Image;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.toolbox.ImageLoader;
import tingting.chen.R;
import tingting.chen.metadata.Status;
import tingting.chen.metadata.User;
import tingting.chen.tingting.TingtingApp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 微博列表适配器
 *
 * @author longkai
 */
public class TweetAdapter extends CursorAdapter {

	private ImageLoader mImageLoader;

	private boolean isInflate;

	public TweetAdapter(Context context) {
		super(context, null, 0);
		mImageLoader = TingtingApp.getTingtingApp().getImageLoader();
	}

	private static class ViewHolder {
		ImageView avatar;
		int avatarIndex;
		TextView create_at;
		int create_atIndex;
		TextView nick;
		int nickIndex;
		TextView text;
		int textIndex;

		ImageView thumbs;
		int thumbsIndex;
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.ENGLISH);

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder holder = new ViewHolder();
		View view = LayoutInflater.from(context).inflate(R.layout.tweet, null);
		holder.text = (TextView) view.findViewById(R.id.text);
		holder.textIndex = cursor.getColumnIndex(Status.columnText);
		holder.nick = (TextView) view.findViewById(R.id.nick);
		holder.nickIndex = cursor.getColumnIndex(User.screen_name);
		holder.create_at = (TextView) view.findViewById(R.id.create_at);
		holder.create_atIndex = cursor.getColumnIndex(Status.created_at);
		holder.thumbsIndex = cursor.getColumnIndex(Status.thumbnail_pic);
		holder.avatar = (ImageView) view.findViewById(R.id.avatar);
		holder.avatarIndex = cursor.getColumnIndex(User.profile_image_url);

		ViewStub stub = (ViewStub) view.findViewById(R.id.view_stub);
		if (!TextUtils.isEmpty(cursor.getString(holder.thumbsIndex))) {
			holder.thumbs = (ImageView) stub.inflate();
		}
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.text.setText(cursor.getString(holder.textIndex));
		mImageLoader.get(cursor.getString(holder.avatarIndex),
			ImageLoader.getImageListener(holder.avatar, R.drawable.error, R.drawable.error));
		holder.nick.setText(cursor.getString(holder.nickIndex));
		try {
			Date parse = sdf.parse(cursor.getString(holder.create_atIndex));
			holder.create_at.setText(DateUtils.getRelativeTimeSpanString(parse.getTime()));
		} catch (ParseException e) {
		}
		String thumbsUri = cursor.getString(holder.thumbsIndex);
		if (holder.thumbs != null && !TextUtils.isEmpty(thumbsUri)) {
			mImageLoader.get(thumbsUri,
				ImageLoader.getImageListener(holder.thumbs, R.drawable.error, R.drawable.error),
				holder.thumbs.getMaxWidth(), holder.thumbs.getMaxHeight());
		}
	}
}
