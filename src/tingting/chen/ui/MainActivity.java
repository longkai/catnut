/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Process;
import android.view.*;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import tingting.chen.R;
import tingting.chen.tingting.TingtingApp;

/**
 * 应用程序主界面。
 * todo 后面需要好好设计界面
 *
 * @author longkai
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
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new DummyFragment())
				.commit();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mRequestQueue.cancelAll(TAG);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// 登出，kill掉本app的进程，不同于按下back按钮，这个不保证回到上一个back stack
			case R.id.logout:
				new AlertDialog.Builder(this)
					.setMessage(getString(R.string.logout_confirm))
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Process.killProcess(Process.myPid());
						}
					})
					.setNegativeButton(android.R.string.no, null)
					.show();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
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