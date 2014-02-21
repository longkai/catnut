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
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONObject;

/**
 * 用户信息
 *
 * @author longkai
 */
public class ProfileFragment extends Fragment {

	private static final String TAG = "ProfileFragment";

	private Handler mHandler = new Handler();
	private CatnutApp mApp;

	private long mUid;
	private String mScreenName;

	private ImageView mAvatar;
	private TextView mNick;
	private TextView mDescription;
	private View mTweetsCount;
	private View mFollowingsCount;
	private View mFollowersCount;
	private View mTweetLayout;
	private ListView mList;

	private Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	};

	private class AsyncProfileQueryHandler extends AsyncQueryHandler {

		public AsyncProfileQueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			if (cursor.moveToNext()) {
				mApp.getImageLoader().get(
						cursor.getString(cursor.getColumnIndex(User.avatar_large)),
						ImageLoader.getImageListener(mAvatar, R.drawable.error, R.drawable.error)
				);
				mNick.setText(mScreenName);
				mDescription.setText(cursor.getString(cursor.getColumnIndex(User.description)));
				// 微博数
				mTweetsCount.setOnClickListener(onTweetClickListener);
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
			}
			cursor.close();
		}
	}

	;

	private class AsyncTweetQueryHandler extends AsyncQueryHandler {

		public AsyncTweetQueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			if (cursor.moveToNext()) {
				ViewStub stub = (ViewStub) mTweetLayout.findViewById(R.id.latest_tweet);
				mTweetLayout.setOnClickListener(onTweetClickListener);
				View tweet = stub.inflate();
				CatnutUtils.setText(tweet, R.id.nick, getString(R.string.latest_statues))
						.setTextColor(R.color.actionbar_background);

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
	}

	;

	/**
	 * @param screenName 必填
	 * @param uid        可选为0
	 */
	public static ProfileFragment getInstance(String screenName, long uid) {
		Bundle args = new Bundle();
		args.putString(TAG, screenName); // 随便找的名字...
		args.putLong(Constants.ID, uid);
		ProfileFragment fragment = new ProfileFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mApp = CatnutApp.getTingtingApp();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mScreenName = getArguments().getString(TAG);
		mUid = getArguments().getLong(Constants.ID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.profile, container, false);
		mAvatar = (ImageView) view.findViewById(R.id.avatar_profile);
		mNick = (TextView) view.findViewById(R.id.nick);
		mDescription = (TextView) view.findViewById(R.id.description);
		mTweetsCount = view.findViewById(R.id.tweets_count);
		mFollowingsCount = view.findViewById(R.id.following_count);
		mFollowersCount = view.findViewById(R.id.followers_count);
		mTweetLayout = view.findViewById(R.id.tweet_layout);
		mList = (ListView) view.findViewById(android.R.id.list);
		String query1 = CatnutUtils.buildQuery(
				new String[]{
						User.screen_name,
						User.avatar_large,
						User.description,
						User.statuses_count,
						User.followers_count,
						User.friends_count
				},
				User.screen_name + "='" + mScreenName + "'",
				User.TABLE,
				null,
				null,
				null
		);
		new AsyncProfileQueryHandler(getActivity().getContentResolver())
				.startQuery(
						0,
						null,
						CatnutProvider.parse(User.MULTIPLE),
						null,
						query1,
						null,
						null
				);
		if (mUid != 0) {
			String query = CatnutUtils.buildQuery(
					new String[]{
							Status.columnText,
							Status.thumbnail_pic,
							Status.comments_count,
							Status.reposts_count,
							Status.attitudes_count,
							Status.source,
							Status.created_at,
					},
					"uid=(select _id from " + User.TABLE + " where " + User.screen_name + "='" + mScreenName + "')",
					Status.TABLE,
					null,
					BaseColumns._ID + " desc",
					"1"
			);
			new AsyncTweetQueryHandler(getActivity().getContentResolver())
					.startQuery(
							0,
							null,
							CatnutProvider.parse(Status.MULTIPLE),
							null,
							query,
							null,
							null
					);
		}
		return view;
	}

	private View.OnClickListener onTweetClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Bundle args = new Bundle();
			args.putLong(Constants.ID, mUid);
			args.putString(User.screen_name, mScreenName);
			UserTimeLineFragment fragment = new UserTimeLineFragment();
			fragment.setArguments(args);
			FragmentManager fragmentManager = getActivity().getFragmentManager();
			fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
				@Override
				public void onBackStackChanged() {
					getActivity().invalidateOptionsMenu();
				}
			});
			fragmentManager
					.beginTransaction()
					.setCustomAnimations(
							R.animator.card_flip_right_in, R.animator.card_flip_right_out,
							R.animator.card_flip_left_in, R.animator.card_flip_left_out)
					.replace(android.R.id.content, fragment)
					.addToBackStack(null)
					.commit();
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					getActivity().invalidateOptionsMenu();
				}
			});
		}
	};

}
