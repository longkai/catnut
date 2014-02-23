/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;

/**
 * 收藏接口
 *
 * @author longkai
 */
public class FavoritesAPI {

	private static final String BASE_URI = CatnutAPI.API_DOMAIN + "/2/favorites/";

	/**
	 * 添加一条微博到收藏里
	 *
	 * @param id 要收藏的微博ID
	 * @return api
	 */
	public static CatnutAPI create(long id) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("create.json").append("?id=").append(id);
		return new CatnutAPI(Request.Method.POST, uri.toString(), true, null);
	}

	/**
	 * 要取消收藏的微博ID
	 *
	 * @param id 要收藏的微博ID
	 * @return api
	 */
	public static CatnutAPI destroy(long id) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("destroy.json").append("?id=").append(id);
		return new CatnutAPI(Request.Method.POST, uri.toString(), true, null);
	}
}
