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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.api.FavoritesAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.StatusProcessor;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.ui.ProfileActivity;
import org.catnut.ui.SingleFragmentActivity;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 微博列表适配器
 *
 * @author longkai
 */
public class TweetAdapter extends CursorAdapter implements View.OnClickListener {

	private static final String TAG = "TweetAdapter";

	private Context mContext;
	private RequestQueue mRequestQueue;
	private TweetImageSpan mImageSpan;
	private String mThumbsOption;
	private boolean mSmall;
	private String mScreenName;

	/** 自定义字体，用户偏好 */
	private Typeface mCustomizedFont;
	private int mCustomizedFontSize;
	private float mCustomizedLineSpacing = 1.0f;

	/**
	 * @param context
	 * @param nick    如果是不是某个用户的微博时间线，请赋null
	 */
	public TweetAdapter(Context context, String nick) {
		super(context, null, 0);
		mContext = context;
		this.mScreenName = nick;
		CatnutApp app = CatnutApp.getTingtingApp();
		mRequestQueue = app.getRequestQueue();
		SharedPreferences preferences = app.getPreferences();
		mThumbsOption = preferences.getString(
				context.getString(R.string.pref_thumbs_options),
				context.getString(R.string.thumb_small)
		);
		mCustomizedFontSize = CatnutUtils.resolveListPrefInt(
				preferences,
				context.getString(R.string.pref_tweet_font_size),
				context.getResources().getInteger(R.integer.default_tweet_font_size)
		);
		mCustomizedFont = CatnutUtils.getTypeface(preferences, context.getString(R.string.pref_customize_tweet_font));
		mImageSpan = new TweetImageSpan(mContext);
		mCustomizedLineSpacing = CatnutUtils.getLineSpacing(preferences,
				context.getString(R.string.pref_line_spacing),
				context.getString(R.string.default_line_spacing));
	}

	private void showPopupMenu(View view) {
		final Bean bean = (Bean) view.getTag();
		PopupMenu popup = new PopupMenu(mContext, view);
		Menu menu = popup.getMenu();
		popup.getMenuInflater().inflate(R.menu.tweet_overflow, menu);
		MenuItem item = menu.findItem(R.id.action_favorite);
		item.setTitle(bean.favorited ? R.string.cancle_favorite : R.string.favorite);
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
					case R.id.action_favorite:
						mRequestQueue.add(new CatnutRequest(
								mContext,
								bean.favorited ? FavoritesAPI.destroy(bean.id) : FavoritesAPI.create(bean.id),
								new StatusProcessor.FavoriteTweetProcessor(),
								new Response.Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										Toast.makeText(mContext,
												bean.favorited ?
														R.string.cancle_favorite_success :
														R.string.favorite_success,
												Toast.LENGTH_SHORT).show();
									}
								},
								new Response.ErrorListener() {
									@Override
									public void onErrorResponse(VolleyError error) {
										WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
										Toast.makeText(mContext, weiboAPIError.error, Toast.LENGTH_LONG).show();
									}
								}
						));
						break;
					case R.id.action_like:
						Toast.makeText(mContext, "sina not provide this option...", Toast.LENGTH_SHORT).show();
						break;
					default:
						break;
				}
				return false;
			}
		});
		popup.show();
	}

	@Override
	public void onClick(final View v) {
		v.post(new Runnable() {
			@Override
			public void run() {
				showPopupMenu(v);
			}
		});
	}

	private static class ViewHolder {
		ImageView avatar;
		int avatarIndex;
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
		TextView likeCount;
		int likeCountIndex;

		ImageView thumbs;
		int thumbsIndex;
		int originalPicIndex;
		int remarkIndex;

		ImageView reply;
		ImageView retweet;

		View retweetView;
		int retweetIndex;

		TextView time;
		View verified;
		private View overflow;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder holder = new ViewHolder();
		View view = LayoutInflater.from(context).inflate(R.layout.tweet_row, null);
		holder.nick = (TextView) view.findViewById(R.id.nick);
		// 如果是某个主页时间线
		if (mScreenName == null) {
			holder.nickIndex = cursor.getColumnIndex(User.screen_name);
			holder.avatar = (ImageView) view.findViewById(R.id.avatar);
			holder.avatarIndex = cursor.getColumnIndex(User.profile_image_url);
		}
		// 微博相关
		holder.text = (TweetTextView) view.findViewById(R.id.text);
		holder.textIndex = cursor.getColumnIndex(Status.columnText);
		holder.create_atIndex = cursor.getColumnIndex(Status.created_at);
		holder.replyCount = (TextView) view.findViewById(R.id.reply_count);
		holder.replyCountIndex = cursor.getColumnIndex(Status.comments_count);
		holder.reteetCount = (TextView) view.findViewById(R.id.reteet_count);
		holder.reteetCountIndex = cursor.getColumnIndex(Status.reposts_count);
		holder.likeCount = (TextView) view.findViewById(R.id.like_count);
		holder.likeCountIndex = cursor.getColumnIndex(Status.attitudes_count);
		holder.source = (TextView) view.findViewById(R.id.source);
		holder.sourceIndex = cursor.getColumnIndex(Status.source);
		holder.thumbsIndex = cursor.getColumnIndex(Status.thumbnail_pic);
		holder.thumbs = (ImageView) view.findViewById(R.id.thumbs);
		holder.remarkIndex = cursor.getColumnIndex(User.remark);
		holder.reply = (ImageView) view.findViewById(R.id.action_reply);
		holder.retweet = (ImageView) view.findViewById(R.id.action_reteet);
		if (mThumbsOption.equals(mContext.getString(R.string.thumb_medium))) {
			// 中型缩略图
			holder.thumbsIndex = cursor.getColumnIndex(Status.bmiddle_pic);
			holder.originalPicIndex = cursor.getColumnIndex(Status.original_pic);
			mSmall = false;
		} else if (mThumbsOption.equals(mContext.getString(R.string.thumb_small))) {
			// 小型缩略图
			holder.thumbsIndex = cursor.getColumnIndex(Status.thumbnail_pic);
			holder.originalPicIndex = cursor.getColumnIndex(Status.original_pic);
			mSmall = true;
		} else {
			// ta不要缩略图
			holder.thumbsIndex = -1;
		}
		// 转发
		holder.retweetView = view.findViewById(R.id.retweet);
		holder.retweetIndex = cursor.getColumnIndex(Status.retweeted_status);
		// time
		holder.time = (TextView) view.findViewById(R.id.time);
		holder.verified = view.findViewById(R.id.verified);
		holder.overflow = view.findViewById(R.id.tweet_overflow);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
		// 不是某个用户的时间线
		if (mScreenName == null) {
			holder.avatar.setVisibility(View.VISIBLE);
			Picasso.with(context)
					.load(cursor.getString(holder.avatarIndex))
					.placeholder(R.drawable.error)
					.error(R.drawable.error)
					.into(holder.avatar);
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
			if (CatnutUtils.getBoolean(cursor, User.verified)) {
				holder.verified.setVisibility(View.VISIBLE);
			} else {
				holder.verified.setVisibility(View.GONE);
			}
		} else {
			holder.nick.setText(mScreenName);
		}
		// 微博相关
		CatnutUtils.setTypeface(holder.text, mCustomizedFont);
		holder.text.setLineSpacing(0, mCustomizedLineSpacing);
		holder.reply.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
		holder.time.setText(DateTime.getRelativeTimeString(create_at));
		int replyCount = cursor.getInt(holder.replyCountIndex);
		holder.replyCount.setText(CatnutUtils.approximate(replyCount));
		int retweetCount = cursor.getInt(holder.reteetCountIndex);
		holder.reteetCount.setText(CatnutUtils.approximate(retweetCount));
		int favoriteCount = cursor.getInt(holder.likeCountIndex);
		holder.likeCount.setText(CatnutUtils.approximate(favoriteCount));
		String source = cursor.getString(holder.sourceIndex);
		// remove html tags, maybe we should do this after we load the data from cloud...
		holder.source.setText(Html.fromHtml(source).toString());
		// 文字处理
		CatnutUtils.vividTweet(holder.text, mImageSpan);
		// 缩略图，用户偏好
		if (holder.thumbsIndex == -1) {
			holder.thumbs.setVisibility(View.GONE);
		} else {
			final String thumbsUri = cursor.getString(holder.thumbsIndex);
			final String originUri = cursor.getString(holder.originalPicIndex);
			if (!TextUtils.isEmpty(thumbsUri)) {
				if (!mSmall) {
					Picasso.with(context)
							.load(thumbsUri)
							.resizeDimen(R.dimen.thumb_width, R.dimen.thumb_height) // todo: remove hard code
							.centerCrop()
							.into(holder.thumbs);
				} else {
					Picasso.with(context)
							.load(thumbsUri)
							.into(holder.thumbs);
				}
				holder.thumbs.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = SingleFragmentActivity.getIntent(context, SingleFragmentActivity.PHOTO_VIEWER);
						intent.putExtra(Constants.PIC, originUri);
						mContext.startActivity(intent);
					}
				});
				holder.thumbs.setVisibility(View.VISIBLE);
			} else {
				holder.thumbs.setVisibility(View.GONE);
			}
		}
		// 处理转发
		retweet(cursor.getString(holder.retweetIndex), holder);
		// others
		Bean bean = new Bean();
		bean.id = id;
		bean.favorited = CatnutUtils.getBoolean(cursor, Status.favorited);
		holder.overflow.setTag(bean); // inject data
		holder.overflow.setOnClickListener(this);
	}

	private void retweet(final String jsonString, ViewHolder holder) {
		if (!TextUtils.isEmpty(jsonString)) {
			final JSONObject json;
			try {
				json = new JSONObject(jsonString);
			} catch (JSONException e) {
				Log.e(TAG, "retweet text convert json error!", e);
				holder.retweetView.setVisibility(View.GONE);
				return;
			}
			holder.retweetView.setVisibility(View.VISIBLE);
			JSONObject user = json.optJSONObject(User.SINGLE);
			if (user == null) { // 有可能trim_user了，这种情况一般是查看某个用户的时间线的或者那条微博已被删除
				CatnutUtils.setText(holder.retweetView, R.id.retweet_nick, mContext.getString(R.string.unknown_user));
			} else {
				String str = user.optString(User.remark);
				if (TextUtils.isEmpty(str)) {
					str = user.optString(User.screen_name);
				}
				CatnutUtils.setText(holder.retweetView, R.id.retweet_nick, mContext.getString(R.string.mention_text, str));
				holder.retweetView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(mContext, TweetActivity.class);
						intent.putExtra(Constants.JSON, jsonString);
						mContext.startActivity(intent);
					}
				});
			}
			TweetTextView text = (TweetTextView) holder.retweetView.findViewById(R.id.retweet_text);
			CatnutUtils.setText(holder.retweetView, R.id.retweet_create_at,
					DateTime.getRelativeTimeString(json.optString(Status.created_at)));
			text.setText(json.optString(Status.text));
			CatnutUtils.vividTweet(text, mImageSpan);
			CatnutUtils.setTypeface(text, mCustomizedFont);
			text.setLineSpacing(0, mCustomizedLineSpacing);
		} else {
			holder.retweetView.setVisibility(View.GONE);
		}
	}

	private static class Bean {
		long id;
		boolean favorited;
	}
}
