/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.core.CatnutApp;
import org.catnut.fragment.OAuthFragment;

/**
 * 欢迎界面，可以在这里放一些更新说明，pager，或者进行一些初始化（检测网络状态，是否通过了新浪的授权）等等，
 * 设置好播放的时间后自动跳转到main-ui，或者用户自己触发某个控件跳转
 * <p/>
 * no history in android-manifest!
 *
 * @author longkai
 */
public class HelloActivity extends Activity {

	/** 欢迎界面默认的播放时间 */
	private static final long DEFAULT_SPLASH_TIME_MILLS = 3000L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final CatnutApp app = CatnutApp.getTingtingApp();
		// 根据是否已经授权，切换不同的界面
		if (app.getAccessToken() == null) {
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new OAuthFragment())
				.commit();
		} else {
			startActivity(new Intent(HelloActivity.this, MainActivity.class));
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}
}