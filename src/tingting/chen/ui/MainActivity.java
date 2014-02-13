/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.*;
import android.content.AsyncQueryHandler;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import tingting.chen.R;
import tingting.chen.adapter.DrawerNavAdapter;
import tingting.chen.adapter.TweetAdapter;
import tingting.chen.fragment.HomeTimelineFragment;
import tingting.chen.fragment.PrefFragment;
import tingting.chen.fragment.UserTimeLineFragment;
import tingting.chen.metadata.Status;
import tingting.chen.metadata.User;
import tingting.chen.support.TweetImageSpan;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.tingting.TingtingProvider;
import tingting.chen.util.Constants;
import tingting.chen.util.TingtingUtils;

import java.text.ParseException;
import java.util.Date;

/**
 * 应用程序主界面。
 *
 * @author longkai
 */
public class MainActivity extends Activity implements DrawerLayout.DrawerListener, ListView.OnItemClickListener,
	FragmentManager.OnBackStackChangedListener {

	private static final String TAG = "MainActivity";

	private static final int[] DRAWER_LIST_ITEMS_IDS = {
		0, // 我的
		R.id.action_my_followings,
		R.id.action_my_followers,
		R.id.action_my_list,
		R.id.action_my_favorites,
		R.id.action_my_drafts,
		0, // 分享
		R.id.action_share_app,
		R.id.action_view_source_code,
	};

	/** the last title before drawer open */
	private transient CharSequence mTitleBeforeDrawerClosed;
	/** should we go back to the last title before the drawer open? */
	private boolean mShouldPopupLastTile = true;

	// for card flip animation
	private Handler mHandler = new Handler();

	private TingtingApp mApp;
	private ImageLoader mImageLoader;
	private ActionBar mActionBar;

	private View mDrawer;
	private ListView mListView;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	private String mNick;
	private ImageView mProfileCover;
	private TextView mTextNick;
	private TextView mDescription;
	private ViewStub mLatestTweet;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			setContentView(R.layout.main);

			mApp = TingtingApp.getTingtingApp();
			mImageLoader = mApp.getImageLoader();

			// drawer specific
			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
			mDrawerLayout.setDrawerListener(this);

			mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.open_drawer, R.string.close_drawer);

			// the whole left drawer
			mDrawer = findViewById(R.id.drawer);

			mListView = (ListView) findViewById(android.R.id.list);
			mListView.setAdapter(new DrawerNavAdapter(this, R.array.drawer_list, R.array.drawer_list_header_indexes));
			mListView.setOnItemClickListener(this);

			// drawer customized view
			mProfileCover = (ImageView) findViewById(R.id.avatar_profile);
			mTextNick = (TextView) findViewById(R.id.nick);
			mDescription = (TextView) findViewById(R.id.description);
			mLatestTweet = (ViewStub) findViewById(R.id.latest_tweet);

			mActionBar = getActionBar();
			prepareActionBar();
			fetchLatestTweet();

			getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_container, new HomeTimelineFragment())
				.commit();
			getFragmentManager().addOnBackStackChangedListener(this);
		}
	}

	/**
	 * 设置顶部，关联用户的头像和昵称
	 */
	private void prepareActionBar() {
		// for drawer
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
		// for user' s profile
		new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor != null && cursor.moveToNext()) {
					mActionBar.setDisplayUseLogoEnabled(true);
					mNick = cursor.getString(cursor.getColumnIndex(User.screen_name));
					mActionBar.setTitle(mNick);
					mImageLoader.get(cursor.getString(cursor.getColumnIndex(User.profile_image_url)), new ImageLoader.ImageListener() {
						@Override
						public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
							mActionBar.setIcon(new BitmapDrawable(getResources(), response.getBitmap()));
						}

						@Override
						public void onErrorResponse(VolleyError error) {
						}
					});
					mTextNick.setText(mNick);
					mImageLoader.get(cursor.getString(cursor.getColumnIndex(User.avatar_large)),
						ImageLoader.getImageListener(
							mProfileCover, R.drawable.error, R.drawable.error
						));
					String description = cursor.getString(cursor.getColumnIndex(User.description));
					mDescription.setText(TextUtils.isEmpty(description) ? getString(R.string.no_description) : description);

					View flowingCount = findViewById(R.id.following_count);
					TingtingUtils.setText(flowingCount, android.R.id.text1, cursor.getString(cursor.getColumnIndex(User.friends_count)));
					TingtingUtils.setText(flowingCount, android.R.id.text2, getString(R.string.followings));
					View flowerCount = findViewById(R.id.followers_count);
					TingtingUtils.setText(flowerCount, android.R.id.text1, cursor.getString(cursor.getColumnIndex(User.followers_count)));
					TingtingUtils.setText(flowerCount, android.R.id.text2, getString(R.string.followers));
					View tweetsCount = findViewById(R.id.tweets_count);
					tweetsCount.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							UserTimeLineFragment fragment = new UserTimeLineFragment();
							Bundle args = new Bundle();
							args.putLong(Constants.ID, mApp.getAccessToken().uid);
							fragment.setArguments(args);
							mShouldPopupLastTile = false;
							mDrawerLayout.closeDrawer(mDrawer);
							flipCard(fragment, null);
						}
					});
					TingtingUtils.setText(tweetsCount, android.R.id.text1, cursor.getString(cursor.getColumnIndex(User.statuses_count)));
					TingtingUtils.setText(tweetsCount, android.R.id.text2, getString(R.string.tweets));
					cursor.close();
				}
			}
		}.startQuery(
			0,
			null,
			TingtingProvider.parse(User.MULTIPLE, String.valueOf(mApp.getAccessToken().uid)),
			new String[]{
				User.screen_name,
				User.profile_image_url,
				User.avatar_large,
				User.description,
				User.statuses_count,
				User.followers_count,
				User.friends_count
			},
			null,
			null,
			null
		);
	}

	private void fetchLatestTweet() {
		String query = TingtingUtils.buildQuery(
			new String[]{
				Status.columnText,
				Status.thumbnail_pic,
				Status.comments_count,
				Status.reposts_count,
				Status.attitudes_count,
				Status.source,
				Status.created_at,
			},
			"_id=(select max(s._id) from " + Status.TABLE + " as s where s.uid=" + mApp.getAccessToken().uid + ")",
			Status.TABLE,
			null,
			null,
			null
		);
		new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor != null && cursor.moveToNext()) {
					View tweet = mLatestTweet.inflate();
					String tweetText = cursor.getString(cursor.getColumnIndex(Status.columnText));
					TextView text = TingtingUtils.setText(tweet, R.id.text, new TweetImageSpan(MainActivity.this).getImageSpan(tweetText));
					// 不处理链接，直接跳转到自己所有的微博
					Linkify.addLinks(text, TweetAdapter.MENTION_PATTERN, null, null, null);
					Linkify.addLinks(text, TweetAdapter.TOPIC_PATTERN, null, null, null);
					Linkify.addLinks(text, TweetAdapter.WEB_URL, null, null, null);
					TingtingUtils.removeLinkUnderline(text);

					int replyCount = cursor.getInt(cursor.getColumnIndex(Status.comments_count));
					TingtingUtils.setText(tweet, R.id.reply_count, TingtingUtils.approximate(replyCount));
					int retweetCount = cursor.getInt(cursor.getColumnIndex(Status.reposts_count));
					TingtingUtils.setText(tweet, R.id.reteet_count, TingtingUtils.approximate(retweetCount));
					int favoriteCount = cursor.getInt(cursor.getColumnIndex(Status.attitudes_count));
					TingtingUtils.setText(tweet, R.id.favorite_count, TingtingUtils.approximate(favoriteCount));
					String source = cursor.getString(cursor.getColumnIndex(Status.source));
					TingtingUtils.setText(tweet, R.id.source, Html.fromHtml(source).toString());
					try {
						Date date = TweetAdapter.sdf.parse(cursor.getString(cursor.getColumnIndex(Status.created_at)));
						TingtingUtils.setText(tweet, R.id.create_at, DateUtils.getRelativeTimeSpanString(date.getTime()));
					} catch (ParseException e) {
						Log.d(TAG, "parse time", e);
					}
					TingtingUtils.setText(tweet, R.id.nick, "@" + mActionBar.getTitle()).setTextColor(R.color.actionbar_background);
					cursor.close();
				}
			}
		}.startQuery(
			0,
			null,
			TingtingProvider.parse(Status.MULTIPLE),
			null,
			query,
			null,
			null
		);
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
				Fragment pref = getFragmentManager().findFragmentByTag(TAG);
				if (pref == null || !pref.isVisible()) {
					flipCard(new PrefFragment(), TAG);
				}
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
		mTitleBeforeDrawerClosed = mActionBar.getTitle();
		mActionBar.setTitle(getString(R.string.my_profile));
	}

	@Override
	public void onDrawerClosed(View drawerView) {
		mDrawerToggle.onDrawerClosed(drawerView);
		if (mShouldPopupLastTile) {
			mActionBar.setTitle(mTitleBeforeDrawerClosed);
		}
		mShouldPopupLastTile = true;
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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent;
		switch (DRAWER_LIST_ITEMS_IDS[position]) {
			case R.id.action_share_app:
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/*");
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app));
				intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
				intent.putExtra(Intent.EXTRA_STREAM,
					Uri.parse("android.resource://tingting.chen/drawable/ic_launcher"));
				startActivity(intent);
				break;
			case R.id.action_view_source_code:
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)));
				startActivity(intent);
				break;
			default:
				Toast.makeText(MainActivity.this, position + " click! not yet implement for now:-(", Toast.LENGTH_SHORT).show();
				break;
		}
		mDrawerLayout.closeDrawer(mDrawer);
	}

	@Override
	public void onBackStackChanged() {
		invalidateOptionsMenu();
	}

	/**
	 * 切换fragment时卡片翻转的效果
	 * @param fragment
	 * @param tag 没有赋null即可
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
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				invalidateOptionsMenu();
			}
		});
	}

	/**
	 * 获取当前授权用户的nick
	 */
	public String getDefaultUserNick() {
		return mNick;
	}
}