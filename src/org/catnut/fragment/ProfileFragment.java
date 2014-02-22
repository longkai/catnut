/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.viewpagerindicator.LinePageIndicator;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.ui.ProfileActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;

/**
 * 用户信息界面
 *
 * @author longkai
 */
public class ProfileFragment extends Fragment {

	private static final int COVER_SIZE = 2;

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
			User.followers_count
	};

	private CatnutApp mApp;
	private Activity mActivity;

	private long mUid;
	private String mScreenName;
	private String mCoverUrl;
	private String mAvatarUrl;
	private boolean mVerified;
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
			activity.flipCard(UserTimeLineFragment.getFragment(mUid, mScreenName), null, true);
		}
	};

	private View.OnClickListener followersOnclickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ProfileActivity activity = (ProfileActivity) getActivity();
			activity.flipCard(FollowersFragment.getFragment(mScreenName), null, true);
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
		mActivity = activity;
		Bundle args = getArguments();
		mUid = args.getLong(Constants.ID);
		mScreenName = args.getString(User.screen_name);
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
		// 从本地抓取数据*_*
		String query = CatnutUtils.buildQuery(PROJECTION,
				User.screen_name + "=" + CatnutUtils.quote(mScreenName), User.TABLE, null, null, null);
		new AsyncQueryHandler(getActivity().getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					// 暂存元数据
					mAvatarUrl = cursor.getString(cursor.getColumnIndex(User.avatar_large));
					mVerified = CatnutUtils.getBoolean(cursor, User.verified);
					mRemark = cursor.getString(cursor.getColumnIndex(User.remark));
					mDescription = cursor.getString(cursor.getColumnIndex(User.description));
					mLocation = cursor.getString(cursor.getColumnIndex(User.location));
					mProfileUrl = cursor.getString(cursor.getColumnIndex(User.profile_url));
					mCoverUrl = cursor.getString(cursor.getColumnIndex(User.cover_image));
					// load封面图片
					mApp.getImageLoader().get(mCoverUrl, new ImageLoader.ImageListener() {
						@Override
						public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
							mPlaceHolder.setBackground(new BitmapDrawable(getResources(), response.getBitmap()));
						}

						@Override
						public void onErrorResponse(VolleyError error) {
							mPlaceHolder.setBackgroundResource(R.raw.default_cover);
//							Toast.makeText(getActivity(), getString(R.string.load_cover_fail), Toast.LENGTH_SHORT).show();
						}
					});
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
		// 接下来抓最新的一条微博，这里弄得稍微复杂了，以后设置一个偏好吧 todo
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
					CatnutUtils.setText(tweet, R.id.nick, getString(R.string.latest_statues))
							.setTextColor(getResources().getColor(R.color.actionbar_background));
					String tweetText = cursor.getString(cursor.getColumnIndex(Status.columnText));
					TweetTextView text = (TweetTextView) CatnutUtils.setText(tweet, R.id.text,
							new TweetImageSpan(getActivity()).getImageSpan(tweetText));
					Linkify.addLinks(text, TweetTextView.MENTION_PATTERN, TweetTextView.MENTION_SCHEME, null, TweetTextView.MENTION_FILTER);
					Linkify.addLinks(text, TweetTextView.TOPIC_PATTERN, TweetTextView.TOPIC_SCHEME, null, TweetTextView.TOPIC_FILTER);
					Linkify.addLinks(text, TweetTextView.WEB_URL, null, null, TweetTextView.URL_FILTER);
					CatnutUtils.removeLinkUnderline(text);

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
		return view;
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
					mApp.getImageLoader().get(mAvatarUrl,
							ImageLoader.getImageListener(avatar, R.drawable.error, R.drawable.error));
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
}
