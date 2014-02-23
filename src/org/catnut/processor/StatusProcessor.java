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
	 * 持久化我的微博。
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
				// 转发微博
				while (json.has(Status.retweeted_status)) {
					json = json.optJSONObject(Status.retweeted_status);
					status.put(Status.TYPE, Status.RETWEET); // 标记为转发微博
					statues.add(status);
					// 没有uid则标识返回用户的全部字段
					if (!json.has(Status.uid) && json.has(User.SINGLE)) { // 有些时候，数据也是不可靠的...
						users.add(userMetadata.convert(json.optJSONObject(User.SINGLE)));
					}
				}
			}
			ContentValues[] _statuses = statues.toArray(new ContentValues[statues.size()]);
			context.getContentResolver().bulkInsert(CatnutProvider.parse(Status.MULTIPLE), _statuses);
			ContentValues[] _users = users.toArray(new ContentValues[users.size()]);
			context.getContentResolver().bulkInsert(CatnutProvider.parse(User.MULTIPLE), _users);
		}
	}
}