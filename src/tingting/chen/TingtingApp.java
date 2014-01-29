/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import tingting.chen.metadata.AccessToken;

import static tingting.chen.metadata.AccessToken.*;

//import static tingting.chen.util.Constants.*;

/**
 * 应用程序对象
 *
 * @author longkai
 * @date 2014-01-18
 */
public class TingtingApp extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String TAG = "TingtingApp";

	/** singleton */
	private static TingtingApp sApp;

	private RequestQueue mRequestQueue;
	private SharedPreferences mPreferences;
	private AccessToken mAccessToken;

	@Override
	public void onCreate() {
		super.onCreate();
		sApp = this;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mRequestQueue = Volley.newRequestQueue(this);

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
	 * 直接以静态方法获取应用程序对象
	 *
	 * @return TingtingApp
	 */
	public static TingtingApp getTingtingApp() {
		return sApp;
	}
}
