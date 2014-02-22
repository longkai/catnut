/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;
import org.catnut.util.CatnutUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 关系接口
 *
 * @author longkai
 */
public class FriendshipsAPI {

	private static final String BASE_URI = CatnutAPI.API_DOMAIN + "/2/friendships/";

	/**
	 * 获取用户的关注列表
	 *
	 * @param uid         需要查询的用户UID
	 * @param count       单页返回的记录条数，默认为50，最大不超过200
	 * @param cursor      返回结果的游标，下一页用返回值里的next_cursor，上一页用previous_cursor，默认为0。
	 * @param trim_status 返回值中user字段中的status字段开关，0：返回完整status字段、1：status字段仅返回status_id，默认为1
	 * @return 用户的关注列表api
	 */
	public static CatnutAPI friends(long uid, int count, int cursor, int trim_status) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("friends.json")
				.append("?uid=").append(uid)
				.append("&count=").append(CatnutUtils.optValue(count, 50))
				.append("&cursor=").append(CatnutUtils.optValue(cursor, 0))
				.append("&trim_status=").append(CatnutUtils.optValue(trim_status, 1));
		return new CatnutAPI(Request.Method.GET, uri.toString(), true, null);
	}

	/**
	 * 获取用户的关注列表
	 *
	 * @param screen_name 需要查询的用户昵称。
	 * @param count       单页返回的记录条数，默认为50，最大不超过200
	 * @param cursor      返回结果的游标，下一页用返回值里的next_cursor，上一页用previous_cursor，默认为0。
	 * @param trim_status 返回值中user字段中的status字段开关，0：返回完整status字段、1：status字段仅返回status_id，默认为1
	 * @return 用户的关注列表api
	 */
	public static CatnutAPI friends(String screen_name, int count, int cursor, int trim_status) {
		String encode = null;
		try {
			encode = URLEncoder.encode(screen_name, "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("friends.json")
				.append("?screen_name=").append(encode)
				.append("&count=").append(CatnutUtils.optValue(count, 50))
				.append("&cursor=").append(CatnutUtils.optValue(cursor, 0))
				.append("&trim_status=").append(CatnutUtils.optValue(trim_status, 1));
		return new CatnutAPI(Request.Method.GET, uri.toString(), true, null);
	}

	/**
	 * 获取用户的粉丝列表
	 *
	 * @param uid         需要查询的用户UID
	 * @param count       单页返回的记录条数，默认为50，最大不超过200
	 * @param cursor      返回结果的游标，下一页用返回值里的next_cursor，上一页用previous_cursor，默认为0
	 * @param trim_status 返回值中user字段中的status字段开关，0：返回完整status字段、1：status字段仅返回status_id，默认为1
	 * @return api
	 */
	public static CatnutAPI followers(long uid, int count, int cursor, int trim_status) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("followers.json")
				.append("?uid=").append(uid)
				.append("&count=").append(CatnutUtils.optValue(count, 50))
				.append("&cursor=").append(CatnutUtils.optValue(cursor, 0))
				.append("&trim_status=").append(CatnutUtils.optValue(trim_status, 1));
		return new CatnutAPI(Request.Method.GET, uri.toString(), true, null);
	}

	/**
	 * 获取用户的粉丝列表
	 *
	 * @param screen_name 需要查询的用户昵称。
	 * @param count       单页返回的记录条数，默认为50，最大不超过200
	 * @param cursor      返回结果的游标，下一页用返回值里的next_cursor，上一页用previous_cursor，默认为0
	 * @param trim_status 返回值中user字段中的status字段开关，0：返回完整status字段、1：status字段仅返回status_id，默认为1
	 * @return api
	 */
	public static CatnutAPI followers(String screen_name, int count, int cursor, int trim_status) {
		String encode = null;
		try {
			encode = URLEncoder.encode(screen_name, "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("followers.json")
				.append("?screen_name=").append(encode)
				.append("&count=").append(CatnutUtils.optValue(count, 50))
				.append("&cursor=").append(CatnutUtils.optValue(cursor, 0))
				.append("&trim_status=").append(CatnutUtils.optValue(trim_status, 1));
		return new CatnutAPI(Request.Method.GET, uri.toString(), true, null);
	}

	/**
	 * 关注一个用户
	 *
	 * @param screen_name 需要关注的用户昵称
	 * @param rip         开发者上报的操作用户真实IP，形如：211.156.0.1
	 * @return api
	 */
	public static CatnutAPI create(String screen_name, String rip) {
		String encode = null;
		try {
			encode = URLEncoder.encode(screen_name, "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("create.json")
				.append("?screen_name=").append(encode)
				.append("&rip=").append(rip);
		return new CatnutAPI(Request.Method.POST, uri.toString(), true, null);
	}
}
