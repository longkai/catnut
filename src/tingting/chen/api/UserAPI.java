/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.api;

import com.android.volley.Request;
import tingting.chen.tingting.TingtingAPI;

/**
 * 用户接口
 *
 * @author longkai
 */
public class UserAPI {

	private static final String BASE_URI = TingtingAPI.API_DOMAIN + "/2/users/";

	/**
	 * 根据用户ID获取用户信息
	 *
	 * @param uid 需要查询的用户ID
	 * @return api
	 */
	public static TingtingAPI profile(long uid) {
		return new TingtingAPI(
			Request.Method.GET,
			BASE_URI + "show.json?uid=" + uid,
			true,
			null
		);
	}

	/**
	 * 根据用户ID获取用户信息
	 *
	 * @param screen_name 需要查询的用户昵称
	 * @return api
	 */
	public static TingtingAPI profile(String screen_name) {
		return new TingtingAPI(
			Request.Method.GET,
			BASE_URI + "show.json?screen_name=" + screen_name,
			true,
			null
		);
	}
}
