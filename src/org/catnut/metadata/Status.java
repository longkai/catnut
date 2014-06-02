/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.metadata;

import android.content.ContentValues;
import android.provider.BaseColumns;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.catnut.core.CatnutMetadata;
import org.catnut.util.Constants;

/**
 * 微博元数据，关联作者，转发微博的原微博，
 *
 * @author longkai
 */
public final class Status implements CatnutMetadata<JSONObject, ContentValues> {

	private static final String TAG = "Status";

	/** 标记微博的类型，本地使用 */ // todo: 可能一条微博有多个type...
	public static final String TYPE = "_type";
	/** 评论指向的微博id，本地使用 */
	public static final String TO_WHICH_TWEET = "_to";

	/** 本地使用，标记为主页微博 */
	public static final int HOME = 2;
	/** 本地使用，标记为转发微博 */
	public static final int RETWEET = 3;
	/** 本地使用，标记为公共微博 */
	public static final int PUBLIC = 4;
	/** 本地使用，标记为评论 */
	public static final int COMMENT = 5;
	/** 本地使用，标记为@我的微博 */
	public static final int MENTION = 6;
	/** 本地使用，标记为其它类型的微博 */
	public static final int OTHERS = 1;


	public static final String TABLE = "statuses";
	public static final String SINGLE = "status";
	public static final String MULTIPLE = "statuses";

	public static final String COMMENTS = "comments";
	public static final String FAVORITES = "favorites";
	public static final String total_number = "total_number";

	public static final String MEDIUM_THUMBNAIL = "bmiddle";
	public static final String LARGE_THUMBNAIL = "large";

	// singleton
	public static final Status METADATA = new Status();

	private Status() {
	}

	// 自关联，直接存储json字符串
	public static final String retweeted_status = "retweeted_status";

	/** 创建时间 */
	public static final String created_at = "created_at";
	/** 内容 */
	public static final String text = "text";
	public static final String columnText = "_text"; // keywords...
	/** 来源 */
	public static final String source = "source";
	/** 是否已收藏 boolean */
	public static final String favorited = "favorited";
	/** 是否被截断 boolean */
	public static final String truncated = "truncated";
	/** （暂未支持）回复ID */
//	public static final String in_reply_to_status_id = "in_reply_to_status_id";
	/** （暂未支持）回复人UID */
//	public static final String in_reply_to_user_id = "in_reply_to_user_id";
	/** （暂未支持）回复人昵称 */
//	public static final String in_reply_to_screen_name = "in_reply_to_screen_name";
	/** 微博配图地址，key=thumbnail_pic，多图时返回多图链接，无配图返回"[]" array */
	public static final String pic_urls = "pic_urls";
	/** 缩略图片地址，没有时不返回此字段 */
	public static final String thumbnail_pic = "thumbnail_pic";
	/** 中等尺寸图片地址，没有时不返回此字段 */
	public static final String bmiddle_pic = "bmiddle_pic";
	/** 原始图片地址，没有时不返回此字段 */
	public static final String original_pic = "original_pic";
	/** 被转发的原微博信息字段，当该微博为转发微博时返回 long */
	public static final String retweeted_status_id = "retweeted_status_id";
	// todo: geo
	/** 作者 long */
	public static final String uid = "uid";
	/** 转发数 int */
	public static final String reposts_count = "reposts_count";
	/** 评论数 int */
	public static final String comments_count = "comments_count";
	/** 表态数 int */
	public static final String attitudes_count = "attitudes_count";
	/** 暂未支持 */
	public static final String mlevel = "mlevel";
	// todo: visible
	// 微博的可见性及指定可见分组信息。该object中type取值，0：普通微博，1：私密微博，3：指定分组微博，4：密友微博；list_id为分组的组号
	// "visible": {
	// 	"type": 0,
	//	"list_id": 0
	// }

	@Override
	public String ddl() {
		Log.i(TAG, "create table [statuses]...");

		StringBuilder ddl = new StringBuilder("CREATE TABLE ");
		ddl.append(TABLE).append("(")
				.append(BaseColumns._ID).append(" int primary key,")
				.append(TYPE).append(" int,") // 标记微博的类型
				.append(TO_WHICH_TWEET).append(" int,") // 评论指向的微博id
				.append(created_at).append(" text,")
				.append(columnText).append(" text,")
				.append(source).append(" text,")
				.append(favorited).append(" int,")
				.append(truncated).append(" int,")
				.append(pic_urls).append(" text,")
				.append(thumbnail_pic).append(" text,")
				.append(bmiddle_pic).append(" text,")
				.append(original_pic).append(" text,")
				.append(retweeted_status).append(" text,")
				.append(uid).append(" int,")
				.append(reposts_count).append(" int,")
				.append(comments_count).append(" int,")
				.append(attitudes_count).append(" int");
		return ddl.append(")").toString();
	}

	@Override
	public ContentValues convert(JSONObject json) {
		ContentValues tweet = new ContentValues();
		tweet.put(BaseColumns._ID, json.optLong(Constants.ID));
		tweet.put(created_at, json.optString(created_at));
		// 注意json字段和sql字段的不同！
		tweet.put(columnText, json.optString(text));
		tweet.put(source, json.optString(source));
		tweet.put(favorited, json.optBoolean(favorited));
		tweet.put(truncated, json.optBoolean(truncated));
		// 有些时候，数据来源是不可靠的
		JSONArray thumbs = json.optJSONArray(pic_urls);
		if (thumbs != null) {
			// 直接存储为json字符串
			tweet.put(pic_urls, thumbs.toString());
		}
		tweet.put(thumbnail_pic, json.optString(thumbnail_pic));
		tweet.put(bmiddle_pic, json.optString(bmiddle_pic));
		tweet.put(original_pic, json.optString(original_pic));
		// 检查是否是转发微博
		if (json.has(retweeted_status)) {
			tweet.put(retweeted_status, json.optJSONObject(retweeted_status).toString());
		}
		// 加入作者的外键
		if (json.has(User.SINGLE)) {
			tweet.put(uid, json.optJSONObject(User.SINGLE).optLong(Constants.ID));
		} else if (json.has(uid)) {
			tweet.put(uid, json.optLong(uid));
		}
		tweet.put(reposts_count, json.optInt(reposts_count));
		tweet.put(comments_count, json.optInt(comments_count));
		tweet.put(attitudes_count, json.optInt(attitudes_count));
		return tweet;
	}
}
