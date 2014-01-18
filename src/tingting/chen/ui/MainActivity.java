/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import tingting.chen.R;
import tingting.chen.TingtingApp;
import tingting.chen.beans.AccessToken;
import tingting.chen.fragments.OAuthFragment;

/**
 * 应用程序主界面。
 * todo 后面需要好好设计界面
 *
 * @author longkai
 * @date 2014-01-18
 */
public class MainActivity extends Activity {

	public static final String TAG = "MainActivity";

	private TingtingApp mApp;

	private AccessToken mAccessToken;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_container);
		mApp = TingtingApp.getTingtingApp();

		if (savedInstanceState == null) {
			Fragment fragment;
			mAccessToken = mApp.getAccessToken();
			// 判断以下是否已经认证，跳转不同的界面
			if (mAccessToken != null) {
				fragment = new Fragment() {
					@Override
					public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
						return inflater.inflate(R.layout.main, container, false);
					}
				};
			} else {
				fragment = new OAuthFragment();
			}
			getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();
		}
	}

}