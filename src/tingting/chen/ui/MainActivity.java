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
import android.widget.TextView;
import com.android.volley.RequestQueue;
import tingting.chen.R;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.fragment.OAuthFragment;

/**
 * 应用程序主界面。
 * todo 后面需要好好设计界面
 *
 * @author longkai
 * @date 2014-01-18
 */
public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	private TingtingApp mApp;
	private RequestQueue mRequestQueue;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = TingtingApp.getTingtingApp();
		mRequestQueue = mApp.getRequestQueue();

		if (savedInstanceState == null) {
			Fragment fragment;
			// 判断以下是否已经认证，跳转不同的界面
			if (mApp.getAccessToken() != null) {
				// todo: add main ui!
				fragment = new DummyFragment();
			} else {
				fragment = new OAuthFragment();
			}
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, fragment)
				.commit();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mRequestQueue.cancelAll(this);
	}

	/**
	 * just for test!
	 */
	public static class DummyFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			TextView tv = new TextView(getActivity());
			tv.setText(R.string.app_name);
			return tv;
		}
	}
}