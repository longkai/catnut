/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.AsyncQueryHandler;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.analytics.tracking.android.EasyTracker;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.fragment.HomeTimelineFragment;
import org.catnut.metadata.User;
import org.catnut.support.ConfirmBarController;
import org.catnut.support.QuickReturnScrollView;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

import java.lang.reflect.Field;

/**
 * 应用程序主界面。
 *
 * @author longkai
 */
public class MainActivity extends Activity implements
		DrawerLayout.DrawerListener, View.OnClickListener,
		FragmentManager.OnBackStackChangedListener, QuickReturnScrollView.Callbacks {

	private static final String TAG = "MainActivity";

	private static final int[] DRAWER_LIST_ITEMS_IDS = {
			0, // 我的
			R.id.action_my_tweets,
			R.id.action_my_followings,
			R.id.action_my_followers,
			R.id.action_my_list,
			R.id.action_my_favorites,
			R.id.action_my_drafts,
			0, // 分享
			R.id.action_share_app,
			R.id.action_view_source_code,
	};

	// for card flip animation
	private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();

	private CatnutApp mApp;
	private EasyTracker mTracker;
	private ActionBar mActionBar;

	private DrawerLayout mDrawerLayout;
	private View mPlaceholderView;
	private View mQuickReturnView;
	private QuickReturnScrollView mQuickReturnDrawer;
	private ActionBarDrawerToggle mDrawerToggle;

	private int mMinRawY = 0;
	private int mState = STATE_ON_SCREEN;
	private int mQuickReturnHeight;
	private int mMaxScrollY;

	private String mNick;
	private ImageView mProfileCover;
	private TextView mTextNick;
	private TextView mDescription;

	private ConfirmBarController.Callbacks mCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = CatnutApp.getTingtingApp();
		forceOverflow();
		mActionBar = getActionBar();
		mActionBar.setIcon(R.drawable.ic_title_home);
		setContentView(R.layout.main);
		// drawer specific
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
		mDrawerLayout.setDrawerListener(this);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.open_drawer, R.string.close_drawer);

		// the whole left drawer
		mQuickReturnDrawer = (QuickReturnScrollView) findViewById(R.id.drawer);
		mQuickReturnDrawer.setCallbacks(this);
		mQuickReturnView = findViewById(R.id.quick_return);
		mPlaceholderView = findViewById(R.id.place_holder);

		mQuickReturnDrawer.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						onScrollChanged(mQuickReturnDrawer.getScrollY());
						mMaxScrollY = mQuickReturnDrawer.computeVerticalScrollRange()
								- mQuickReturnDrawer.getHeight();
						mQuickReturnHeight = mQuickReturnView.getHeight();
					}
				}
		);

		// drawer customized view
		mProfileCover = (ImageView) findViewById(R.id.avatar_profile);
		mTextNick = (TextView) findViewById(R.id.nick);
		mDescription = (TextView) findViewById(R.id.description);

		prepareDrawer();
		injectListeners();

		if (savedInstanceState == null) {
			HomeTimelineFragment fragment = HomeTimelineFragment.getFragment();
			mCallbacks = fragment;
			getFragmentManager()
					.beginTransaction()
					.replace(R.id.fragment_container, fragment)
					.commit();
		}

		getFragmentManager().addOnBackStackChangedListener(this);
		if (mApp.getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
		}
	}

	private void forceOverflow() {
		ViewConfiguration viewConfig = ViewConfiguration.get(this);
		try {
			Field menuKey = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKey != null) {
				menuKey.setAccessible(true);
				menuKey.setBoolean(viewConfig, true);
			}
		} catch (Exception e) {
			Log.e(TAG, "force to show overflow error!", e);
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
	protected void onSaveInstanceState(Bundle outState) {
		if (mCallbacks != null) {
			mCallbacks.onActivitySaveInstanceState(outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (mCallbacks != null) {
			mCallbacks.onActivityRestoreInstanceState(savedInstanceState);
		}
	}

	private void prepareDrawer() {
		// for drawer
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
		// for user' s profile
		new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					mNick = cursor.getString(cursor.getColumnIndex(User.screen_name));
					mTextNick.setText(mNick);
					Picasso.with(MainActivity.this)
							.load(cursor.getString(cursor.getColumnIndex(User.avatar_large)))
							.placeholder(R.drawable.error)
							.error(R.drawable.error)
							.into(mProfileCover);
					String description = cursor.getString(cursor.getColumnIndex(User.description));
					mDescription.setText(TextUtils.isEmpty(description) ? getString(R.string.no_description) : description);

					View flowingCount = findViewById(R.id.following_count);
					CatnutUtils.setText(flowingCount, android.R.id.text1, cursor.getString(cursor.getColumnIndex(User.friends_count)));
					CatnutUtils.setText(flowingCount, android.R.id.text2, getString(R.string.followings));
					View flowerCount = findViewById(R.id.followers_count);
					CatnutUtils.setText(flowerCount, android.R.id.text1, cursor.getString(cursor.getColumnIndex(User.followers_count)));
					CatnutUtils.setText(flowerCount, android.R.id.text2, getString(R.string.followers));
					View tweetsCount = findViewById(R.id.tweets_count);

					tweetsCount.setOnClickListener(MainActivity.this);
					flowingCount.setOnClickListener(MainActivity.this);
					flowerCount.setOnClickListener(MainActivity.this);
					CatnutUtils.setText(tweetsCount, android.R.id.text1, cursor.getString(cursor.getColumnIndex(User.statuses_count)));
					CatnutUtils.setText(tweetsCount, android.R.id.text2, getString(R.string.tweets));
				}
				cursor.close();
			}
		}.startQuery(
				0, null,
				CatnutProvider.parse(User.MULTIPLE, mApp.getAccessToken().uid),
				new String[]{
						User.screen_name,
						User.avatar_large,
						User.description,
						User.statuses_count,
						User.followers_count,
						User.friends_count,
						User.verified
				},
				null, null, null
		);
	}

	private void injectListeners() {
		findViewById(R.id.action_my_tweets).setOnClickListener(this);
		findViewById(R.id.action_my_followings).setOnClickListener(this);
		findViewById(R.id.action_my_followers).setOnClickListener(this);
		findViewById(R.id.action_my_list).setOnClickListener(this);
		findViewById(R.id.action_my_favorites).setOnClickListener(this);
		findViewById(R.id.action_my_drafts).setOnClickListener(this);
		findViewById(R.id.action_share_app).setOnClickListener(this);
		findViewById(R.id.action_view_source_code).setOnClickListener(this);
		findViewById(R.id.fantasy).setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// open or close the drawer
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
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
			// 注销，需要重新授权的
			case R.id.cancellation:
				new AlertDialog.Builder(this)
						.setMessage(getString(R.string.cancellation_confirm))
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mApp.invalidateAccessToken();
								Intent intent = new Intent(MainActivity.this, HelloActivity.class);
								// 清除掉之前的back stack哦
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							}
						})
						.setNegativeButton(android.R.string.no, null)
						.show();
				break;
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PREF));
				break;
			case R.id.action_compose:
				startActivity(new Intent(this, ComposeTweetActivity.class));
				break;
			case R.id.fantasy:
				startActivity(new Intent(this, HelloActivity.class).putExtra(HelloActivity.TAG, HelloActivity.TAG));
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onDrawerOpened(View drawerView) {
		mDrawerToggle.onDrawerOpened(drawerView);
		mActionBar.setTitle(getString(R.string.my_profile));
	}

	@Override
	public void onDrawerClosed(View drawerView) {
		mDrawerToggle.onDrawerClosed(drawerView);
		mActionBar.setTitle(getString(R.string.home_timeline));
	}

	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
	}

	@Override
	public void onDrawerStateChanged(int newState) {
		mDrawerToggle.onDrawerStateChanged(newState);
	}


	@Override
	public void onBackStackChanged() {
		invalidateOptionsMenu();
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
			case R.id.tweets_count:
			case R.id.action_my_tweets:
				intent = SingleFragmentActivity.getIntent(this, SingleFragmentActivity.USER_TWEETS);
				intent.putExtra(Constants.ID, mApp.getAccessToken().uid);
				intent.putExtra(User.screen_name, mApp.getPreferences().getString(User.screen_name, null));
				break;
			case R.id.following_count:
			case R.id.action_my_followings:
				intent = SingleFragmentActivity.getIntent(this, SingleFragmentActivity.FRIENDS);
				intent.putExtra(User.following, true);
				break;
			case R.id.followers_count:
			case R.id.action_my_followers:
				intent = SingleFragmentActivity.getIntent(this, SingleFragmentActivity.FRIENDS);
				intent.putExtra(User.following, false);
				break;
			case R.id.action_my_favorites:
				intent = SingleFragmentActivity.getIntent(this, SingleFragmentActivity.FAVORITES);
				break;
			case R.id.action_my_drafts:
				intent = SingleFragmentActivity.getIntent(this, SingleFragmentActivity.DRAFT);
				break;
			case R.id.action_share_app:
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/*");
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app));
				intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
				intent.putExtra(Intent.EXTRA_STREAM,
						Uri.parse("android.resource://org.catnut/drawable/ic_launcher"));
				break;
			case R.id.action_view_source_code:
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)));
				break;
			case R.id.fantasy:
				intent = new Intent(this, HelloActivity.class).putExtra(HelloActivity.TAG, HelloActivity.TAG);
				break;
			case R.id.action_my_list:
			default:
				Toast.makeText(this, "sorry, not yet implemented =.=", Toast.LENGTH_SHORT).show();
				return;
		}
		startActivity(intent);
		mDrawerLayout.closeDrawer(mQuickReturnDrawer);
	}

	/**
	 * 切换fragment时卡片翻转的效果
	 *
	 * @param fragment
	 * @param tag      没有赋null即可
	 */
	private void flipCard(Fragment fragment, String tag) {
		getFragmentManager()
				.beginTransaction()
				.setCustomAnimations(
						R.animator.card_flip_right_in, R.animator.card_flip_right_out,
						R.animator.card_flip_left_in, R.animator.card_flip_left_out)
				.replace(R.id.fragment_container, fragment, tag)
				.addToBackStack(null)
				.commit();
		mScrollSettleHandler.post(new Runnable() {
			@Override
			public void run() {
				invalidateOptionsMenu();
			}
		});
	}

	@Override
	public void onScrollChanged(int scrollY) {
		scrollY = Math.min(mMaxScrollY, scrollY);

		mScrollSettleHandler.onScroll(scrollY);

		int rawY = mPlaceholderView.getTop() - scrollY;
		int translationY = 0;

		switch (mState) {
			case STATE_OFF_SCREEN:
				if (rawY <= mMinRawY) {
					mMinRawY = rawY;
				} else {
					mState = STATE_RETURNING;
				}
				translationY = rawY;
				break;

			case STATE_ON_SCREEN:
				if (rawY < -mQuickReturnHeight) {
					mState = STATE_OFF_SCREEN;
					mMinRawY = rawY;
				}
				translationY = rawY;
				break;

			case STATE_RETURNING:
				translationY = (rawY - mMinRawY) - mQuickReturnHeight;
				if (translationY > 0) {
					translationY = 0;
					mMinRawY = rawY - mQuickReturnHeight;
				}

				if (rawY > 0) {
					mState = STATE_ON_SCREEN;
					translationY = rawY;
				}

				if (translationY < -mQuickReturnHeight) {
					mState = STATE_OFF_SCREEN;
					mMinRawY = rawY;
				}
				break;
		}
		mQuickReturnView.animate().cancel();
		mQuickReturnView.setTranslationY(translationY + scrollY);
	}

	@Override
	public void onDownMotionEvent() {
		mScrollSettleHandler.setSettleEnabled(false);
	}

	@Override
	public void onUpOrCancelMotionEvent() {
		mScrollSettleHandler.setSettleEnabled(true);
		mScrollSettleHandler.onScroll(mQuickReturnDrawer.getScrollY());
	}

	// quick return animation
	private class ScrollSettleHandler extends Handler {
		private static final int SETTLE_DELAY_MILLIS = 100;

		private int mSettledScrollY = Integer.MIN_VALUE;
		private boolean mSettleEnabled;

		public void onScroll(int scrollY) {
			if (mSettledScrollY != scrollY) {
				// Clear any pending messages and post delayed
				removeMessages(0);
				sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
				mSettledScrollY = scrollY;
			}
		}

		public void setSettleEnabled(boolean settleEnabled) {
			mSettleEnabled = settleEnabled;
		}

		@Override
		public void handleMessage(Message msg) {
			// Handle the scroll settling.
			if (STATE_RETURNING == mState && mSettleEnabled) {
				int mDestTranslationY;
				if (mSettledScrollY - mQuickReturnView.getTranslationY() > mQuickReturnHeight / 2) {
					mState = STATE_OFF_SCREEN;
					mDestTranslationY = Math.max(
							mSettledScrollY - mQuickReturnHeight,
							mPlaceholderView.getTop());
				} else {
					mDestTranslationY = mSettledScrollY;
				}

				mMinRawY = mPlaceholderView.getTop() - mQuickReturnHeight - mDestTranslationY;
				mQuickReturnView.animate().translationY(mDestTranslationY);
			}
			mSettledScrollY = Integer.MIN_VALUE; // reset
		}
	}
}