/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.util.Linkify;
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
import org.catnut.fragment.PrefFragment;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.util.CatnutUtils;
import org.catnut.util.DateTime;

import java.io.File;

import static org.catnut.support.TweetTextView.MENTION_FILTER;
import static org.catnut.support.TweetTextView.MENTION_PATTERN;
import static org.catnut.support.TweetTextView.MENTION_SCHEME;
import static org.catnut.support.TweetTextView.TOPIC_FILTER;
import static org.catnut.support.TweetTextView.TOPIC_PATTERN;
import static org.catnut.support.TweetTextView.TOPIC_SCHEME;
import static org.catnut.support.TweetTextView.URL_FILTER;
import static org.catnut.support.TweetTextView.WEB_URL;

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
		mThumbsRequired = preferences.getBoolean(PrefFragment.SHOW_TWEET_THUMBS, true);
		mCustomizedFontSize = CatnutUtils.resolveListPrefInt(preferences,
			PrefFragment.TWEET_FONT_SIZE, context.getResources().getInteger(R.integer.default_tweet_font_size));
		String fontPath = preferences.getString(PrefFragment.CUSTOMIZE_TWEET_FONT, null);
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
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder holder = new ViewHolder();
		View view = LayoutInflater.from(context).inflate(R.layout.tweet, null);
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
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		// 用户相关
		if (mUserNick == null) {
			mImageLoader.get(cursor.getString(holder.avatarIndex),
				ImageLoader.getImageListener(holder.avatar, R.drawable.error, R.drawable.error));
			holder.nick.setText(cursor.getString(holder.nickIndex));
		} else {
			holder.nick.setText(mUserNick);
		}
		// 微博相关
		if (mCustomizedFont != null) {
			// 用户自定义字体
			holder.text.setTypeface(mCustomizedFont);
		}
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
		// 表情处理
		holder.text.setText(mImageSpan.getImageSpan(holder.text.getText()));
		// 分别对微博的链接，@，##话题过滤
		// todo：对@，## 进行处理
		Linkify.addLinks(holder.text, MENTION_PATTERN, MENTION_SCHEME, null, MENTION_FILTER);
		Linkify.addLinks(holder.text, TOPIC_PATTERN, TOPIC_SCHEME, null, TOPIC_FILTER);
		Linkify.addLinks(holder.text, WEB_URL, null, null, URL_FILTER);
		CatnutUtils.removeLinkUnderline(holder.text);
	}
}
