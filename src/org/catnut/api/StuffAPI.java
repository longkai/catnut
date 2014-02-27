/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.util.Manifest;

/**
 * 其它的api
 *
 * @author longkai
 */
public class StuffAPI {

	/**
	 * 将一个或多个长链接转换成短链接
	 * <p/>
	 * 多个url参数需要使用如下方式：url_long=aaa&url_long=bbb
	 *
	 * @param url_long 需要转换的长链接，需要URLencoded，最多不超过20个。
	 * @return api
	 */
	public static CatnutAPI shorten(String[] url_longs) {
		StringBuilder uri = new StringBuilder(CatnutAPI.API_DOMAIN);
		uri.append("/2/short_url/shorten.json")
				.append("?access_token=").append(CatnutApp.getTingtingApp().getAccessToken().access_token);
		for (int i = 0; i < url_longs.length; i++) {
			uri.append("&url_long=").append(url_longs[i]);
		}
		return new CatnutAPI(Request.Method.GET, uri.toString(), false, null);
	}
}
