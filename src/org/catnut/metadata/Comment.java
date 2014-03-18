/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.metadata;

import android.content.ContentValues;
import android.provider.BaseColumns;
import android.util.Log;
import org.catnut.core.CatnutMetadata;
import org.catnut.util.Constants;
import org.json.JSONObject;

/**
 * 我收到的评论（实际上是一个会话）
 *
 * @author longkai
 */
public class Comment implements CatnutMetadata<JSONObject, ContentValues> {

	public static final String TABLE = "comments";
	public static final String SINGLE = "comment";
	public static final String MULTIPLE = "comments";

	public static final String created_at = "created_at";
	public static final String text = "text";
	public static final String columnText = "_text";
	public static final String source = "source";
	public static final String uid = "uid"; // 评论作者的用户信息字段
	public static final String status = "status";
	public static final String reply_comment = "reply_comment"; //评论来源评论，当本评论属于对另一评论的回复时返回此字段

	public static final Comment METADATA = new Comment();

	private Comment() {
	}

	@Override
	public String ddl() {
		Log.d(TABLE, "create table [" + TABLE + "]");

		StringBuilder ddl = new StringBuilder("CREATE TABLE ");
		ddl.append(TABLE).append("(")
				.append(BaseColumns._ID).append(" int primary key,")
				.append(created_at).append(" text,")
				.append(columnText).append(" text,")
				.append(source).append(" text,")
				.append(uid).append(" int,")
				.append(status).append(" text,")
				.append(reply_comment).append(" text")
				.append(")");
		return ddl.toString();
	}

	@Override
	public ContentValues convert(JSONObject data) {
		ContentValues values = new ContentValues();
		values.put(BaseColumns._ID, data.optLong(Constants.ID));
		values.put(created_at, data.optString(created_at));
		values.put(columnText, data.optString(text));
		values.put(source, data.optString(source));
		JSONObject user = data.optJSONObject(User.SINGLE);
		if (user != null) {
			values.put(uid, user.optLong(Constants.ID));
		}
		values.put(status, data.optString(status));
		values.put(reply_comment, data.optString(reply_comment));
		return values;
	}
}
