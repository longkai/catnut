/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.processor;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.catnut.core.CatnutProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 微博相关处理器
 *
 * @author longkai
 */
public class StatusProcessor {

	/**
	 * 持久化主页时间线
	 *
	 * @author longkai
	 */
	public static class HomeTweetsProcessor implements CatnutProcessor<JSONObject> {

		@Override
		public void asyncProcess(Context context, JSONObject jsonObject) throws Exception {
			JSONArray jsonArray = jsonObject.optJSONArray(Status.MULTIPLE);
			List<ContentValues> statues = new ArrayList<ContentValues>(jsonArray.length());
			List<ContentValues> users = new ArrayList<ContentValues>(jsonArray.length());
			User userMetadata = User.METADATA;
			Status statusMetadata = Status.METADATA;
			JSONObject json;
			ContentValues status;
			for (int i = 0; i < jsonArray.length(); i++) {
				json = jsonArray.optJSONObject(i);
				// 一次持久化微博和作者信息
				status = statusMetadata.convert(json);
				status.put(Status.TYPE, Status.HOME); // 标记为主页微博
				statues.add(status);
				// 如果这条微博包含了原作者信息
				if (json.has(User.SINGLE)) {
					users.add(userMetadata.convert(json.optJSONObject(User.SINGLE)));
				}
			}
			ContentValues[] _statuses = statues.toArray(new ContentValues[statues.size()]);
			context.getContentResolver().bulkInsert(CatnutProvider.parse(Status.MULTIPLE), _statuses);
			ContentValues[] _users = users.toArray(new ContentValues[users.size()]);
			context.getContentResolver().bulkInsert(CatnutProvider.parse(User.MULTIPLE), _users);
		}
	}

	/**
	 * 持久化评论时间线
	 *
	 * @author longkai
	 */
	public static class CommentTweetsProcessor implements CatnutProcessor<JSONObject> {

		/** 改评论指向的微博id */
		private long to;

		public CommentTweetsProcessor(long to) {
			this.to = to;
		}


		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			JSONArray array = data.optJSONArray(Status.COMMENTS);
			ContentValues[] comments = new ContentValues[array.length()];
			ContentValues[] users = new ContentValues[array.length()];
			ContentValues comment;
			JSONObject jsonUser;
			JSONObject jsonStatus;
			for (int i = 0; i < comments.length; i++) {
				// 解析微博
				jsonStatus = array.optJSONObject(i);
				comment = Status.METADATA.convert(jsonStatus);
				comment.put(Status.TYPE, Status.COMMENT); // 标记为评论微博
				comment.put(Status.TO_WHICH_TWEET, to);
				comments[i] = comment;
				// 解析评论作者
				jsonUser = jsonStatus.optJSONObject(User.SINGLE);
				users[i] = User.METADATA.convert(jsonUser);
			}
			// 持久化
			context.getContentResolver().bulkInsert(CatnutProvider.parse(Status.MULTIPLE), comments);
			context.getContentResolver().bulkInsert(CatnutProvider.parse(User.MULTIPLE), users);
		}
	}

	/**
	 * 收藏/取消收藏处理器
	 *
	 * @author longkai
	 */
	public static class FavoriteTweetProcessor implements CatnutProcessor<JSONObject> {

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			JSONObject jsonObject = data.optJSONObject(Status.SINGLE);
			ContentValues status = Status.METADATA.convert(jsonObject);
			// set fav type
			status.put(Status.TYPE, Status.FAVORITE);
			ContentValues user = User.METADATA.convert(jsonObject.optJSONObject(User.SINGLE));
			context.getContentResolver().insert(CatnutProvider.parse(User.MULTIPLE), user);
			context.getContentResolver().insert(CatnutProvider.parse(Status.MULTIPLE), status);
		}
	}
}