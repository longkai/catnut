/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.metadata;

import android.content.ContentValues;
import android.provider.BaseColumns;
import android.util.Log;
import org.json.JSONObject;
import org.catnut.core.CatnutMetadata;
import org.catnut.util.Constants;

/**
 * 微博用户元数据
 *
 * @author longkai
 */
public final class User implements CatnutMetadata<JSONObject, ContentValues> {

	private static final String TAG = "User";

	public static final String TABLE = "users";
	public static final String SINGLE = "user";
	public static final String MULTIPLE = "users";

	public static final String next_cursor = "next_cursor";
	public static final String total_number = "total_number";

	// singleton
	public static final User METADATA = new User();

	private User() {
	}

	/** 昵称 */
	public static final String screen_name = "screen_name";
	/** 友好显示名称 */
	public static final String name = "name";
	/** 所在省级ID int */
	public static final String province = "province";
	/** 所在省级ID int */
	public static final String city = "city";
	/** 所在地 */
	public static final String location = "location";
	/** 个人描述 */
	public static final String description = "description";
	/** 博客地址 */
	public static final String url = "url";
	/** 头像地址（中图），50×50像素 */
	public static final String profile_image_url = "profile_image_url";
	/** 主页封面图片（桌面） */
	public static final String cover_image = "cover_image";
	/** 主页封面图片（手机） */
	public static final String cover_image_phone = "cover_image_phone";
	/** 微博统一URL地址 */
	public static final String profile_url = "profile_url";
	/** 个性化域名 */
	public static final String domain = "domain";
	/** 微号 */
	public static final String weihao = "weihao";
	/** 性别，m：男、f：女、n：未知 */
	public static final String gender = "gender";
	/** 粉丝数 int */
	public static final String followers_count = "followers_count";
	/** 关注数 int */
	public static final String friends_count = "friends_count";
	/** 微博数 int */
	public static final String statuses_count = "statuses_count";
	/** 收藏数 int */
	public static final String favourites_count = "favourites_count";
	/** 用户创建（注册）时间 */
	public static final String created_at = "created_at";
	/** 暂未支持 boolean */
	public static final String following = "following";
	/** 是否允许所有人给我发私信 boolean */
	public static final String allow_all_act_msg = "allow_all_act_msg";
	/** 是否允许标识用户的地理位置 boolean */
	public static final String geo_enabled = "geo_enabled";
	/** 是否是微博认证用户，即加V用户 boolean */
	public static final String verified = "verified";
	/** 暂未支持 int */
	public static final String verified_type = "verified_type";
	/** 用户备注信息，只有在查询用户关系时才返回此字段 */
	public static final String remark = "remark";
	/** int */
//	public static final String ptype = "ptype";
	/** 是否允许所有人对我的微博进行评论 boolean */
	public static final String allow_all_comment = "allow_all_comment";
	/** 头像地址（大图），180×180像素 */
	public static final String avatar_large = "avatar_large";
	/** 头像地址（高清），高清头像原图 */
	public static final String avatar_hd = "avatar_hd";
	/** 认证原因 */
	public static final String verified_reason = "verified_reason";
	/** 是否关注当前登录用户 boolean */
	public static final String follow_me = "follow_me";
	/** 在线状态，0：不在线、1：在线 int */
	public static final String online_status = "online_status";
	/** 互粉数 int */
	public static final String bi_followers_count = "bi_followers_count";
	/** 当前的语言版本，zh-cn：简体中文，zh-tw：繁体中文，en：英语 */
	public static final String lang = "lang";
	/** int */
//	public static final String star = "star";
	/** int */
//	public static final String mbtype = "mbtype";
	/** int */
//	public static final String mbrank = "mbrank";
	/** int */
//	public static final String block_word = "block_word";
	/** 关联最新的一条微博 long */
	public static final String status_id = "status_id";

	@Override
	public String ddl() {
		Log.i(TAG, "create table [users]...");

		StringBuilder ddl = new StringBuilder("CREATE TABLE ");
		ddl.append(TABLE).append("(")
				.append(BaseColumns._ID).append(" int primary key,")
				.append(screen_name).append(" text,")
				.append(name).append(" text,")
				.append(province).append(" int,")
				.append(city).append(" int,")
				.append(location).append(" text,")
				.append(description).append(" text,")
				.append(url).append(" text,")
				.append(profile_image_url).append(" text,")
				.append(cover_image).append(" text,")
				.append(cover_image_phone).append(" text,")
				.append(profile_url).append(" text,")
				.append(domain).append(" text,")
				.append(weihao).append(" text,")
				.append(gender).append(" text,")
				.append(followers_count).append(" int,")
				.append(friends_count).append(" int,")
				.append(statuses_count).append(" int,")
				.append(favourites_count).append(" int,")
				.append(created_at).append(" text,")
				.append(following).append(" int,")
				.append(allow_all_act_msg).append(" int,")
				.append(geo_enabled).append(" int,")
				.append(verified).append(" int,")
				.append(verified_type).append(" int,")
				.append(remark).append(" int,")
//				.append(ptype).append(" int,")
				.append(allow_all_comment).append(" int,")
				.append(avatar_large).append(" text,")
				.append(avatar_hd).append(" text,")
				.append(verified_reason).append(" text,")
				.append(follow_me).append(" int,")
				.append(online_status).append(" int,")
				.append(bi_followers_count).append(" int,")
				.append(lang).append(" text"); // 注意最后一个字段！
//				.append(star).append(" int,")
//				.append(mbtype).append(" int,")
//				.append(mbrank).append(" int,")
//				.append(block_word).append(" int");
		return ddl.append(")").toString();
	}

	@Override
	public ContentValues convert(JSONObject json) {
		ContentValues user = new ContentValues();
		user.put(BaseColumns._ID, json.optLong(Constants.ID));
		user.put(screen_name, json.optString(screen_name));
		user.put(name, json.optString(name));
		user.put(province, json.optInt(province));
		user.put(city, json.optInt(city));
		user.put(location, json.optString(location));
		user.put(description, json.optString(description));
		user.put(url, json.optString(url));
		user.put(profile_image_url, json.optString(profile_image_url));
		user.put(cover_image, json.optString(cover_image));
		user.put(cover_image_phone, json.optString(cover_image_phone));
		user.put(profile_url, json.optString(profile_url));
		user.put(domain, json.optString(domain));
		user.put(weihao, json.optString(weihao));
		user.put(gender, json.optString(gender));
		user.put(followers_count, json.optInt(followers_count));
		user.put(friends_count, json.optInt(friends_count));
		user.put(statuses_count, json.optInt(statuses_count));
		user.put(favourites_count, json.optInt(favourites_count));
		user.put(created_at, json.optString(created_at));
		user.put(following, json.optBoolean(following));
		user.put(allow_all_act_msg, json.optBoolean(allow_all_act_msg));
		user.put(geo_enabled, json.optBoolean(geo_enabled));
		user.put(verified, json.optBoolean(verified));
		user.put(verified_type, json.optInt(verified_type));
		user.put(remark, json.optString(remark));
//		user.put(ptype, json.optInt(ptype));
		user.put(allow_all_comment, json.optBoolean(allow_all_comment));
		user.put(avatar_large, json.optString(avatar_large));
		user.put(avatar_hd, json.optString(avatar_hd));
		user.put(verified_reason, json.optString(verified_reason));
		user.put(follow_me, json.optBoolean(follow_me));
		user.put(online_status, json.optInt(online_status));
		user.put(bi_followers_count, json.optInt(bi_followers_count));
		user.put(lang, json.optString(lang));
//		user.put(star, json.optString(star));
//		user.put(mbtype, json.optInt(mbtype));
//		user.put(mbrank, json.optInt(mbrank));
//		user.put(block_word, json.optInt(block_word));
		// 添加最新微博id
		if (json.has(SINGLE)) {
			user.put(status_id, json.optJSONObject(Status.SINGLE).optLong(Constants.ID));
		}
		return user;
	}
}
