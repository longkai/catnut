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
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.analytics.tracking.android.EasyTracker;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.api.StuffAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.fragment.ConversationFragment;
import org.catnut.fragment.DraftFragment;
import org.catnut.fragment.FavoriteFragment;
import org.catnut.fragment.HomeTimelineFragment;
import org.catnut.fragment.MentionTimelineFragment;
import org.catnut.fragment.MyRelationshipFragment;
import org.catnut.fragment.UserTimelineFragment;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.support.ConfirmBarController;
import org.catnut.support.FragmentCallbackFromActivity;
import org.catnut.support.QuickReturnScrollView;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

/**
 * 应用程序主界面。
 *
 * @author longkai
 */
public class MainActivity extends Activity implements
		DrawerLayout.DrawerListener, View.OnClickListener,
		FragmentManager.OnBackStackChangedListener, QuickReturnScrollView.Callbacks {

	private static final String TAG = "MainActivity";

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

	private long mLastFetchMillis;
	private TextView mFetchNews;
	private TextView mNewTweet;
	private TextView mNewMention;
	private TextView mNewComment;

	private ConfirmBarController.Callbacks mCallbacks;

	private FragmentCallbackFromActivity mRefreshCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = CatnutApp.getTingtingApp();
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
		fetchNews();

		if (savedInstanceState == null) {
			HomeTimelineFragment fragment = HomeTimelineFragment.getFragment();
			mCallbacks = fragment;
			mRefreshCallback = fragment;
			getFragmentManager()
					.beginTransaction()
					.replace(R.id.fragment_container, fragment, HomeTimelineFragment.TAG)
					.commit();
		}

		getFragmentManager().addOnBackStackChangedListener(this);
		if (mApp.getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
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
					TextView location = (TextView) findViewById(R.id.location);
					location.setText(cursor.getString(cursor.getColumnIndex(User.location)));

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
						User.verified,
						User.location
				},
				null, null, null
		);
	}

	// 更新消息数
	private void fetchNews() {
		if (mFetchNews == null) {
			mFetchNews = (TextView) findViewById(R.id.fetch_news);
			mFetchNews.setOnClickListener(this);

			View newTweet = findViewById(R.id.new_tweet);
			CatnutUtils.setText(newTweet, android.R.id.text2, getString(R.string.new_tweet_count));
			mNewTweet = (TextView) newTweet.findViewById(android.R.id.text1);
			newTweet.setOnClickListener(this);

			View newComment = findViewById(R.id.new_comment);
			mNewComment = (TextView) newComment.findViewById(android.R.id.text1);
			CatnutUtils.setText(newComment, android.R.id.text2, getString(R.string.new_comment_count));
			newComment.setOnClickListener(this);

			View newMention = findViewById(R.id.new_mention);
			mNewMention = (TextView) newMention.findViewById(android.R.id.text1);
			CatnutUtils.setText(newMention, android.R.id.text2, getString(R.string.new_mention_count));
			newMention.setOnClickListener(this);
		}
		mFetchNews.setText(R.string.loading);
		mFetchNews.setClickable(false);
		mApp.getRequestQueue().add(new CatnutRequest(
				this,
				StuffAPI.unread_count(mApp.getAccessToken().uid, 0),
				null,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						CatnutUtils.setText(mNewTweet, android.R.id.text1, String.valueOf(response.optInt(Status.SINGLE)));
						CatnutUtils.setText(mNewComment, android.R.id.text1, String.valueOf(response.optInt("cmt")));
						CatnutUtils.setText(mNewMention, android.R.id.text1, String.valueOf(response.optInt("mention_status")));

						mLastFetchMillis = System.currentTimeMillis();
						mFetchNews.setText(getString(R.string.last_check_time, DateUtils.getRelativeTimeSpanString(mLastFetchMillis)));
						mFetchNews.setClickable(true);
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
						Toast.makeText(MainActivity.this, weiboAPIError.error, Toast.LENGTH_LONG).show();
						mFetchNews.setText(getString(R.string.error_click_try_again));
						mFetchNews.setClickable(true);
					}
				}
		));
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
			case R.id.plugins:
				switch2Plugins(null);
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
		mFetchNews.setText(getString(R.string.last_check_time, DateUtils.getRelativeTimeSpanString(mLastFetchMillis)));
	}

	@Override
	public void onDrawerClosed(View drawerView) {
		mDrawerToggle.onDrawerClosed(drawerView);
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
		int id = v.getId();
		if (id != R.id.fetch_news) {
			mDrawerLayout.closeDrawer(mQuickReturnDrawer);
		}
		Fragment fragment = null;
		String tag = null;
		switch (id) {
			case R.id.tweets_count:
			case R.id.action_my_tweets:
				fragment = UserTimelineFragment.getFragment(mApp.getAccessToken().uid, mApp.getPreferences().getString(User.screen_name, null));
				tag = UserTimelineFragment.TAG;
				break;
			case R.id.following_count:
			case R.id.action_my_followings:
				fragment = MyRelationshipFragment.getFragment(true);
				tag = "true";
				break;
			case R.id.followers_count:
			case R.id.action_my_followers:
				fragment = MyRelationshipFragment.getFragment(false);
				tag = "false";
				break;
			case R.id.action_my_favorites:
				fragment = FavoriteFragment.getFragment();
				tag = FavoriteFragment.TAG;
				break;
			case R.id.action_my_drafts:
				fragment = DraftFragment.getFragment();
				tag = DraftFragment.TAG;
				break;
			case R.id.action_share_app:
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType(getString(R.string.mime_image));
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app));
				intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
				intent.putExtra(Intent.EXTRA_STREAM,
						Uri.fromFile(new File(getExternalCacheDir() + File.separator + Constants.SHARE_IMAGE)));
				startActivity(intent);
				return;
			case R.id.action_view_source_code:
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link))));
				return;
			case R.id.fetch_news:
				fetchNews();
				return;
			case R.id.new_tweet:
				// 回掉主页时间线
				Fragment home = getFragmentManager().findFragmentByTag(HomeTimelineFragment.TAG);
				// never null, but we still check it.
				if (home == null || !home.isVisible()) {
					fragment = HomeTimelineFragment.getFragment();
					tag = HomeTimelineFragment.TAG;
				} else {
					if (mRefreshCallback != null) {
						mRefreshCallback.callback(null);
					}
				}
				mNewTweet.setText("0");
				break;
			case R.id.new_mention:
				fragment = MentionTimelineFragment.getFragment();
				tag = MentionTimelineFragment.TAG;
				mNewMention.setText("0");
				break;
			case R.id.new_comment:
				fragment = ConversationFragment.getFragment();
				tag = ConversationFragment.TAG;
				break;
			case R.id.action_my_list:
			default:
				Toast.makeText(this, "sorry, not yet implemented =.=", Toast.LENGTH_SHORT).show();
				return;
		}
		if (fragment != null) {
			pendingFragment(fragment, tag);
		}
	}

	// 切换到插件，如果没有启用插件则跳转到插件设置界面
	public void switch2Plugins(View view) {
		if (mDrawerLayout.isDrawerOpen(mQuickReturnDrawer)) {
			mDrawerLayout.closeDrawer(mQuickReturnDrawer);
		}
		if (view != null) { // 来自layout xml
			String key = (String) view.getTag();
			boolean enable = CatnutApp.getTingtingApp().getPreferences()
					.getBoolean(key, getResources().getBoolean(R.bool.default_plugin_status));
			if (!enable) {
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PLUGINS_PREF));
				return;
			}
		}
		ArrayList<Integer> plugins = CatnutUtils.enabledPlugins();
		if (plugins != null) {
			Intent intent = new Intent(this, PluginsActivity.class);
			intent.putExtra(PluginsActivity.PLUGINS, plugins);
			startActivity(intent);
		} else {
			startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PLUGINS_PREF));
		}
	}

	/**
	 * 切换fragment，附带一个动画效果
	 *
	 * @param fragment
	 * @param tag      没有赋null即可
	 */
	private void pendingFragment(Fragment fragment, String tag) {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment tmp = fragmentManager.findFragmentByTag(tag);
		if (tmp == null || !tmp.isVisible()) {
			fragmentManager
					.beginTransaction()
					.setCustomAnimations(
							R.animator.fragment_slide_left_enter,
							R.animator.fragment_slide_left_exit,
							R.animator.fragment_slide_right_enter,
							R.animator.fragment_slide_right_exit
					)
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