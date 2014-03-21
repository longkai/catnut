/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.fragment.PluginsPrefFragment;
import org.catnut.plugin.fantasy.FantasyFallFragment;
import org.catnut.plugin.zhihu.PagerItemFragment;
import org.catnut.plugin.zhihu.ZhihuItemFragment;
import org.catnut.plugin.zhihu.ZhihuItemsFragment;
import org.catnut.util.Constants;

import java.util.Collections;
import java.util.List;

/**
 * 插件界面
 *
 * @author longkai
 */
public class PluginsActivity extends Activity implements
		FragmentManager.OnBackStackChangedListener, ActionBar.TabListener {

	public static final String PLUGINS = "plugins";

	public static final int ACTION_ZHIHU_PAGER = 0;
	public static final int ACTION_ZHIHU_ITEM = 1;

	private EasyTracker mTracker;

	private Handler mHandler = new Handler();

	private ViewPager mViewPager;

	private List<Integer> mIds;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar bar = getActionBar();
		bar.setDisplayShowHomeEnabled(false);
		bar.setTitle(R.string.plugins);
		getFragmentManager().addOnBackStackChangedListener(this);
		int which = getIntent().getIntExtra(Constants.ACTION, -1);
		switch (which) {
			case ACTION_ZHIHU_ITEM:
			case ACTION_ZHIHU_PAGER:
				if (savedInstanceState == null) {
					Fragment fragment =
							which == ACTION_ZHIHU_ITEM
							? ZhihuItemFragment.getFragment(getIntent().getLongExtra(Constants.ID, 0L))
							: PagerItemFragment.getFragment(getIntent().getLongExtra(Constants.ID, 0L),
									getIntent().getLongExtra(PagerItemFragment.ORDER_ID, 0L));
					getFragmentManager().beginTransaction()
							.replace(android.R.id.content, fragment)
							.commit();
				}
				break;
			default:
				injectPager(bar, savedInstanceState);
				break;
		}
		if (CatnutApp.getTingtingApp().getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
		}
	}

	private void injectPager(ActionBar bar, Bundle savedInstanceState) {
		// not show the bar, but not hide, u known what i mean?
		bar.setDisplayHomeAsUpEnabled(false);
		bar.setDisplayShowHomeEnabled(false);
		bar.setDisplayShowTitleEnabled(false);
		setContentView(R.layout.pager);

		mIds = getIntent().getIntegerArrayListExtra(PLUGINS);
		if (savedInstanceState == null) {
			mIds.add(0); // add an alt one...
		}
		Collections.shuffle(mIds); // shuffle it :-)
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setPageMargin(10);

		mViewPager.setPageMarginDrawable(new ColorDrawable(getResources().getColor(R.color.tab_selected)));
		mViewPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
			@Override
			public Fragment getItem(int position) {
				Fragment fragment;
				switch (mIds.get(position)) {
					case PluginsPrefFragment.ZHIHU:
						fragment = ZhihuItemsFragment.getFragment();
						break;
					case PluginsPrefFragment.FANTASY:
						fragment = FantasyFallFragment.getFragment();
						break;
					default:
						fragment = new PlaceHolderFragment();
						break;
				}
				return fragment;
			}

			@Override
			public int getCount() {
				return mIds.size();
			}

			@Override
			public CharSequence getPageTitle(int position) {
				switch (mIds.get(position)) {
					case PluginsPrefFragment.ZHIHU:
						return getString(R.string.read_zhihu);
					case PluginsPrefFragment.FANTASY:
						return getString(R.string.fantasy);
					default:
						return "more plugins...";
				}
			}
		});
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				getActionBar().setSelectedNavigationItem(position);
			}
		});
		for (int i = 0; i < mViewPager.getAdapter().getCount(); i++) {
			bar.addTab(bar.newTab().setText(mViewPager.getAdapter().getPageTitle(i)).setTabListener(this));
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mTracker != null) {
			mTracker.activityStart(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mTracker != null) {
			mTracker.activityStop(this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (getIntent().getIntExtra(Constants.ACTION, -1) != -1) {
					int count = getFragmentManager().getBackStackEntryCount();
					if (count == 0) {
						onBackPressed();
					} else {
						getFragmentManager().popBackStack();
					}
				} else {
					onBackPressed();
				}
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackStackChanged() {
		invalidateOptionsMenu();
	}

	public void flipCard(Fragment fragment, String tag, boolean backStack) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction
				.setCustomAnimations(
						R.animator.card_flip_right_in, R.animator.card_flip_right_out,
						R.animator.card_flip_left_in, R.animator.card_flip_left_out)
				.replace(android.R.id.content, fragment, tag);
		if (backStack) {
			fragmentTransaction.addToBackStack(null);
		}
		fragmentTransaction.commit();
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				invalidateOptionsMenu();
			}
		});
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
		// no-op
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
		// no-op
	}

	public static class PlaceHolderFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			TextView view = (TextView) inflater.inflate(R.layout.empty_list, container, false);
			view.setTextSize(25);
			view.setText("if interesting and free, there would be one or more coming :-)\n\nfeel free to drop me a line.");
			return view;
		}
	}
}