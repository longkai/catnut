/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.util.CatnutUtils;
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
	 * @param url_longs 需要转换的长链接，需要URLencoded，最多不超过20个。
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

	/**
	 * 获取某个用户的各种消息未读数
	 *
	 * @param uid            需要获取消息未读数的用户UID，必须是当前登录用户
	 * @param unread_message 未读数版本。0：原版未读数，1：新版未读数。默认为0
	 * @return api
	 */
	public static CatnutAPI unread_count(long uid, int unread_message) {
		StringBuilder uri = new StringBuilder(CatnutAPI.API_DOMAIN)
				.append("/2/remind/unread_count.json")
				.append("?uid=").append(uid)
				.append("&unread_message=")
				.append(CatnutUtils.optValue(unread_message, 0));
		return new CatnutAPI(Request.Method.GET, uri.toString(), true, null);
	}
}
