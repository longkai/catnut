/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.*;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.adapter.CommentsAdapter;
import org.catnut.adapter.EmotionsAdapter;
import org.catnut.api.CommentsAPI;
import org.catnut.api.FavoritesAPI;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.StatusProcessor;
import org.catnut.support.OnFragmentBackPressedListener;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.ui.ComposeTweetActivity;
import org.catnut.ui.HelloActivity;
import org.catnut.ui.ProfileActivity;
import org.catnut.ui.SingleFragmentActivity;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * 微博界面
 *
 * @author longkai
 */
public class TweetFragment extends Fragment implements
		TextWatcher, OnFragmentBackPressedListener, PopupMenu.OnMenuItemClickListener,
		LoaderManager.LoaderCallbacks<Cursor>, OnRefreshListener, AdapterView.OnItemClickListener,
		AbsListView.OnScrollListener, AdapterView.OnItemLongClickListener {

	private static final String TAG = "TweetFragment";
	private static final String RETWEET_INDICATOR = ">"; // 标记转发
	private static final int COMMENT = 0;
	private static final int REPLY = 1;
	private static final int RETWEET = 2;

	private static final int MAX_SHOW_TOAST_TIME = 2;

	private Handler mHandler = new Handler();

	/** 回复待检索的列 */
	private static final String[] PROJECTION = new String[]{
			"s._id",
			Status.uid,
			Status.columnText,
			"s." + Status.created_at,
			User.screen_name,
			User.profile_image_url,
			User.remark
	};

	private RequestQueue mRequestQueue;
	private TweetImageSpan mImageSpan;
	private SharedPreferences mPreferences;
	private ConnectivityManager mConnectivityManager;
	private PullToRefreshLayout mPullToRefreshLayout;

	private String mSelection;

	private ListView mListView;
	private CommentsAdapter mAdapter;
	private EditText mSendText;
	private ImageView mSend;
	private TextView mTextCounter;
	private ImageView mOverflow;
	private PopupMenu mPopupMenu;

	// tweet id
	private long mId;
	private JSONObject mJson;
	// 共有多少条评论
	private int mTotal; // 注意，这个是时时更新的，很可能出现这样的情况：我已经滑倒低了，但是此时又有新的评论，那么，total增加了，但是我们真的已经到底了...所以还需要另一个变量来控制
	private int mLastTotalCount; // 两次一样表示，完了...
	// 是否收藏这条微博
	private boolean mFavorited = false;
	// 回复那个评论id
	private long mReplyTo = 0L;
	// 转发选项
	private int mRetweetOption = 0;
	// 提示没有更多了的次数，
	private int mShowToastTimes = 0;

	private String mPlainText; // 微博纯文本

	// widgets
	private View mTweetLayout;
	private ImageView mAvatar;
	private TextView mRemark;
	private TextView mScreenName;
	private TweetTextView mText;
	private TextView mReplayCount;
	private TextView mReteetCount;
	private TextView mFavoriteCount;
	private TextView mSource;
	private TextView mCreateAt;
	private ImageView mThumbs;
//	private View mRetweetLayout;
	private ViewStub mRetweetLayout;

	private Typeface mTypeface;
	private float mLineSpacing = 1.0f;

	// others
	private ShareActionProvider mShareActionProvider;
	private Intent mShareIntent;

	protected Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Log.d(TAG, "error loading data from cloud!", error);
			WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
			Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_LONG).show();
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	public static TweetFragment getFragment(long id) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, id);
		TweetFragment fragment = new TweetFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static TweetFragment getFragment(String tweet) {
		Bundle args = new Bundle();
		args.putString(Constants.JSON, tweet);
		TweetFragment fragment = new TweetFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public int getFetchSize() {
		return CatnutUtils.resolveListPrefInt(
				mPreferences,
				getString(R.string.pref_default_fetch_size),
				getResources().getInteger(R.integer.default_fetch_size)
		);
	}

	protected void refresh() {
		// 检测一下是否网络已经连接，否则从本地加载
		if (!isNetworkAvailable()) {
			Toast.makeText(getActivity(), getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
			initFromLocal();
			return;
		}
		// refresh!
		final int size = getFetchSize();
		(new Thread(new Runnable() {
			@Override
			public void run() {
				// 这里需要注意一点，我们不需要最新的那条，而是需要(最新那条-数目)，否则你拿最新那条去刷新，球都没有返回Orz...
				String query = CatnutUtils.buildQuery(
						new String[]{BaseColumns._ID},
						mSelection,
						Status.TABLE,
						null,
						BaseColumns._ID + " desc",
						size + ", 1" // limit x, y
				);
				Cursor cursor = getActivity().getContentResolver().query(
						CatnutProvider.parse(Status.MULTIPLE),
						null, query, null, null
				);
				// the cursor never null?
				final long since_id;
				if (cursor.moveToNext()) {
					since_id = cursor.getLong(0);
				} else {
					since_id = 0;
				}
				cursor.close();
				final CatnutAPI api = CommentsAPI.show(mId, since_id, 0, size, 0, 0);
				// refresh...
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mRequestQueue.add(new CatnutRequest(
								getActivity(),
								api,
								new StatusProcessor.CommentTweetsProcessor(mId),
								new Response.Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										Log.d(TAG, "refresh done...");
										mTotal = response.optInt(Status.total_number);
										// 重新置换数据
										mLastTotalCount = 0;
										mShowToastTimes = 0;
										JSONArray jsonArray = response.optJSONArray(Status.COMMENTS);
										int newSize = jsonArray.length(); // 刷新，一切从新开始...
										Bundle args = new Bundle();
										args.putInt(TAG, newSize);
										getLoaderManager().restartLoader(0, args, TweetFragment.this);
									}
								},
								errorListener
						)).setTag(TAG);
					}
				});
			}
		})).start();
	}

	private void initFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, getFetchSize());
		getLoaderManager().initLoader(0, args, this);
		new Thread(updateLocalCount).start();
	}

	private Runnable updateLocalCount = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(
					new String[]{"count(0)"},
					mSelection,
					Status.TABLE,
					null, null, null
			);
			Cursor cursor = getActivity().getContentResolver().query(
					CatnutProvider.parse(Status.MULTIPLE),
					null,
					query,
					null, null
			);
			if (cursor.moveToNext()) {
				mTotal = cursor.getInt(0);
			}
			cursor.close();
		}
	};

	protected void loadMore(long max_id) {
		// 加载更多，判断一下是从本地加载还是从远程加载
		// 根据(偏好||是否有网络连接)
		boolean fromCloud = mPreferences.getBoolean(
				getString(R.string.pref_keep_latest),
				getResources().getBoolean(R.bool.pref_load_more_from_cloud)
		);
		if (fromCloud && isNetworkAvailable()) {
			// 如果用户要求最新的数据并且网络连接ok，那么从网络上加载数据
			loadFromCloud(max_id);
		} else {
			// 从本地拿
			loadFromLocal();
			// 顺便更新一下本地的数据总数
			new Thread(updateLocalCount).start();
		}
	}

	private void loadFromLocal() {
		Bundle args = new Bundle();
		mLastTotalCount = mAdapter.getCount(); // 暂存一下
		args.putInt(TAG, mAdapter.getCount() + getFetchSize());
		getLoaderManager().restartLoader(0, args, this);
		mPullToRefreshLayout.setRefreshing(true);
	}

	private void loadFromCloud(long max_id) {
		mPullToRefreshLayout.setRefreshing(true);
		CatnutAPI api = CommentsAPI.show(mId, 0, max_id, getFetchSize(), 0, 0);
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				api,
				new StatusProcessor.CommentTweetsProcessor(mId),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG, "load more from cloud done...");
						mTotal = response.optInt(Status.total_number);
						mLastTotalCount = mAdapter.getCount();
						int newSize = response.optJSONArray(Status.COMMENTS).length() + mAdapter.getCount();
						Bundle args = new Bundle();
						args.putInt(TAG, newSize);
						getLoaderManager().restartLoader(0, args, TweetFragment.this);
					}
				},
				errorListener
		)).setTag(TAG);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Bundle args = getArguments();
		if (args.containsKey(Constants.JSON)) {
			try {
				mJson = new JSONObject(args.getString(Constants.JSON));
				mId = mJson.optLong(Constants.ID);
			} catch (JSONException e) {
				Log.e(TAG, "malformed json!", e);
				Toast.makeText(activity, activity.getString(R.string.malformed_json), Toast.LENGTH_LONG).show();
				// getActivity().onBackPressed();
			}
		} else {
			mId = args.getLong(Constants.ID);
		}
		mSelection = new StringBuilder(Status.TYPE)
				.append("=").append(Status.COMMENT)
				.append(" and ").append(Status.TO_WHICH_TWEET)
				.append("=").append(mId).toString();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		CatnutApp app = CatnutApp.getTingtingApp();
		mRequestQueue = app.getRequestQueue();
		mPreferences = app.getPreferences();
		mTypeface = CatnutUtils.getTypeface(
				mPreferences,
				getString(R.string.pref_customize_tweet_font),
				getString(R.string.default_typeface)
		);
		mLineSpacing = CatnutUtils.getLineSpacing(mPreferences,
				getString(R.string.pref_line_spacing), getString(R.string.default_line_spacing));
		mImageSpan = new TweetImageSpan(getActivity());
		mAdapter = new CommentsAdapter(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.comments, null, false);
		mListView = (ListView) view.findViewById(android.R.id.list);
		mSendText = (EditText) view.findViewById(R.id.action_reply);
		mSend = (ImageView) view.findViewById(R.id.action_send);
		mTextCounter = (TextView) view.findViewById(R.id.text_counter);
		mOverflow = (ImageView) view.findViewById(R.id.action_overflow);
		mPopupMenu = new PopupMenu(getActivity(), mOverflow);
		// for our headers
		mTweetLayout = inflater.inflate(R.layout.tweet, null);
		mAvatar = (ImageView) mTweetLayout.findViewById(R.id.avatar);
		mRemark = (TextView) mTweetLayout.findViewById(R.id.remark);
		mScreenName = (TextView) mTweetLayout.findViewById(R.id.screen_name);
		mText = (TweetTextView) mTweetLayout.findViewById(R.id.text);
		mReplayCount = (TextView) mTweetLayout.findViewById(R.id.reply_count);
		mReteetCount = (TextView) mTweetLayout.findViewById(R.id.reteet_count);
		mFavoriteCount = (TextView) mTweetLayout.findViewById(R.id.like_count);
		mSource = (TextView) mTweetLayout.findViewById(R.id.source);
		mCreateAt = (TextView) mTweetLayout.findViewById(R.id.create_at);
		mThumbs = (ImageView) mTweetLayout.findViewById(R.id.thumbs);
		mRetweetLayout = (ViewStub) mTweetLayout.findViewById(R.id.view_stub);
		// just return the list
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		ViewGroup viewGroup = (ViewGroup) view;
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
		ActionBarPullToRefresh.from(getActivity())
				.insertLayoutInto(viewGroup)
				.theseChildrenArePullable(android.R.id.list, android.R.id.empty)
				.listener(this)
				.setup(mPullToRefreshLayout);
		mSendText.addTextChangedListener(this);
		mSendText.setTextColor(getResources().getColor(android.R.color.primary_text_light));
		mSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 发送评论！
				send();
			}
		});
		mPopupMenu.inflate(R.menu.comment_overflow);
		mPopupMenu.setOnMenuItemClickListener(this);
		mOverflow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPopupMenu.show();
			}
		});
		// 载入评论
		mPullToRefreshLayout.setRefreshing(true);
		if (savedInstanceState == null) {
			if (mPreferences.getBoolean(getString(R.string.pref_keep_latest), true)) {
				refresh();
			} else {
				initFromLocal();
			}
		} else {
			initFromLocal();
		}
		// 载入微博/转发微博
		if (mJson == null) {
			loadTweet();
		} else {
			loadRetweet();
		}
	}

	// 载入那条微博
	private void loadTweet() {
		// load tweet from local...
		String query = CatnutUtils.buildQuery(
				new String[]{
						Status.uid,
						Status.columnText,
						Status.bmiddle_pic,
						Status.original_pic,
						Status.comments_count,
						Status.reposts_count,
						Status.attitudes_count,
						Status.source,
						Status.favorited,
						Status.retweeted_status,
						"s." + Status.created_at,
						User.screen_name,
						User.avatar_large,
						User.remark,
						User.verified
				},
				"s._id=" + mId,
				Status.TABLE + " as s",
				"inner join " + User.TABLE + " as u on s.uid=u._id",
				null,
				null
		);
		new AsyncQueryHandler(getActivity().getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					Picasso.with(getActivity())
							.load(cursor.getString(cursor.getColumnIndex(User.avatar_large)))
							.placeholder(R.drawable.error)
							.error(R.drawable.error)
							.into(mAvatar);
					final long uid = cursor.getLong(cursor.getColumnIndex(Status.uid));
					final String screenName = cursor.getString(cursor.getColumnIndex(User.screen_name));
					mAvatar.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(getActivity(), ProfileActivity.class);
							intent.putExtra(Constants.ID, uid);
							intent.putExtra(User.screen_name, screenName);
							startActivity(intent);
						}
					});
					String remark = cursor.getString(cursor.getColumnIndex(User.remark));
					mRemark.setText(TextUtils.isEmpty(remark) ? screenName : remark);
					mScreenName.setText(getString(R.string.mention_text, screenName));
					mPlainText = cursor.getString(cursor.getColumnIndex(Status.columnText));
					mText.setText(mPlainText);
					CatnutUtils.vividTweet(mText, mImageSpan);
					CatnutUtils.setTypeface(mText, mTypeface);
					mText.setLineSpacing(0, mLineSpacing);
					int replyCount = cursor.getInt(cursor.getColumnIndex(Status.comments_count));
					mReplayCount.setText(CatnutUtils.approximate(replyCount));
					int retweetCount = cursor.getInt(cursor.getColumnIndex(Status.reposts_count));
					mReteetCount.setText(CatnutUtils.approximate(retweetCount));
					int favoriteCount = cursor.getInt(cursor.getColumnIndex(Status.attitudes_count));
					mFavoriteCount.setText(CatnutUtils.approximate(favoriteCount));
					String source = cursor.getString(cursor.getColumnIndex(Status.source));
					mSource.setText(Html.fromHtml(source).toString());
					mCreateAt.setText(DateUtils.getRelativeTimeSpanString(
							DateTime.getTimeMills(cursor.getString(cursor.getColumnIndex(Status.created_at)))));
					if (CatnutUtils.getBoolean(cursor, User.verified)) {
						mTweetLayout.findViewById(R.id.verified).setVisibility(View.VISIBLE);
					}
					String thumb = cursor.getString(cursor.getColumnIndex(Status.bmiddle_pic));
					String url = cursor.getString(cursor.getColumnIndex(Status.original_pic));
					loadThumbs(thumb, url);
					// retweet
					final String jsonString = cursor.getString(cursor.getColumnIndex(Status.retweeted_status));
					if (!TextUtils.isEmpty(jsonString)) {
						View retweet = mRetweetLayout.inflate();
						try {
							JSONObject json = new JSONObject(jsonString);
							JSONObject user = json.optJSONObject(User.SINGLE);
							String _remark = user.optString(User.remark);
							if (TextUtils.isEmpty(_remark)) {
								_remark = user.optString(User.screen_name);
							}
							CatnutUtils.setText(retweet, R.id.retweet_nick, getString(R.string.mention_text, _remark));
							long mills = DateTime.getTimeMills(json.optString(Status.created_at));
							CatnutUtils.setText(retweet, R.id.retweet_create_at, DateUtils.getRelativeTimeSpanString(mills));
							TweetTextView retweetText = (TweetTextView) CatnutUtils.setText(retweet, R.id.retweet_text, json.optString(Status.text));
							CatnutUtils.vividTweet(retweetText, mImageSpan);
							CatnutUtils.setTypeface(retweetText, mTypeface);
							retweetText.setLineSpacing(0, mLineSpacing);
							retweet.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent intent = new Intent(getActivity(), TweetActivity.class);
									intent.putExtra(Constants.JSON, jsonString);
									startActivity(intent);
								}
							});
						} catch (JSONException e) {
							Log.e(TAG, "convert text to string error!", e);
							retweet.setVisibility(View.GONE);
						}
					}
					// shareAndFavorite&favorite
					shareAndFavorite(CatnutUtils.getBoolean(cursor, Status.favorited), mPlainText);
				}
				cursor.close();
			}
		}.startQuery(0, null, CatnutProvider.parse(Status.MULTIPLE), null, query, null, null);
	}

	// 载入转发微博
	private void loadRetweet() {
		JSONObject user = mJson.optJSONObject(User.SINGLE);
		Picasso.with(getActivity())
				.load(user == null ? Constants.NULL : user.optString(User.avatar_large))
				.placeholder(R.drawable.error)
				.error(R.drawable.error)
				.into(mAvatar);
		final long uid = mJson.optLong(Status.uid);
		final String screenName = user == null ? getString(R.string.unknown_user) : user.optString(User.screen_name);
		mAvatar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), ProfileActivity.class);
				intent.putExtra(Constants.ID, uid);
				intent.putExtra(User.screen_name, screenName);
				startActivity(intent);
			}
		});
		String remark = user.optString(User.remark);
		mRemark.setText(TextUtils.isEmpty(remark) ? screenName : remark);
		mScreenName.setText(getString(R.string.mention_text, screenName));
		mPlainText = mJson.optString(Status.text);
		mText.setText(mPlainText);
		CatnutUtils.vividTweet(mText, mImageSpan);
		CatnutUtils.setTypeface(mText, mTypeface);
		mText.setLineSpacing(0, mLineSpacing);
		int replyCount = mJson.optInt(Status.comments_count);
		mReplayCount.setText(CatnutUtils.approximate(replyCount));
		int retweetCount = mJson.optInt(Status.reposts_count);
		mReteetCount.setText(CatnutUtils.approximate(retweetCount));
		int favoriteCount = mJson.optInt(Status.attitudes_count);
		mFavoriteCount.setText(CatnutUtils.approximate(favoriteCount));
		String source = mJson.optString(Status.source);
		mSource.setText(Html.fromHtml(source).toString());
		mCreateAt.setText(DateUtils.getRelativeTimeSpanString(
				DateTime.getTimeMills(mJson.optString(Status.created_at))));
		if (user.optBoolean(User.verified)) {
			mTweetLayout.findViewById(R.id.verified).setVisibility(View.VISIBLE);
		}

		loadThumbs(mJson.optString(Status.bmiddle_pic), mJson.optString(Status.original_pic));
		shareAndFavorite(mJson.optBoolean(Status.favorited), mJson.optString(Status.text));

		if (!mJson.has(Status.retweeted_status)) {
			//todo: 转发再转发貌似没什么意思
		}
	}

	/**
	 * 载入微博缩略图并监听用户查看原图
	 *
	 * @param thumb 缩略图url
	 * @param originalUrl 原图url
	 */
	private void loadThumbs(String thumb, final String originalUrl) {
		if (!TextUtils.isEmpty(thumb)) {
			Picasso.with(getActivity()).load(thumb).into(mThumbs);
			mThumbs.setVisibility(View.VISIBLE);
			mThumbs.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = SingleFragmentActivity.getIntent(getActivity(),
							SingleFragmentActivity.PHOTO_VIEWER);
					intent.putExtra(Constants.PIC, originalUrl);
					startActivity(intent);
				}
			});
		} else {
			mThumbs.setVisibility(View.GONE);
		}
	}

	/**
	 * 分享与收藏/取消收藏
	 *
	 * @param favorited 是否已经收藏/取消这条微博
	 * @param text 微博文字
	 */
	private void shareAndFavorite(boolean favorited, String text) {
		mShareIntent = new Intent(Intent.ACTION_SEND).setType(getString(R.string.mime_text_plain));
		mShareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.tweet_share_subject));
		mShareIntent.putExtra(Intent.EXTRA_TEXT, text);
		if (mShareActionProvider != null) {
			mShareActionProvider.setShareIntent(mShareIntent);
		}
		mFavorited = favorited;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getString(R.string.tweet));
	}

	@Override
	public void onStop() {
		super.onStop();
		mRequestQueue.cancelAll(TAG);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListView.addHeaderView(mTweetLayout);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		mListView.setOnScrollListener(this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int limit = args.getInt(TAG, getFetchSize());
		return CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(Status.MULTIPLE),
				PROJECTION,
				mSelection,
				null,
				Status.TABLE + " as s",
				"inner join " + User.TABLE + " as u on s.uid=u._id",
				"s." + BaseColumns._ID + " desc",
				String.valueOf(limit)
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (mPullToRefreshLayout.isRefreshing()) {
			mPullToRefreshLayout.setRefreshComplete();
		}
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onRefreshStarted(View view) {
		refresh();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		boolean canLoading = SCROLL_STATE_IDLE == scrollState // 停住了，不滑动了
				&& (mListView.getLastVisiblePosition() - 1) == (mAdapter.getCount() - 1) // 到底了，这里有一个header！
				&& !mPullToRefreshLayout.isRefreshing(); // 当前没有处在刷新状态
//				&& mAdapter.getCount() > 0; // 不是一开始
		if (canLoading) {
			// 可以加载更多，但是我们需要判断一下是否加载完了，没有更多了
			if (mAdapter.getCount() >= mTotal || mLastTotalCount == mAdapter.getCount()) {
				Log.d(TAG, "load all done...");
//				if (mShowToastTimes < MAX_SHOW_TOAST_TIME) {
//					Toast.makeText(getActivity(), R.string.no_more, Toast.LENGTH_SHORT).show();
//					mShowToastTimes++;
//				}
			} else {
				Log.d(TAG, "load...");
				loadMore(mAdapter.getItemId(mAdapter.getCount() - 1));
			}
		} else {
			Log.d(TAG, "cannot load more!");
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// no-op
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
		if (CatnutUtils.hasLength(mSendText)) {
			confirmAbortEdit(new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mSendText.setText(null);
					intentToReply(position, id);
				}
			});
		} else {
			intentToReply(position, id);
		}
	}

	// 确认一下是否需要放弃修改
	private void confirmAbortEdit(DialogInterface.OnClickListener abortListener) {
		new AlertDialog.Builder(getActivity())
				.setMessage(R.string.abort_existing_reply_alert)
				.setPositiveButton(android.R.string.ok, abortListener)
				.setNegativeButton(android.R.string.no, null)
				.show();
	}

	/**
	 * 想要去回复某个评论?
	 *
	 * @param position
	 */
	private void intentToReply(int position, long id) {
		// 清除text，调用之前最好询问一下用户
		mSendText.setText(null);

		Cursor cursor = (Cursor) mAdapter.getItem(position - 1); // a header view in top...
		String screenName = cursor.getString(cursor.getColumnIndex(User.screen_name));
		// 标记一下
		mReplyTo = id;
		mSendText.requestFocus();
		mSendText.setHint(getString(R.string.reply_comment_hint, screenName));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.tweet, menu);
		MenuItem share = menu.findItem(R.id.share);
		mShareActionProvider = (ShareActionProvider) share.getActionProvider();
		mShareActionProvider.setShareIntent(mShareIntent);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem fav = menu.findItem(R.id.action_toggle_favorite);
		if (mFavorited) {
			fav.setTitle(getString(R.string.cancle_favorite)).setIcon(R.drawable.ic_title_decline);
		} else {
			fav.setTitle(getString(R.string.favorite)).setIcon(R.drawable.ic_title_favorite);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_toggle_favorite:
				toggleFavorite();
				break;
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(getActivity(), SingleFragmentActivity.PREF));
				break;
			case R.id.action_compose:
				startActivity(new Intent(getActivity(), ComposeTweetActivity.class));
				break;
			case R.id.action_comment:
				// 确认放弃修改
				// never be null but empty
				if (CatnutUtils.hasLength(mSendText)) {
					confirmAbortEdit(new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// 重置
							mReplyTo = 0L;
							resetEditor(getString(R.string.comment_tweet), false);
						}
					});
				} else {
					mReplyTo = 0L; // 这句其实无所谓
					resetEditor(getString(R.string.comment_tweet), false);
				}
				break;
			case R.id.action_reteet:
				if (CatnutUtils.hasLength(mSendText)) {
					confirmAbortEdit(new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							resetEditor(RETWEET_INDICATOR + getString(R.string.retweet), true);
						}
					});
				} else {
					resetEditor(RETWEET_INDICATOR + getString(R.string.retweet), true);
				}
				break;
			case R.id.fantasy:
				startActivity(new Intent(getActivity(), HelloActivity.class).putExtra(HelloActivity.TAG, HelloActivity.TAG));
				break;
			case android.R.id.copy:
				CatnutUtils.copy2ClipBoard(getActivity(), getString(R.string.tweet), mPlainText, getString(R.string.tweet_text_copied));
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void resetEditor(String hint, boolean sendEnabled) {
		mSendText.setText(null);
		mSendText.setHint(hint);
		mSendText.requestFocus();
		mSend.setImageResource(sendEnabled ? R.drawable.ic_dm_send_default : R.drawable.ic_dm_send_disabled);
		mSend.setClickable(sendEnabled);
	}

	private void toggleFavorite() {
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				mFavorited ? FavoritesAPI.destroy(mId) : FavoritesAPI.create(mId),
				new StatusProcessor.FavoriteTweetProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Toast.makeText(getActivity(),
								mFavorited ? R.string.cancle_favorite_success
										: R.string.favorite_success, Toast.LENGTH_SHORT).show();
						mFavorited = !mFavorited;
						// 更新一下当前ui
						JSONObject status = response.optJSONObject(Status.SINGLE);
						mReplayCount.setText(CatnutUtils.approximate(status.optInt(Status.comments_count)));
						mReteetCount.setText(CatnutUtils.approximate(status.optInt(Status.reposts_count)));
						mFavoriteCount.setText(CatnutUtils.approximate(status.optInt(Status.attitudes_count)));
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
						Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_SHORT).show();
					}
				}
		)).setTag(TAG);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// no-op
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// no-op
	}

	@Override
	public void afterTextChanged(Editable s) {
		int count = 140 - mSendText.length();
		mTextCounter.setText(String.valueOf(count));
		mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
		// never lt 140, 'cause the edit text' s max length is 140
		if (count != 140) {
			mSend.setClickable(true);
			mSend.setFocusable(true);
			mSend.setImageResource(R.drawable.ic_dm_send_default);
		} else {
			mSend.setClickable(false);
			mSend.setFocusable(false);
			mSend.setImageResource(R.drawable.ic_dm_send_disabled);
		}
	}

	// 发送评论
	private void send() {
		// 简单的通过hint来判断是啥类型
		final int type;
		String hint = mSendText.getHint().toString();
		if (hint.startsWith(RETWEET_INDICATOR)) {
			type = RETWEET;
		} else {
			type = hint.contains("@") ? REPLY : COMMENT;
		}
		if (type != RETWEET) { // 转发允许啥都不写...
			if (!CatnutUtils.hasLength(mSendText)) {
				Toast.makeText(getActivity(), getString(R.string.require_not_empty), Toast.LENGTH_SHORT).show();
				return; // 提前结束
			}
		}
		mSend.setBackgroundResource(R.drawable.ic_dm_send_disabled);
		mSend.setClickable(false);
		String text = mSendText.getText().toString();
		CatnutAPI api;
		switch (type) {
			default:
			case COMMENT:
				api = CommentsAPI.create(text, mId, 0, null);
				break;
			case REPLY:
				api = CommentsAPI.reply(mReplyTo, mId, text, 0, 0, null);
				break;
			case RETWEET:
				api = TweetAPI.repost(mId, text, mRetweetOption, null);
				break;
		}
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				api,
				type == RETWEET
						? new StatusProcessor.SingleTweetProcessor(Status.RETWEET)
						: new StatusProcessor.CommentTweetProcessor(mId), // 这里共用一个了
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						// 删除刚才编辑的内容
						mSendText.setText(null);
						mTotal++;
						String msg;
						switch (type) {
							default:
							case COMMENT:
								msg = getString(R.string.comment_success);
								break;
							case REPLY:
								msg = getString(R.string.reply_success);
								break;
							case RETWEET:
								msg = getString(R.string.retweet_success);
								mSend.setImageResource(R.drawable.ic_dm_send_default);
								mSend.setClickable(true);
								break;
						}
						Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
						// 更新ui
						TextView which =
								type == RETWEET ? mReteetCount : mReplayCount;
						String before = which.getText().toString();
						if (TextUtils.isEmpty(before)) {
							which.setText("1"); // 小心，这里不要写数字，因为R.string.xx，很容易犯错
						} else {
							which.setText(String.valueOf(Integer.parseInt(before) + 1));
						}
						// list会自动更新，但是由于limit的限制，尾部的一条评论会从list中去掉...
						int pos = mListView.getFirstVisiblePosition();
						mLastTotalCount = mAdapter.getCount();
						Bundle args = new Bundle();
						args.putInt(TAG, mLastTotalCount + 1);
						getLoaderManager().restartLoader(0, args, TweetFragment.this);
						mListView.setSelection(pos);
					}
				},
				sendErrorListener
		)).setTag(TAG);
	}

	// 来个默认的回复错误监听吧
	private Response.ErrorListener sendErrorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
			Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_SHORT).show();
		}
	};


	@Override
	public void onBackPressed() {
		if (CatnutUtils.hasLength(mSendText)) {
			confirmAbortEdit(new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getActivity().onBackPressed();
				}
			});
		} else {
			getActivity().onBackPressed();
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// first check emotions
		if (item.getItemId() == R.id.action_emotions) {
			GridView emotions = (GridView) LayoutInflater.from(getActivity()).inflate(R.layout.emotions, null);
			emotions.setAdapter(new EmotionsAdapter(getActivity()));
			emotions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					int cursor = mSendText.getSelectionStart();
					mSendText.getText().insert(cursor, CatnutUtils.text2Emotion(getActivity(),
							TweetImageSpan.EMOTION_KEYS[position]));
					mSendText.requestFocus();
				}
			});
			AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
					.setView(emotions).create();
			alertDialog.show();
			alertDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					getResources().getDimensionPixelSize(R.dimen.emotion_window_height));
			return true;
		}
		switch (item.getItemId()) {
			case R.id.action_reply_none:
				mRetweetOption = 0;
				break;
			case R.id.action_reply_current:
				mRetweetOption = 1;
				break;
			case R.id.action_reply_original:
				mRetweetOption = 2;
				break;
			case R.id.action_reply_both:
				mRetweetOption = 3;
				break;
			default:
				break;
		}
		if (!item.isChecked()) {
			item.setChecked(true);
		}
		return true;
	}

	public boolean isNetworkAvailable() {
		NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		return activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position - 1);
		String text = cursor.getString(cursor.getColumnIndex(Status.columnText));
		CatnutUtils.copy2ClipBoard(getActivity(), getString(R.string.tweet), text, getString(R.string.tweet_text_copied));
		return true;
	}
}
