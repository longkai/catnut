/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;

/**
 * 用户接口
 *
 * @author longkai
 */
public class UserAPI {

	private static final String BASE_URI = CatnutAPI.API_DOMAIN + "/2/users/";

	/**
	 * 根据用户ID获取用户信息
	 *
	 * @param uid 需要查询的用户ID
	 * @return api
	 */
	public static CatnutAPI profile(long uid) {
		return new CatnutAPI(
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
	public static CatnutAPI profile(String screen_name) {
		return new CatnutAPI(
			Request.Method.GET,
			BASE_URI + "show.json?screen_name=" + screen_name,
			true,
			null
		);
	}
}
