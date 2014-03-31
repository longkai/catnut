/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import org.catnut.R;
import org.catnut.api._500pxAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.fragment.FantasyFragment;
import org.catnut.metadata.AccessToken;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.plugin.fantasy.Photo;
import org.catnut.support.PageTransformer;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 欢迎界面，可以在这里放一些更新说明，pager，或者进行一些初始化（检测网络状态，是否通过了新浪的授权）等等，
 * 设置好播放的时间后自动跳转到main-ui，或者用户自己触发某个控件跳转
 * <p/>
 *
 * @author longkai
 */
public class HelloActivity extends Activity {

	public static final String TAG = "HelloActivity";

	/** 欢迎界面默认的播放时间 */
//	private static final long DEFAULT_SPLASH_TIME_MILLS = 3000L;

	public static final String ACTION_FROM_GRID = "action_from_grid";

	// only use for auth!
	private EasyTracker mTracker;

	private CatnutApp mApp;
	private SharedPreferences mPreferences;

	private Handler mHandler = new Handler();
	private ConnectivityManager mConnectivityManager;

	private List<Image> mImages;
	private Image mTargetFromGrid;
	private ViewPager mViewPager;
	private FragmentStatePagerAdapter mPagerAdapter;
	private View mAbout;

	private TextView mFantasyDesc;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = CatnutApp.getTingtingApp();
		mPreferences = mApp.getPreferences();
		fetch500px();
		// 根据是否已经授权或者授权过期，切换不同的界面
		AccessToken accessToken = mApp.getAccessToken();
		if (accessToken == null || System.currentTimeMillis() > accessToken.expires_in) {
			mApp.invalidateAccessToken();
			Toast.makeText(this, getString(R.string.not_yet_auth), Toast.LENGTH_SHORT).show();
			startActivity(new Intent(this, NoHistoryActivity.class));
		} else {
			// 检查一次更新，每周一次
			CatnutUtils.checkout(false, this, mPreferences);
			// 根据情况跳转
			if (getIntent().hasExtra(TAG)) {
				init();
			} else if (ACTION_FROM_GRID.equals(getIntent().getAction())) {
				mTargetFromGrid = new Image();
				mTargetFromGrid.url = getIntent().getStringExtra(Photo.image_url);
				mTargetFromGrid.name = getIntent().getStringExtra(Photo.name);
				init();
			} else {
				if (CatnutApp.getBoolean(R.string.pref_enter_home_directly, R.bool.pref_enter_home_directly)) {
					startActivity(new Intent(this, MainActivity.class)
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
									| Intent.FLAG_ACTIVITY_NEW_TASK
									| Intent.FLAG_ACTIVITY_NO_ANIMATION)
					);
				} else {
					init();
				}
			}
		}
	}

	private void init() {
		setContentView(R.layout.about);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOnPageChangeListener(new PagerListener());

		mImages = new ArrayList<Image>();

		mPagerAdapter = new Gallery();
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setPageTransformer(true, new PageTransformer.DepthPageTransformer());
		if (mTargetFromGrid != null) {
			mImages.add(mTargetFromGrid);
			mPagerAdapter.notifyDataSetChanged();
		}

		mAbout = findViewById(R.id.about);
		mFantasyDesc = (TextView) findViewById(R.id.description);
		mFantasyDesc.setMovementMethod(LinkMovementMethod.getInstance());
		ActionBar bar = getActionBar();
		bar.setTitle(R.string.fantasy);
		TextView about = (TextView) findViewById(R.id.about_body);
		TextView version = (TextView) findViewById(R.id.app_version);
		TextView appName = (TextView) findViewById(R.id.app_name);
		TextView weiboApp = (TextView) findViewById(R.id.weibo_app);
		weiboApp.setText(R.string.weibo_app);
		appName.setText(R.string.app_name);
		TextView appDesc = (TextView) findViewById(R.id.app_desc);
		appDesc.setText(R.string.app_desc);
		if (CatnutApp.getBoolean(R.string.pref_fantasy_say_salutation, R.bool.default_fantasy_say_salutation)) {
			version.setText(getString(R.string.about_version_template, getString(R.string.version_name)));
//			Calendar now = Calendar.getInstance();
//			boolean salutation = now.get(Calendar.MONTH) <= Calendar.MAY;
			int n = (int) (Math.random() * 101);
			if (0 < n && n < 22) {
				about.setText(Html.fromHtml(getString(R.string.about_body)));
				about.setMovementMethod(LinkMovementMethod.getInstance());
			} else if (n > 30) {
				about.setText(Html.fromHtml(getString(R.string.salutation2)));
				about.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			} else {
				about.setText(Html.fromHtml(getString(R.string.salutation)));
				about.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			}
		} else {
			mAbout.setVisibility(View.GONE);
		}

		loadImage();
		mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (mApp.getPreferences().getBoolean(getString(R.string.enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
		}
	}

	public boolean isNetworkAvailable() {
		NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		return activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();
	}

	// save image urls locally
	private void fetch500px() {
		if (Photo.shouldRefresh()) {
			mApp.getRequestQueue().add(new CatnutRequest(
					this,
					_500pxAPI.photos(Photo.FEATURE_POPULAR, 0),
					new Photo._500pxProcessor(),
					null,
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e(TAG, "fetch 500px error", error);
							final WeiboAPIError volleyError = WeiboAPIError.fromVolleyError(error);
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(HelloActivity.this, volleyError.error, Toast.LENGTH_LONG).show();
								}
							});
						}
					}
			));
		}
	}

	private void loadImage() {
		(new Thread(mLoadImage)).start();
	}

	private Runnable mLoadImage = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(null, null, Photo.TABLE, null, Constants.RANDOM_ORDER, "1");
			final Cursor cursor = getContentResolver().query(CatnutProvider.parse(Photo.MULTIPLE), null, query, null, null);
			if (cursor.moveToNext()) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Image image = new Image();
						image.name = cursor.getString(cursor.getColumnIndex(Photo.name));
						image.url = cursor.getString(cursor.getColumnIndex(Photo.image_url));
						if (mTargetFromGrid != null) {
							mImages.add(mTargetFromGrid);
							mPagerAdapter.notifyDataSetChanged();
							mViewPager.setCurrentItem(1);
						} else {
							mImages.add(image);
						}
						mImages.add(image);
						mPagerAdapter.notifyDataSetChanged();
						cursor.close();
					}
				});
			} else {
				cursor.close();
			}
		}
	};

	public void onMenuItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.toggle_fantasy:
				int i = mViewPager.getCurrentItem();
				if (i == 0) {
					if (mAbout.getVisibility() == View.VISIBLE) {
						mAbout.setVisibility(View.GONE);
						item.setTitle(R.string.show_fantasy_words)
								.setIcon(R.drawable.ic_action_fullscreen_off);
					} else {
						mAbout.setVisibility(View.VISIBLE);
						item.setTitle(R.string.hide_fantasy_words)
								.setIcon(R.drawable.ic_action_fullscreen_on);
					}
				} else {
					if (mFantasyDesc.getVisibility() == View.VISIBLE) {
						mFantasyDesc.setVisibility(View.GONE);
						item.setTitle(R.string.show_fantasy_words)
								.setIcon(R.drawable.ic_action_fullscreen_off);
					} else {
						mFantasyDesc.setVisibility(View.VISIBLE);
						item.setTitle(R.string.hide_fantasy_words)
								.setIcon(R.drawable.ic_action_fullscreen_on);
					}
				}
				break;
			case android.R.id.home:
				Intent intent = getIntent();
				if (Intent.ACTION_MAIN.equals(intent.getAction())) {
					startActivity(new Intent(this, MainActivity.class));
				} else {
					navigateUpTo(intent);
				}
				break;
			case R.id.home:
				startActivity(new Intent(this, MainActivity.class));
				break;
			default:
				break;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mTracker != null) {
			mTracker.send(MapBuilder.createAppView().set(Fields.SCREEN_NAME, "auth").build());
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

	private class PagerListener extends ViewPager.SimpleOnPageChangeListener {
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			if (mImages.size() - position <= 3) {
				new Thread(expand).start();
			}
		}

		@Override
		public void onPageSelected(int position) {
			if (position != 0) {
				mAbout.setVisibility(View.GONE);
				mFantasyDesc.setVisibility(View.VISIBLE);
				String desc = mImages.get(position).name;
				if (!Constants.NULL.equals(desc)) {
					mFantasyDesc.setText(Html.fromHtml(desc));
				}
			} else {
				if (CatnutApp.getBoolean(R.string.pref_fantasy_say_salutation, R.bool.default_fantasy_say_salutation)) {
					mAbout.setVisibility(View.VISIBLE);
				} else {
					mAbout.setVisibility(View.GONE);
				}
				mFantasyDesc.setVisibility(View.GONE);
			}
		}
	}

	private static final String[] PROJECTION = new String[]{
			Photo.image_url,
			Photo.name,
	};

	private Runnable expand = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(PROJECTION, null, Photo.TABLE, null, Constants.RANDOM_ORDER, String.valueOf(mImages.size() + 10));
			final Cursor cursor = getContentResolver()
					.query(CatnutProvider.parse(Photo.MULTIPLE), null, query, null, null);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Image image;
					while (cursor.moveToNext()) {
						image = new Image();
						image.url = cursor.getString(0);
						image.name = cursor.getString(1);
						mImages.add(image);
					}
					cursor.close();
					mPagerAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	private class Gallery extends FragmentStatePagerAdapter {

		public Gallery() {
			super(getFragmentManager());
		}

		@Override
		public Fragment getItem(int position) {
			Image image = mImages.get(position);
			return FantasyFragment.getFragment(image.url, image.name, position == 0);
		}

		@Override
		public int getCount() {
			return mImages.size();
		}
	}

	private static class Image {
		String url;
		String name;
	}
}