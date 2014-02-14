/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.core;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import org.catnut.metadata.AccessToken;
import org.catnut.util.BitmapLruCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.catnut.metadata.AccessToken.*;

/**
 * 应用程序对象
 *
 * @author longkai
 */
public class CatnutApp extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String TAG = "CatnutApp";

	/** singleton */
	private static CatnutApp sApp;
	/** http request header for weibo' s access token */
	private static Map<String, String> sAuthHeaders;

	private RequestQueue mRequestQueue;
	private ImageLoader mImageLoader;
	private SharedPreferences mPreferences;
	private AccessToken mAccessToken;

	@Override
	public void onCreate() {
		super.onCreate();
		sApp = this;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		mRequestQueue = Volley.newRequestQueue(this);
		mImageLoader = new ImageLoader(mRequestQueue, new BitmapLruCache(1000)); // 1000个缓存条目
		checkAccessToken();
	}

	/** 检查用户是否已经成功授权，并且授权是否过期 */
	private void checkAccessToken() {
		if (mAccessToken == null) {
			mAccessToken = getAccessToken();
		}

		if (mAccessToken != null) {
			long now = System.currentTimeMillis();
			if (now > mAccessToken.expires_in) {
				// 保存用户的uid信息意义不大，因为只是清掉了uid，他所保存的其它信息依然存在
				Log.d(TAG, "用户授权已过期，清除授权信息...");
				invalidateAccessToken();
			} else {
				Log.d(TAG, "授权将在" + DateUtils.getRelativeTimeSpanString(mAccessToken.expires_in) + "过期！");
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "shared prefs " + key + " has got changed!");
	}

	public SharedPreferences getPreferences() {
		return mPreferences;
	}

	/**
	 * 保存授权用户的授权信息
	 *
	 * @param json 包含access token的json对象
	 */
	public void saveAccessToken(JSONObject json) {
		Log.d(TAG, "save access token...");
		mPreferences.edit()
			.putLong(UID, json.optLong(UID))
			.putString(ACCESS_TOKEN, json.optString(ACCESS_TOKEN))
			.putLong(EXPIRES_IN, json.optLong(EXPIRES_IN) * 1000 + System.currentTimeMillis())
			.commit();
	}

	/**
	 * 注销用户
	 */
	public void invalidateAccessToken() {
		Log.d(TAG, "invalidate access token...");
		mPreferences.edit()
			.remove(UID)
			.remove(EXPIRES_IN)
			.remove(ACCESS_TOKEN)
			.commit();
		mAccessToken = null;
		if (sAuthHeaders != null) {
			sAuthHeaders.clear();
		}
		sAuthHeaders = null;
	}

	/**
	 * 获取已经成功授权用户的信息
	 *
	 * @return AccessToken
	 */
	public AccessToken getAccessToken() {
		if (mAccessToken == null) {
			long uid = mPreferences.getLong(UID, 0L);
			long expiresIn = mPreferences.getLong(EXPIRES_IN, 0L);
			String accessToken = mPreferences.getString(ACCESS_TOKEN, null);
			// 当且仅当三项都完备的时候我们才会初始化授权信息对象！
			if (uid != 0L && expiresIn != 0L && accessToken != null) {
				mAccessToken = new AccessToken(uid, expiresIn, accessToken);
			}
		}
		return mAccessToken;
	}

	/**
	 * 获取异步http请求队列对象
	 *
	 * @return RequestQueue
	 */
	public RequestQueue getRequestQueue() {
		return mRequestQueue;
	}

	/**
	 * 获取异步图片loader
	 *
	 * @return {@link com.android.volley.toolbox.ImageLoader}
	 */
	public ImageLoader getImageLoader() {
		return mImageLoader;
	}

	/**
	 * 直接以静态方法获取应用程序对象
	 *
	 * @return CatnutApp
	 */
	public static CatnutApp getTingtingApp() {
		return sApp;
	}

	/**
	 * 获得在请求头部设置access token的map，避免每次都在uri中append上access token
	 * <p/>
	 * 没有授权则返回空的map
	 *
	 * @return headers with access token
	 */
	public static Map<String, String> getAuthHeaders() {
		// 如果没有授权，返回null
		AccessToken accessToken = sApp.getAccessToken();
		if (accessToken == null) {
			return Collections.emptyMap();
		}

		if (sAuthHeaders == null) {
			sAuthHeaders = new HashMap<String, String>(1);
			sAuthHeaders.put("Authorization", "OAuth2 " + accessToken.access_token);
		}
		return sAuthHeaders;
	}
}
