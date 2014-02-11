/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.processor;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import tingting.chen.metadata.Status;
import tingting.chen.metadata.User;
import tingting.chen.tingting.TingtingProcessor;
import tingting.chen.tingting.TingtingProvider;

/**
 * 用户数据处理器，关联该用户最新的一条微博
 *
 * @author longkai
 */
public class UserProcessor {

	/**
	 * 将我的好友持久化到本地
	 */
	public static class MyFriendsProcessor implements TingtingProcessor<JSONObject> {

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

			context.getContentResolver().bulkInsert(TingtingProvider.parse(User.MULTIPLE), users);
			if (hasTweet) {
				context.getContentResolver().bulkInsert(TingtingProvider.parse(Status.MULTIPLE), tweets);
			}
		}
	}

	/**
	 * 持久化单个用户的信息
	 */
	public static class UserProfileProcessor implements TingtingProcessor<JSONObject> {

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			ContentValues user = User.METADATA.convert(data);
			context.getContentResolver().insert(TingtingProvider.parse(User.MULTIPLE), user);
		}
	}
}
