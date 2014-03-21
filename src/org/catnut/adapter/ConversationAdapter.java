/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.metadata.Comment;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.ui.ProfileActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 我收到的评论（回话视图）
 *
 * @author longkai
 */
public class ConversationAdapter extends CursorAdapter {

	private TweetImageSpan mTweetImageSpan;

	public ConversationAdapter(Context context) {
		super(context, null, 0);
		mTweetImageSpan = new TweetImageSpan(context);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = LayoutInflater.from(context).inflate(R.layout.comments_2_me, null);
		ViewHolder holder = new ViewHolder();

		holder.myText = (TweetTextView) view.findViewById(R.id.my_text);
		holder.myTextCreateAt = (TextView) view.findViewById(R.id.my_create_at);
		holder.myTextCreateAtIndex = cursor.getColumnIndex(Comment.created_at);

		holder.avatar = (ImageView) view.findViewById(R.id.avatar);
		holder.screenName = (TextView) view.findViewById(R.id.screen_name);
		holder.screenNameIndex = cursor.getColumnIndex(User.screen_name);
		holder.remarkIndex = cursor.getColumnIndex(User.remark);
		holder.hisText = (TweetTextView) view.findViewById(R.id.his_text);
		holder.uidIndex = cursor.getColumnIndex(Comment.uid);
		holder.hisCreateAt = (TextView) view.findViewById(R.id.his_create_at);
		holder.hisCreateAtIndex = cursor.getColumnIndex(Comment.created_at);

		holder.avatarIndex = cursor.getColumnIndex(User.avatar_large);
		holder.hisTextIndex = cursor.getColumnIndex(Comment.columnText);
		holder.statusIndex = cursor.getColumnIndex(Comment.status);
		holder.replyCommentIndex = cursor.getColumnIndex(Comment.reply_comment);

		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		final ViewHolder holder = (ViewHolder) view.getTag();
		Picasso.with(context)
				.load(cursor.getString(holder.avatarIndex))
				.error(R.drawable.error)
				.placeholder(R.drawable.error)
				.into(holder.avatar);

		String replyString = cursor.getString(holder.replyCommentIndex);
		String statusString = cursor.getString(holder.statusIndex);
		if (TextUtils.isEmpty(replyString)) {
			// 表示ta回复我...
			JSONObject status = null;
			try {
				status = new JSONObject(statusString);
			} catch (JSONException e) {
				Toast.makeText(context, R.string.malformed_json, Toast.LENGTH_LONG).show();
			}
			if (status != null) {
				holder.myText.setText(status.optString(Status.text));
				CatnutUtils.vividTweet(holder.myText, mTweetImageSpan);
			}
		} else {
			// 表示ta回复我的评论
			JSONObject reply = null;
			try {
				reply = new JSONObject(replyString);
			} catch (JSONException e) {
				Toast.makeText(context, R.string.malformed_json, Toast.LENGTH_LONG).show();
			}
			if (reply != null) {
				holder.myText.setText(reply.optString(Comment.text));
				CatnutUtils.vividTweet(holder.myText, mTweetImageSpan);
			}
		}

		holder.hisText.setText(cursor.getString(holder.hisTextIndex));
		CatnutUtils.vividTweet(holder.hisText, mTweetImageSpan);
		final String screenName = cursor.getString(holder.screenNameIndex);
		String remark = cursor.getString(holder.remarkIndex);
		holder.screenName.setText(TextUtils.isEmpty(remark) ? screenName : remark);
		holder.hisCreateAt.setText(DateUtils.getRelativeTimeSpanString(
				DateTime.getTimeMills(cursor.getString(holder.hisCreateAtIndex))
		));
		holder.myTextCreateAt.setText(DateUtils.getRelativeTimeSpanString(
				DateTime.getTimeMills(cursor.getString(holder.myTextCreateAtIndex))
		));
		// bind tag for click listener
		final long uid = cursor.getLong(holder.uidIndex);
		holder.avatar.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return CatnutUtils.imageOverlay(v, event);
			}
		});
		holder.avatar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				holder.avatar.getDrawable().clearColorFilter();
				holder.avatar.invalidate();
				Intent intent = new Intent(context, ProfileActivity.class);
				intent.putExtra(Constants.ID, uid);
				intent.putExtra(User.screen_name, screenName);
				context.startActivity(intent);
			}
		});
	}

	private static class ViewHolder {
		TweetTextView myText;
		TextView myTextCreateAt;
		int myTextCreateAtIndex;

		ImageView avatar;
		int avatarIndex;
		TextView screenName;
		int screenNameIndex;
		int remarkIndex;
		int uidIndex;
		TweetTextView hisText;
		TextView hisCreateAt;
		int hisCreateAtIndex;

		int hisTextIndex;
		int statusIndex;
		int replyCommentIndex;
	}
}
