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
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
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
import com.android.volley.toolbox.ImageLoader;
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
import org.catnut.ui.ProfileActivity;
import org.catnut.ui.SingleFragmentActivity;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
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
public class TweetFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
		OnRefreshListener, AbsListView.OnScrollListener, AdapterView.OnItemClickListener, TextWatcher, OnFragmentBackPressedListener, PopupMenu.OnMenuItemClickListener {

	private static final String TAG = "TweetFragment";
	private static final String RETWEET_INDICATOR = ">"; // 标记转发
	private static final int COMMENT = 0;
	private static final int REPLY = 1;
	private static final int RETWEET = 2;


	private static final int BATCH_SIZE = 50;

	/** 回复待检索的列 */
	private static final String[] PROJECTION = new String[]{
			"s._id",
			Status.uid,
			Status.columnText,
//			Status.source,
			"s." + Status.created_at,
			User.screen_name,
			User.profile_image_url,
			User.remark
	};

	private RequestQueue mRequestQueue;
	private ImageLoader mImageLoader;
	private TweetImageSpan mImageSpan;

	private PullToRefreshLayout mPullToRefreshLayout;
	private int mCurrentPage = -1;
	private int mTotalSize = 0;
	private long mMaxId = 0; // 加载更多使用
	private ProgressBar mLoadMore;

	private ListView mListView;
	private CommentsAdapter mAdapter;
	private EditText mSendText;
	private ImageView mSend;
	private TextView mTextCounter;
	private ImageView mOverflow;
	private PopupMenu mPopupMenu;

	// tweet id
	private long mId;
	// 是否收藏这条微博
	private boolean mFavorited = false;
	// 回复那个评论id
	private long mReplyTo = 0L;
	// 转发选项
	private int mRetweetOption = 0;

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
	private View mRetweetLayout;

	// others
	private ShareActionProvider mShareActionProvider;
	private Intent mShareIntent;

	private Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			mTotalSize = response.optInt(Status.total_number);
			if (mTotalSize == 0) {
				mListView.removeFooterView(mLoadMore);
			}
			mCurrentPage++;
			getLoaderManager().restartLoader(0, null, TweetFragment.this);
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			mPullToRefreshLayout.setRefreshComplete();
			Toast.makeText(getActivity(), WeiboAPIError.fromVolleyError(error).error, Toast.LENGTH_SHORT).show();
		}
	};

	public static TweetFragment getFragment(long id) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, id);
		TweetFragment fragment = new TweetFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		CatnutApp app = CatnutApp.getTingtingApp();
		mImageLoader = app.getImageLoader();
		mRequestQueue = app.getRequestQueue();
		mImageSpan = new TweetImageSpan(activity);
		mId = getArguments().getLong(Constants.ID);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new CommentsAdapter(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.comments, container, false);
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
		mFavoriteCount = (TextView) mTweetLayout.findViewById(R.id.favorite_count);
		mSource = (TextView) mTweetLayout.findViewById(R.id.source);
		mCreateAt = (TextView) mTweetLayout.findViewById(R.id.create_at);
		mThumbs = (ImageView) mTweetLayout.findViewById(R.id.thumbs);
		mRetweetLayout = mTweetLayout.findViewById(R.id.place_holder);
		// just return the list
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mSendText.addTextChangedListener(this);
		mSendText.setTextColor(getResources().getColor(android.R.color.primary_text_light));
		mSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 发送评论！
				send();
			}
		});
		mPopupMenu.inflate(R.menu.tweet_overflow);
		mPopupMenu.setOnMenuItemClickListener(this);
		mOverflow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPopupMenu.show();
			}
		});
		mLoadMore = new ProgressBar(getActivity());
		// set actionbar refresh facility
		ViewGroup viewGroup = (ViewGroup) view;
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
		ActionBarPullToRefresh.from(getActivity())
				.insertLayoutInto(viewGroup)
				.theseChildrenArePullable(android.R.id.list, android.R.id.empty)
				.listener(this)
				.setup(mPullToRefreshLayout);
		// load comment from cloud
		mPullToRefreshLayout.setRefreshing(true);
		loadComments(false); // first time call, no need to reinitialize the metadata
		// load from local...
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
					mImageLoader.get(cursor.getString(cursor.getColumnIndex(User.avatar_large)),
							ImageLoader.getImageListener(mAvatar, R.drawable.error, R.drawable.error));
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
					mScreenName.setText("@" + screenName);
					String text = cursor.getString(cursor.getColumnIndex(Status.columnText));
					mText.setText(text);
					CatnutUtils.vividTweet(mText, mImageSpan);
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
					if (!TextUtils.isEmpty(thumb)) {
						Picasso.with(getActivity()).load(thumb).into(mThumbs);
						mThumbs.setVisibility(View.VISIBLE);
						mThumbs.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								Log.d(TAG, "todo");
							}
						});
					} else {
						mThumbs.setVisibility(View.GONE);
					}
					// retweet
					String jsonString = cursor.getString(cursor.getColumnIndex(Status.retweeted_status));
					if (!TextUtils.isEmpty(jsonString)) {
						try {
							final JSONObject json = new JSONObject(jsonString);
							JSONObject user = json.optJSONObject(User.SINGLE);
							String _remark = user.optString(User.remark);
							if (TextUtils.isEmpty(_remark)) {
								_remark = user.optString(User.screen_name);
							}
							CatnutUtils.setText(mRetweetLayout, R.id.retweet_nick, _remark);
							long mills = DateTime.getTimeMills(json.optString(Status.created_at));
							CatnutUtils.setText(mRetweetLayout, R.id.retweet_create_at, DateUtils.getRelativeTimeSpanString(mills));
							TweetTextView retweetText = (TweetTextView) CatnutUtils.setText(mRetweetLayout, R.id.retweet_text, json.optString(Status.text));
							CatnutUtils.vividTweet(retweetText, mImageSpan);
							mRetweetLayout.setVisibility(View.VISIBLE);
						} catch (JSONException e) {
							Log.e(TAG, "convert text to string error!", e);
							mRetweetLayout.setVisibility(View.GONE);
						}
					} else {
						mRetweetLayout.setVisibility(View.GONE);
					}
					// share...
					mShareIntent = new Intent(Intent.ACTION_SEND).setType("text/plain");
					mShareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.tweet_share_subject));
					mShareIntent.putExtra(Intent.EXTRA_TEXT, text);
					if (mShareActionProvider != null) {
						mShareActionProvider.setShareIntent(mShareIntent);
					}
					mFavorited = CatnutUtils.getBoolean(cursor, Status.favorited);
				}
				cursor.close();
			}
		}.startQuery(0, null, CatnutProvider.parse(Status.MULTIPLE), null, query, null, null);
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
		mListView.addFooterView(mLoadMore);
		mListView.setOnScrollListener(this);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		getLoaderManager().initLoader(0, null, this);
	}

	private void loadComments(boolean refresh) {
		if (refresh) {
			// 重置
			mMaxId = 0;
			mTotalSize = 0;
			mCurrentPage = -1;
			mAdapter.swapCursor(null);
			mAdapter = new CommentsAdapter(getActivity());
			mListView.setAdapter(mAdapter);
			getLoaderManager().restartLoader(0, null, this);
		}
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				refresh
						? CommentsAPI.show(mId, 0, 0, BATCH_SIZE, 0, 0)
						: CommentsAPI.show(mId, 0, mMaxId, BATCH_SIZE, 0, 0),
				new StatusProcessor.CommentTweetsProcessor(mId),
				listener,
				errorListener
		)).setTag(TAG);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String limit = String.valueOf(BATCH_SIZE * (mCurrentPage + 1));
		CursorLoader cursorLoader = CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(Status.MULTIPLE),
				PROJECTION,
				Status.TYPE + "=" + Status.COMMENT + " and " + Status.TO_WHICH_TWEET + "=" + mId,
				null,
				Status.TABLE + " as s",
				"inner join " + User.TABLE + " as u on s.uid=u._id",
				"s._id desc",
				limit
		);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		// 标记当前评论尾部的id
		int count = mAdapter.getCount();
		mMaxId = mAdapter.getItemId(count - 1);
		// 移除加载更多
		if (mTotalSize != 0 && count == mTotalSize) {
			mListView.removeFooterView(mLoadMore);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onRefreshStarted(View view) {
		loadComments(true);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// todo: shall we need pref?
		if (mLoadMore.isShown() && !mPullToRefreshLayout.isRefreshing()) {
			loadComments(false);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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
		mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
			@Override
			public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
				startActivity(intent);
				return true;
			}
		});
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
		int tmp;
		String hint = mSendText.getHint().toString();
		if (hint.startsWith(RETWEET_INDICATOR)) {
			tmp = RETWEET;
		} else {
			tmp = hint.contains("@") ? REPLY : COMMENT;
		}
		final int type = tmp; // awful...
		if (type != RETWEET) { // 转发允许啥都不写...
			if (!CatnutUtils.hasLength(mSendText)) {
				Toast.makeText(getActivity(), getString(R.string.require_not_empty), Toast.LENGTH_SHORT).show();
				return; // 提前结束
			}
		}
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
								break;
						}
						Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
						// 删除刚才编辑的内容
						mSendText.setText(null);
						// 更新ui
						TextView which =
								type == RETWEET ? mReteetCount : mReplayCount;
						String before = which.getText().toString();
						if (TextUtils.isEmpty(before)) {
							which.setText("1"); // 小心，这里不要写数字，因为R.string.xx，很容易犯错
						} else {
							which.setText(String.valueOf(Integer.parseInt(before) + 1));
						}
						// list会自动更新，无需手动！
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
}
