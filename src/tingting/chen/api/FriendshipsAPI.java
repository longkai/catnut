/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.api;

import com.android.volley.Request;
import tingting.chen.tingting.TingtingAPI;
import tingting.chen.util.TingtingUtils;

/**
 * 关系接口
 *
 * @author longkai
 */
public class FriendshipsAPI {

	private static final String BASE_URI = TingtingAPI.API_DOMAIN + "/2/friendships/";

	/**
	 * 获取用户的关注列表
	 *
	 * @param uid         需要查询的用户UID
	 * @param count       单页返回的记录条数，默认为50，最大不超过200
	 * @param cursor      返回结果的游标，下一页用返回值里的next_cursor，上一页用previous_cursor，默认为0。
	 * @param trim_status 返回值中user字段中的status字段开关，0：返回完整status字段、1：status字段仅返回status_id，默认为1
	 * @return 用户的关注列表api
	 */
	public static TingtingAPI friends(long uid, int count, int cursor, int trim_status) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("friends.json")
			.append("?uid=").append(uid)
			.append("&count=").append(TingtingUtils.optValue(count, 50))
			.append("&cursor=").append(TingtingUtils.optValue(cursor, 0))
			.append("&trim_status=").append(TingtingUtils.optValue(trim_status, 1));
		return new TingtingAPI(Request.Method.GET, uri.toString(), true, null);
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
	public static TingtingAPI followers(long uid, int count, int cursor, int trim_status) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("followers.json")
			.append("?uid=").append(uid)
			.append("&count=").append(TingtingUtils.optValue(count, 50))
			.append("&cursor=").append(TingtingUtils.optValue(cursor, 0))
			.append("&trim_status=").append(TingtingUtils.optValue(trim_status, 1));
		return new TingtingAPI(Request.Method.GET, uri.toString(), true, null);
	}
}
