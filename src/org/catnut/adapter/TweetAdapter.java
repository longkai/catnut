/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
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
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;

import java.io.File;

/**
 * 微博列表适配器
 *
 * @author longkai
 */
public class TweetAdapter extends CursorAdapter {

	private static final String TAG = "TweetAdapter";

	private Context mContext;
	private ImageLoader mImageLoader;
	private TweetImageSpan mImageSpan;
	private boolean mThumbsRequired;
	private String mUserNick;

	/** 自定义字体，用户偏好 */
	private Typeface mCustomizedFont;
	private int mCustomizedFontSize;

	/**
	 * @param context
	 * @param nick 如果是不是某个用户的微博时间线，请赋null
	 */
	public TweetAdapter(Context context, String nick) {
		super(context, null, 0);
		mContext = context;
		this.mUserNick = nick;
		CatnutApp app = CatnutApp.getTingtingApp();
		mImageLoader = app.getImageLoader();
		SharedPreferences preferences = app.getPreferences();
		mThumbsRequired = preferences.getBoolean(context.getString(R.string.pref_show_tweet_thumbs), true);
		mCustomizedFontSize = CatnutUtils.resolveListPrefInt(
			preferences,
			context.getString(R.string.pref_tweet_font_size),
			context.getResources().getInteger(R.integer.default_tweet_font_size)
		);
		String fontPath = preferences.getString(context.getString(R.string.pref_customize_tweet_font), null);
		if (fontPath != null) {
			try {
				mCustomizedFont = Typeface.createFromFile(new File(fontPath));
			} catch (Exception e) {
				Log.e(TAG, "load customized font fail!", e);
			}
		}
		mImageSpan = new TweetImageSpan(mContext);
	}

	private static class ViewHolder {
		ImageView avatar;
		int avatarIndex;
		TextView create_at;
		int create_atIndex;
		TextView nick;
		int nickIndex;
		TweetTextView text;
		int textIndex;
		TextView replyCount;
		int replyCountIndex;
		TextView reteetCount;
		int reteetCountIndex;
		TextView source;
		int sourceIndex;
		TextView favoriteCount;
		int favoriteCountIndex;

		ImageView thumbs;
		int thumbsIndex;
		int remarkIndex;

		ImageView reply;
		ImageView retweet;
		ImageView favorite;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder holder = new ViewHolder();
		View view = LayoutInflater.from(context).inflate(R.layout.tweet_row, null);
		holder.nick = (TextView) view.findViewById(R.id.nick);
		// 如果是某个主页时间线
		if (mUserNick == null) {
			holder.nickIndex = cursor.getColumnIndex(User.screen_name);
			holder.avatar = (ImageView) view.findViewById(R.id.avatar);
			holder.avatarIndex = cursor.getColumnIndex(User.profile_image_url);
		}
		// 微博相关
		holder.text = (TweetTextView) view.findViewById(R.id.text);
		holder.textIndex = cursor.getColumnIndex(Status.columnText);
		holder.create_at = (TextView) view.findViewById(R.id.create_at);
		holder.create_atIndex = cursor.getColumnIndex(Status.created_at);
		holder.replyCount = (TextView) view.findViewById(R.id.reply_count);
		holder.replyCountIndex = cursor.getColumnIndex(Status.comments_count);
		holder.reteetCount = (TextView) view.findViewById(R.id.reteet_count);
		holder.reteetCountIndex = cursor.getColumnIndex(Status.reposts_count);
		holder.favoriteCount = (TextView) view.findViewById(R.id.favorite_count);
		holder.favoriteCountIndex = cursor.getColumnIndex(Status.attitudes_count);
		holder.source = (TextView) view.findViewById(R.id.source);
		holder.sourceIndex = cursor.getColumnIndex(Status.source);
		holder.thumbsIndex = cursor.getColumnIndex(Status.thumbnail_pic);
		holder.thumbs = (ImageView) view.findViewById(R.id.thumbs);
		holder.remarkIndex = cursor.getColumnIndex(User.remark);
		holder.reply = (ImageView) view.findViewById(R.id.action_reply);
		holder.retweet = (ImageView) view.findViewById(R.id.action_reteet);
		holder.favorite = (ImageView) view.findViewById(R.id.action_favorite);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
		// 用户相关
		if (mUserNick == null) {
			holder.avatar.setVisibility(View.VISIBLE);
			mImageLoader.get(cursor.getString(holder.avatarIndex),
			ImageLoader.getImageListener(holder.avatar, R.drawable.error, R.drawable.error));
			// 跳转到该用户的时间线
			final String nick = cursor.getString(holder.nickIndex);
			final long uid = cursor.getLong(cursor.getColumnIndex(Status.uid));
			holder.avatar.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(mContext, ProfileActivity.class);
					intent.putExtra(User.screen_name, nick);
					intent.putExtra(Constants.ID, uid);
					mContext.startActivity(intent);
				}
			});
			String remark = cursor.getString(holder.remarkIndex);
			holder.nick.setText(TextUtils.isEmpty(remark) ? cursor.getString(holder.nickIndex) : remark);
		} else {
			holder.nick.setText(mUserNick);
		}
		// 微博相关
		if (mCustomizedFont != null) {
			// 用户自定义字体
			holder.text.setTypeface(mCustomizedFont);
		}
		holder.reply.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, TweetActivity.class);
				intent.putExtra(Constants.ID, id);
				context.startActivity(intent);
			}
		});
		if (CatnutUtils.getBoolean(cursor, Status.favorited)) {
			holder.favorite.setImageResource(R.drawable.ic_tweet_action_inline_favorite_on);
		} else {
			holder.favorite.setImageResource(R.drawable.ic_tweet_action_inline_favorite_off);
		}
		holder.favorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// just go to the tweet' s detail todo maybe will need forward?
				Intent intent = new Intent(context, TweetActivity.class);
				intent.putExtra(Constants.ID, id);
				context.startActivity(intent);
			}
		});
		holder.retweet.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// just go to the tweet' s detail todo maybe will need forward?
				Intent intent = new Intent(context, TweetActivity.class);
				intent.putExtra(Constants.ID, id);
				context.startActivity(intent);
			}
		});
		holder.text.setTextSize(mCustomizedFontSize);
		holder.text.setText(cursor.getString(holder.textIndex));
		String create_at = cursor.getString(holder.create_atIndex);
		holder.create_at.setText(DateUtils.getRelativeTimeSpanString(DateTime.getTimeMills(create_at)));
		int replyCount = cursor.getInt(holder.replyCountIndex);
		holder.replyCount.setText(CatnutUtils.approximate(replyCount));
		int retweetCount = cursor.getInt(holder.reteetCountIndex);
		holder.reteetCount.setText(CatnutUtils.approximate(retweetCount));
		int favoriteCount = cursor.getInt(holder.favoriteCountIndex);
		holder.favoriteCount.setText(CatnutUtils.approximate(favoriteCount));
		String source = cursor.getString(holder.sourceIndex);
		// remove html tags, maybe we should do this after we load the data from cloud...
		holder.source.setText(Html.fromHtml(source).toString());
		// 是否需要缩略图，用户偏好
		boolean show = false;
		if (mThumbsRequired) {
			String thumbsUri = cursor.getString(holder.thumbsIndex);
			if (!TextUtils.isEmpty(thumbsUri)) {
				mImageLoader.get(thumbsUri,
					ImageLoader.getImageListener(holder.thumbs, R.drawable.error, R.drawable.error),
					holder.thumbs.getMaxWidth(), holder.thumbs.getMaxHeight());
				holder.thumbs.setVisibility(View.VISIBLE);
				show = true;
			}
		}
		if (!show) {
			holder.thumbs.setVisibility(View.GONE);
		}
		// 文字处理
		CatnutUtils.vividTweet(holder.text, mImageSpan);
	}
}
