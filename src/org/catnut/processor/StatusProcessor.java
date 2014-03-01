/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.processor;

import android.content.ContentValues;
import android.content.Context;
import org.catnut.core.CatnutProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.fragment.FavoriteFragment;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 微博相关处理器
 *
 * @author longkai
 */
public class StatusProcessor {

	/**
	 * 持久化时间线
	 *
	 * @author longkai
	 */
	public static class TimelineProcessor implements CatnutProcessor<JSONObject> {

		private int type = Status.HOME;

		public TimelineProcessor() {
		}

		public TimelineProcessor(int type) {
			this.type = type;
		}

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
				status.put(Status.TYPE, type); // 标记为啥类型的微博
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
	 * 处理单条评论
	 *
	 * @author longkai
	 */
	public static class CommentTweetProcessor implements CatnutProcessor<JSONObject> {

		private long id;

		public CommentTweetProcessor(long id) {
			this.id = id;
		}

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			ContentValues[] statues = new ContentValues[2];
			statues[0] = Status.METADATA.convert(data);
			statues[0].put(Status.TYPE, Status.COMMENT);
			statues[0].put(Status.TO_WHICH_TWEET, id);

			statues[1] = Status.METADATA.convert(data.optJSONObject(Status.SINGLE));
			context.getContentResolver().bulkInsert(CatnutProvider.parse(Status.MULTIPLE), statues);
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
			ContentValues user = User.METADATA.convert(jsonObject.optJSONObject(User.SINGLE));
			context.getContentResolver().insert(CatnutProvider.parse(User.MULTIPLE), user);
			context.getContentResolver().insert(CatnutProvider.parse(Status.MULTIPLE), status);
		}
	}

	/**
	 * 持久化收藏列表
	 *
	 * @author longkai
	 */
	public static class FavoriteTweetsProcessor implements CatnutProcessor<JSONObject> {

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			int delete = 0;
			JSONArray array = data.optJSONArray("favorites");
			ContentValues[] favorites = new ContentValues[array.length()];
			ContentValues[] users = new ContentValues[favorites.length];
			JSONObject json;
			for (int i = 0; i < favorites.length; i++) {
				// 解析微博，这里共用了变量名
				json = array.optJSONObject(i).optJSONObject(Status.SINGLE);
				if (json.optInt("deleted") == 1) {
					// 可能微博已经被删除了
					delete++;
					continue;
				}
				ContentValues values = Status.METADATA.convert(json);
				values.put(Status.favorited, 1);
				favorites[i] = values;
				// 解析用户
				users[i] = User.METADATA.convert(json.optJSONObject(User.SINGLE));
			}
			// persist
			context.getContentResolver().bulkInsert(CatnutProvider.parse(User.MULTIPLE), users);
			context.getContentResolver().bulkInsert(CatnutProvider.parse(Status.MULTIPLE), favorites);
			data.put(FavoriteFragment.TAG, delete);
		}
	}

	/**
	 * 普通的一条微博解析，需要提供一个微博类型
	 *
	 * @author longkai
	 */
	public static class SingleTweetProcessor implements CatnutProcessor<JSONObject> {

		// 微博类型
		private int type;

		public SingleTweetProcessor(int type) {
			this.type = type;
		}

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			ContentValues status = Status.METADATA.convert(data);
			status.put(Status.TYPE, type); // 添加类型
			ContentValues user = User.METADATA.convert(data.optJSONObject(User.SINGLE));
			context.getContentResolver().insert(CatnutProvider.parse(Status.MULTIPLE), status);
			context.getContentResolver().insert(CatnutProvider.parse(User.MULTIPLE), user);
		}
	}
}