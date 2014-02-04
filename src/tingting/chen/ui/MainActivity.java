/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.*;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Process;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.ImageButton;
import android.widget.ImageView;
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
public class MainActivity extends Activity implements ActionBar.TabListener {

	private static final String TAG = "MainActivity";

	private TingtingApp mApp;
	private RequestQueue mRequestQueue;

	private ActionBar mActionBar;
	private ViewPager mViewPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager);

		mApp = TingtingApp.getTingtingApp();
		mRequestQueue = mApp.getRequestQueue();

		mActionBar = getActionBar();
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		buildPagerTabs();

		mViewPager.setAdapter(new MainPagerFragmentAdapter());
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mActionBar.setSelectedNavigationItem(position);
			}
		});
	}

	private void buildPagerTabs() {
		int[] icons = new int[]{
			R.drawable.ic_tab_home,
			R.drawable.ic_tab_home,
		};
		for (int icon : icons) {
			ActionBar.Tab tab = mActionBar.newTab()
				.setIcon(icon)
				.setTabListener(this);
			mActionBar.addTab(tab);
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

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
	}

	private class MainPagerFragmentAdapter extends FragmentPagerAdapter {

		public MainPagerFragmentAdapter() {
			super(getFragmentManager());
		}

		@Override
		public Fragment getItem(int position) {
			return new DummyFragment();
		}

		@Override
		public int getCount() {
			return mActionBar.getTabCount();
		}
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