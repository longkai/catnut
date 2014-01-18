/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * 应用程序对象
 *
 * @author longkai
 * @date 2014-01-18
 */
public class TingtingApp extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String TAG = "TingtingApp";

	private static TingtingApp sApp;
	private static RequestQueue sRequestQueue;

	private SharedPreferences mPreferences;

	@Override
	public void onCreate() {
		super.onCreate();
		sApp = this;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		sRequestQueue = Volley.newRequestQueue(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "shared prefs " + key + " has got changed!");
	}

	public SharedPreferences getPreferences() {
		return mPreferences;
	}

	/**
	 * 直接以静态方法获取应用程序对象
	 *
	 * @return TingtingApp
	 */
	public static TingtingApp getTingtingApp() {
		return sApp;
	}

	/**
	 * 获取异步http请求队列对象
	 *
	 * @return RequestQueue
	 */
	public static RequestQueue getRequestQueue() {
		return sRequestQueue;
	}
}
