/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;

/**
 * 抓取照片
 *
 * @author longkai
 */
public class _500pxAPI {

	private static final String KEY = "HocY5wY9GQaa9sdNO9HvagCGuGt34snyMTHckIQJ";
	private static final String DOMAIN = "https://api.500px.com/v1";

	public static CatnutAPI photos(String feature) {
		StringBuilder uri = new StringBuilder(DOMAIN);
		uri.append("/photos")
				.append("?feature=").append(feature)
				.append("&consumer_key=").append(KEY);
		return new CatnutAPI(Request.Method.GET, uri.toString(), false, null);
	}
}
