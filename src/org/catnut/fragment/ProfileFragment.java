/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.viewpagerindicator.LinePageIndicator;
import org.catnut.R;
import org.catnut.api.FriendshipsAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.UserProcessor;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.ui.ProfileActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONObject;

/**
 * 用户信息界面
 *
 * @author longkai
 */
public class ProfileFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String[] PROJECTION = new String[]{
			BaseColumns._ID,
			User.remark,
			User.verified,
			User.screen_name,
			User.profile_url,
			User.location,
			User.description,
			User.avatar_large,
			User.cover_image,
			User.favourites_count,
			User.friends_count,
			User.statuses_count,
			User.followers_count,
			User.following
	};

	private CatnutApp mApp;
	private Menu mMenu;

	private long mUid;
	private String mScreenName;
	private String mCoverUrl;
	private String mAvatarUrl;
	private boolean mVerified;
	private boolean mFollowing; // 哥是否关注他
	private String mRemark;
	private String mDescription;
	private String mLocation;
	private String mProfileUrl;

	private ViewPager mViewPager;
	private LinePageIndicator mIndicator;
	private View mPlaceHolder;
	private View mTweetsCount;
	private View mFollowingsCount;
	private View mFollowersCount;
	private View mTweetLayout;

	private View.OnClickListener tweetsOnclickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ProfileActivity activity = (ProfileActivity) getActivity();
			activity.flipCard(UserTimelineFragment.getFragment(mUid, mScreenName), null, true);
		}
	};

	private View.OnClickListener followersOnclickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ProfileActivity activity = (ProfileActivity) getActivity();
			activity.flipCard(TransientUsersFragment.getFragment(mScreenName, false), null, true);
		}
	};

	private View.OnClickListener followingsOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ProfileActivity activity = (ProfileActivity) getActivity();
			activity.flipCard(TransientUsersFragment.getFragment(mScreenName, true), null, true);
		}
	};

	private Target profileTarget = new Target() {
		@Override
		public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
			mPlaceHolder.setBackground(new BitmapDrawable(getResources(), bitmap));
		}

		@Override
		public void onBitmapFailed(Drawable errorDrawable) {
			mPlaceHolder.setBackground(errorDrawable);
		}

		@Override
		public void onPrepareLoad(Drawable placeHolderDrawable) {
			//no-op
		}
	};

	public static ProfileFragment getFragment(long uid, String screenName) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, uid);
		args.putString(User.screen_name, screenName);
		ProfileFragment fragment = new ProfileFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mApp = CatnutApp.getTingtingApp();
		Bundle args = getArguments();
		mUid = args.getLong(Constants.ID);
		mScreenName = args.getString(User.screen_name);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(mScreenName);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.profile, container, false);
		mViewPager = (ViewPager) view.findViewById(R.id.pager);
		mIndicator = (LinePageIndicator) view.findViewById(R.id.indicator);
		mPlaceHolder = view.findViewById(R.id.place_holder);
		mTweetsCount = view.findViewById(R.id.tweets_count);
		mFollowingsCount = view.findViewById(R.id.following_count);
		mFollowersCount = view.findViewById(R.id.followers_count);
		mTweetLayout = view.findViewById(R.id.tweet_layout);
		view.findViewById(R.id.action_tweets).setOnClickListener(tweetsOnclickListener);
		view.findViewById(R.id.action_followers).setOnClickListener(followersOnclickListener);
		view.findViewById(R.id.action_followings).setOnClickListener(followingsOnClickListener);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// 从本地抓取数据*_*，有一个时间先后的问题，所以把view的创建放到这个地方来了
		String query = CatnutUtils.buildQuery(PROJECTION,
				User.screen_name + "=" + CatnutUtils.quote(mScreenName), User.TABLE, null, null, null);
		new AsyncQueryHandler(getActivity().getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					// 暂存元数据
					mUid = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
					mAvatarUrl = cursor.getString(cursor.getColumnIndex(User.avatar_large));
					mVerified = CatnutUtils.getBoolean(cursor, User.verified);
					mRemark = cursor.getString(cursor.getColumnIndex(User.remark));
					mDescription = cursor.getString(cursor.getColumnIndex(User.description));
					mLocation = cursor.getString(cursor.getColumnIndex(User.location));
					mProfileUrl = cursor.getString(cursor.getColumnIndex(User.profile_url));
					mCoverUrl = cursor.getString(cursor.getColumnIndex(User.cover_image));
					// +关注
					mFollowing = CatnutUtils.getBoolean(cursor, User.following);
					// menu
					buildMenu();
					// load封面图片
					if (!TextUtils.isEmpty(mCoverUrl)) {
						Picasso.with(getActivity())
								.load(mCoverUrl)
								.error(R.drawable.defaul_fantasy)
								.into(profileTarget);
					} else {
						mPlaceHolder.setBackground(getResources().getDrawable(R.drawable.defaul_fantasy));
					}
					// 我的微博
					mTweetsCount.setOnClickListener(tweetsOnclickListener);
					CatnutUtils.setText(mTweetsCount, android.R.id.text1,
							cursor.getString(cursor.getColumnIndex(User.statuses_count)));
					CatnutUtils.setText(mTweetsCount, android.R.id.text2, getString(R.string.tweets));
					// 关注我的
					mFollowersCount.setOnClickListener(followersOnclickListener);
					CatnutUtils.setText(mFollowersCount, android.R.id.text1,
							cursor.getString(cursor.getColumnIndex(User.followers_count)));
					CatnutUtils.setText(mFollowersCount, android.R.id.text2, getString(R.string.followers));
					// 我关注的
					mFollowingsCount.setOnClickListener(followingsOnClickListener);
					CatnutUtils.setText(mFollowingsCount, android.R.id.text1,
							cursor.getString(cursor.getColumnIndex(User.friends_count)));
					CatnutUtils.setText(mFollowingsCount, android.R.id.text2, getString(R.string.followings));
					// pager adapter, not fragment pager any more
					mViewPager.setAdapter(coverPager);
					mIndicator.setViewPager(mViewPager);
				} else {
					Toast.makeText(getActivity(), getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
				}
				cursor.close();
			}
		}.startQuery(0, null, CatnutProvider.parse(User.MULTIPLE), null, query, null, null);
		// 抓最新的一条微博
		if (mApp.getPreferences().getBoolean(getString(R.string.pref_show_latest_tweet), true)) {
			String queryLatestTweet = CatnutUtils.buildQuery(
					new String[]{
							Status.columnText,
							Status.thumbnail_pic,
							Status.comments_count,
							Status.reposts_count,
							Status.attitudes_count,
							Status.source,
							Status.created_at,
					},
					"uid=(select _id from " + User.TABLE + " where " + User.screen_name
							+ "=" + CatnutUtils.quote(mScreenName) + ")",
					Status.TABLE,
					null,
					BaseColumns._ID + " desc",
					"1"
			);
			new AsyncQueryHandler(getActivity().getContentResolver()) {
				@Override
				protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
					if (cursor.moveToNext()) {
						mTweetLayout.setOnClickListener(tweetsOnclickListener);
						ViewStub viewStub = (ViewStub) mTweetLayout.findViewById(R.id.latest_tweet);
						View tweet = viewStub.inflate();
						// todo: retweet layout!
						tweet.findViewById(R.id.retweet).setVisibility(View.GONE);
						CatnutUtils.setText(tweet, R.id.nick, getString(R.string.latest_statues))
								.setTextColor(getResources().getColor(R.color.actionbar_background));
						String tweetText = cursor.getString(cursor.getColumnIndex(Status.columnText));
						TweetTextView text = (TweetTextView) CatnutUtils.setText(tweet, R.id.text,
								new TweetImageSpan(getActivity()).getImageSpan(tweetText));
						CatnutUtils.vividTweet(text, null);

						int replyCount = cursor.getInt(cursor.getColumnIndex(Status.comments_count));
						CatnutUtils.setText(tweet, R.id.reply_count, CatnutUtils.approximate(replyCount));
						int retweetCount = cursor.getInt(cursor.getColumnIndex(Status.reposts_count));
						CatnutUtils.setText(tweet, R.id.reteet_count, CatnutUtils.approximate(retweetCount));
						int favoriteCount = cursor.getInt(cursor.getColumnIndex(Status.attitudes_count));
						CatnutUtils.setText(tweet, R.id.favorite_count, CatnutUtils.approximate(favoriteCount));
						String source = cursor.getString(cursor.getColumnIndex(Status.source));
						CatnutUtils.setText(tweet, R.id.source, Html.fromHtml(source).toString());
						String create_at = cursor.getString(cursor.getColumnIndex(Status.created_at));
						CatnutUtils.setText(tweet, R.id.create_at, DateUtils.getRelativeTimeSpanString(DateTime.getTimeMills(create_at)));
					}
					cursor.close();
				}
			}.startQuery(0, null, CatnutProvider.parse(Status.MULTIPLE), null, queryLatestTweet, null, null);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		mMenu = menu;
		buildMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_follow:
				toggleFollow(true);
				break;
			case R.id.action_unfollow:
				toggleFollow(false);
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	// 创建menu，只创建一次哦
	private void buildMenu() {
		if (mMenu != null && mAvatarUrl != null) { // 确保数据已经从sqlite载入
			// 只有当两者均为空时才创建menu
			if (mMenu.findItem(R.id.action_follow) == null && mMenu.findItem(R.id.action_unfollow) == null) {
				if (!mFollowing) {
					mMenu.add(Menu.NONE, R.id.action_follow, Menu.NONE, R.string.follow)
							.setIcon(R.drawable.ic_title_follow)
							.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
				} else {
					mMenu.add(Menu.NONE, R.id.action_unfollow, Menu.NONE, R.string.unfollow)
							.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}
			}
		}
	}

	private void toggleFollow(final boolean follow) {
		mApp.getRequestQueue().add(new CatnutRequest(
				getActivity(),
				follow ? FriendshipsAPI.create(mScreenName, null) : FriendshipsAPI.destroy(mScreenName),
				new UserProcessor.UserProfileProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Toast.makeText(getActivity(),
								getString(follow ? R.string.follow_success : R.string.unfollow_success),
								Toast.LENGTH_SHORT).show();
						if (follow) {
							mMenu.removeItem(R.id.action_follow);
							mMenu.add(Menu.NONE, R.id.action_follow, Menu.NONE, R.string.unfollow)
									.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
						} else {
							mMenu.removeItem(R.id.action_unfollow);
							mMenu.add(Menu.NONE, R.id.action_follow, Menu.NONE, R.string.follow)
									.setIcon(R.drawable.ic_title_follow)
									.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
						}
						mFollowing = !mFollowing;
						// 更新本地数据库
						final String str = follow ? "+1" : "-1";
						new Thread(new Runnable() {
							@Override
							public void run() {
								long uid = mApp.getAccessToken().uid;
								String update = "update " + User.TABLE + " SET " + User.friends_count + "="
										+ User.friends_count + str + " WHERE " + BaseColumns._ID + "=" + uid;
								getActivity().getContentResolver().update(
										CatnutProvider.parse(User.MULTIPLE, uid),
										null,
										update,
										null
								);
							}
						}).start();
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
					}
				}
		));
	}

	private final PagerAdapter coverPager = new PagerAdapter() {

		private LayoutInflater mLayoutInflater;

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (mLayoutInflater == null) {
				mLayoutInflater = LayoutInflater.from(getActivity());
			}
			switch (position) {
				case 0:
					View frontPage = mLayoutInflater.inflate(R.layout.profile_cover, container, false);
					ImageView avatar = (ImageView) frontPage.findViewById(R.id.avatar);
					Picasso.with(getActivity())
							.load(mAvatarUrl)
							.placeholder(R.drawable.error)
							.error(R.drawable.error)
							.into(avatar);
					TextView screenName = (TextView) frontPage.findViewById(R.id.screen_name);
					screenName.setText("@" + mScreenName);
					TextView remark = (TextView) frontPage.findViewById(R.id.remark);
					// 如果说没有备注的话那就和微博id一样
					remark.setText(TextUtils.isEmpty(mRemark) ? mScreenName : mRemark);
					if (mVerified) {
						frontPage.findViewById(R.id.verified).setVisibility(View.VISIBLE);
					}
					container.addView(frontPage);
					return frontPage;
				case 1:
					View introPage = mLayoutInflater.inflate(R.layout.profile_intro, container, false);
					CatnutUtils.setText(introPage, R.id.description, TextUtils.isEmpty(mDescription)
							? getString(R.string.no_description) : mDescription);
					CatnutUtils.setText(introPage, R.id.location, mLocation);
					CatnutUtils.setText(introPage, R.id.profile_url, Constants.WEIBO_DOMAIN + mProfileUrl);
					container.addView(introPage);
					return introPage;
				default:
					return null;
			}
		}
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(getString(R.string.pref_show_latest_tweet))) {
			boolean show = mApp.getPreferences().getBoolean(key, true);
			// 如果已经显示那么隐藏，反之，如果根本就没有初始化数据，那么不会再去抓数据
			if (show) {
				mTweetLayout.setVisibility(View.VISIBLE);
			} else {
				mTweetLayout.setVisibility(View.GONE);
			}
		}
	}
}
