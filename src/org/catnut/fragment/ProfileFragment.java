/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
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
							Toast.makeText(getActivity(), getString(R.string.load_cover_fail), Toast.LENGTH_SHORT).show();
						}
					});
					// 我的微博
					mTweetsCount.setOnClickListener(tweetsOnclickListener);
					CatnutUtils.setText(mTweetsCount, android.R.id.text1,
							cursor.getString(cursor.getColumnIndex(User.statuses_count)));
					CatnutUtils.setText(mTweetsCount, android.R.id.text2, getString(R.string.tweets));
					// 关注我的
					CatnutUtils.setText(mFollowersCount, android.R.id.text1,
							cursor.getString(cursor.getColumnIndex(User.followers_count)));
					CatnutUtils.setText(mFollowersCount, android.R.id.text2, getString(R.string.followers));
					// 我关注的
					CatnutUtils.setText(mFollowingsCount, android.R.id.text1,
							cursor.getString(cursor.getColumnIndex(User.friends_count)));
					CatnutUtils.setText(mFollowingsCount, android.R.id.text2, getString(R.string.followings));
					// pager adapter
					mViewPager.setAdapter(new CoverPagerFragment(getFragmentManager()));
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

	private class CoverPagerFragment extends FragmentPagerAdapter {

		public CoverPagerFragment(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				default:
				case 0:
					return CoverImageFragment.getFragment(mAvatarUrl, mScreenName, mRemark, mVerified);
				case 1:
					return CoverIntroFragment.getFragment(mDescription, mLocation, mProfileUrl);
			}
		}

		@Override
		public int getCount() {
			return COVER_SIZE;
		}
	}

}
