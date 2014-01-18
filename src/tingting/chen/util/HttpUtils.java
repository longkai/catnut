/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.util;

import com.google.gson.Gson;

/**
 * http相关工具类
 *
 * @author longkai
 * @date 2014-01-18
 */
public class HttpUtils {

	private static Gson gson;

	/**
	 * 获取一个gson单例对象
	 *
	 * @return singleton gson object
	 */
	public static Gson getGson() {
		if (gson == null) {
			gson = new Gson();
		}
		return gson;
	}
}
