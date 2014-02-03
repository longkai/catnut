/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import com.android.volley.toolbox.ImageLoader;
import tingting.chen.R;
import tingting.chen.fragment.OAuthFragment;
import tingting.chen.tingting.TingtingApp;

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
		final TingtingApp app = TingtingApp.getTingtingApp();
		// 根据是否已经授权，切换不同的界面
		if (app.getAccessToken() == null) {
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new OAuthFragment())
				.commit();
		} else {
			ImageView imageView = new ImageView(this);
			ImageLoader imageLoader = app.getImageLoader();
			// todo 测试图片，过后换个好看的:-)
			imageLoader.get("https://www.google.com.hk/images/srpr/logo11w.png",
				ImageLoader.getImageListener(imageView,
					R.drawable.ic_launcher, R.drawable.ic_launcher));
			setContentView(imageView);

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					startActivity(new Intent(HelloActivity.this, MainActivity.class));
				}
			}, DEFAULT_SPLASH_TIME_MILLS);
		}
	}
}