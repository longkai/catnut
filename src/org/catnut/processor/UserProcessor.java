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
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.core.CatnutProvider;
import org.catnut.util.Constants;

/**
 * 用户数据处理器，关联该用户最新的一条微博
 *
 * @author longkai
 */
public class UserProcessor {

	/**
	 * 将我的好友持久化到本地
	 */
	public static class UsersProcessor implements CatnutProcessor<JSONObject> {

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			JSONArray jsonArray = data.optJSONArray(User.MULTIPLE);
			ContentValues[] users = new ContentValues[jsonArray.length()];
			// 可能会包含有这条用户最新的微博存在
			ContentValues[] tweets = null;
			boolean hasTweet = jsonArray.optJSONObject(0).has(Status.SINGLE);
			if (hasTweet) {
				tweets = new ContentValues[users.length];
			}

			for (int i = 0; i < users.length; i++) {
				JSONObject user = jsonArray.optJSONObject(i);
				users[i] = User.METADATA.convert(user);
				if (hasTweet) {
					tweets[i] = Status.METADATA.convert(user.optJSONObject(Status.SINGLE));
				}
			}

			context.getContentResolver().bulkInsert(CatnutProvider.parse(User.MULTIPLE), users);
			if (hasTweet) {
				context.getContentResolver().bulkInsert(CatnutProvider.parse(Status.MULTIPLE), tweets);
			}
		}
	}

	/**
	 * 持久化单个用户的信息
	 */
	public static class UserProfileProcessor implements CatnutProcessor<JSONObject> {

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			ContentValues user = User.METADATA.convert(data);
			// 如果包含了用户最新的一条微博
			if (data.has(Status.SINGLE)) {
				JSONObject json = data.getJSONObject(Status.SINGLE);
				ContentValues tweet = Status.METADATA.convert(json);
				// 注意，新浪此时返回的内联微博没有uid这项！
				tweet.put(Status.uid, data.optLong(Constants.ID));
				context.getContentResolver().insert(CatnutProvider.parse(Status.MULTIPLE), tweet);
			}
			context.getContentResolver().insert(CatnutProvider.parse(User.MULTIPLE), user);
		}
	}
}
