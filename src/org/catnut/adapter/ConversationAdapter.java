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
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.metadata.Comment;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.util.CatnutUtils;
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

		holder.mime = view.findViewById(R.id.mine);
		holder.myText = (TweetTextView) view.findViewById(R.id.my_text);
		holder.myTextCreateAt = (TextView) view.findViewById(R.id.my_create_at);

		holder.avatar = (ImageView) view.findViewById(R.id.avatar);
		holder.screenName = (TextView) view.findViewById(R.id.screen_name);
		holder.screenNameIndex = cursor.getColumnIndex(User.screen_name);
		holder.hisText = (TweetTextView) view.findViewById(R.id.his_text);
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
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		Picasso.with(context)
				.load(cursor.getString(holder.avatarIndex))
				.error(R.drawable.error)
				.placeholder(R.drawable.error)
				.into(holder.avatar);
		String replyString = cursor.getString(holder.replyCommentIndex);
		if (TextUtils.isEmpty(replyString)) {
			// 表示ta回复我...
			String statusString = cursor.getString(holder.statusIndex);
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
		holder.screenName.setText(cursor.getString(holder.screenNameIndex));
		holder.hisCreateAt.setText(DateTime.getRelativeTimeString(cursor.getString(holder.hisCreateAtIndex)));
	}

	private static class ViewHolder {
		View mime;
		TweetTextView myText;
		TextView myTextCreateAt;

		ImageView avatar;
		int avatarIndex;
		TextView screenName;
		int screenNameIndex;
		TweetTextView hisText;
		TextView hisCreateAt;
		int hisCreateAtIndex;

		int hisTextIndex;
		int statusIndex;
		int replyCommentIndex;
	}
}
