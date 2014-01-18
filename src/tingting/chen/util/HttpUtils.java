/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.util;

import android.text.TextUtils;
import android.util.Log;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import tingting.chen.beans.WeiboAPIError;

/**
 * http相关工具类
 *
 * @author longkai
 * @date 2014-01-18
 */
public class HttpUtils {

	public static final String TAG = "HttpUtils";

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

	/**
	 * 从volley中解析错误
	 * @param volleyError
	 * @return WeiboAPIError
	 */
	public static WeiboAPIError fromVolleyError(VolleyError volleyError) {
		// 很可能出现response body为空的情况
		String json;
		WeiboAPIError error;
		try {
			json = new String(volleyError.networkResponse.data);
		} catch (Exception e) {
			Log.wtf(TAG, e);
			// fall back
			error = new WeiboAPIError();
			String msg = volleyError.getLocalizedMessage();
			error.error = TextUtils.isEmpty(msg) ? "Known error!" : msg;
			error.error_code = volleyError.networkResponse.statusCode;
			return error;
		}

		error = gson.fromJson(json, WeiboAPIError.class);
		return error;
	}
}
