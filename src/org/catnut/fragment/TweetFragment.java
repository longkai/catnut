/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import org.catnut.R;
import org.catnut.adapter.CommentsAdapter;
import org.catnut.api.CommentsAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.StatusProcessor;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.ui.ProfileActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONObject;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * 微博界面
 *
 * @author longkai
 */
public class TweetFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,OnRefreshListener, AbsListView.OnScrollListener {

	private static final String TAG = "TweetFragment";

	private static final int BATCH_SIZE = 50;

	/** 待检索的列 */
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

	private CommentsAdapter mAdapter;

	// tweet id
	private long mId;

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

	// others
	private ShareActionProvider mShareActionProvider;
	private Intent mShareIntent;

	private Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			mTotalSize = response.optInt(Status.total_number);
			if (mTotalSize == 0) {
				getListView().removeFooterView(mLoadMore);
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
			Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
		mTweetLayout = inflater.inflate(R.layout.tweet, null, false);
		mAvatar = (ImageView) mTweetLayout.findViewById(R.id.avatar);
		mRemark = (TextView) mTweetLayout.findViewById(R.id.remark);
		mScreenName = (TextView) mTweetLayout.findViewById(R.id.screen_name);
		mText = (TweetTextView) mTweetLayout.findViewById(R.id.text);
		mReplayCount = (TextView) mTweetLayout.findViewById(R.id.reply_count);
		mReteetCount = (TextView) mTweetLayout.findViewById(R.id.reply_count);
		mFavoriteCount = (TextView) mTweetLayout.findViewById(R.id.favorite_count);
		mSource = (TextView) mTweetLayout.findViewById(R.id.source);
		mCreateAt = (TextView) mTweetLayout.findViewById(R.id.create_at);
		// just return the list
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
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
						Status.thumbnail_pic,
						Status.comments_count,
						Status.reposts_count,
						Status.attitudes_count,
						Status.source,
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
					String remark = cursor.getString(cursor.getColumnIndex(User.remark));
					String screenName = cursor.getString(cursor.getColumnIndex(User.screen_name));
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
						view.findViewById(R.id.verified).setVisibility(View.VISIBLE);
					}

					// share...
					mShareIntent = new Intent(Intent.ACTION_SEND).setType("text/plain");
					mShareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.tweet_share_subject));
					mShareIntent.putExtra(Intent.EXTRA_TEXT, text);
					if (mShareActionProvider != null) {
						mShareActionProvider.setShareIntent(mShareIntent);
					}
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().addHeaderView(mTweetLayout);
		getListView().addFooterView(mLoadMore);
		getListView().setOnScrollListener(this);
		setListAdapter(mAdapter);
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
			setListAdapter(mAdapter);
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
		));
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
			getListView().removeFooterView(mLoadMore);
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
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), ProfileActivity.class);
		Cursor cursor = (Cursor) mAdapter.getItem(position - 1); // a header view in top...
		intent.putExtra(Constants.ID, id);
		intent.putExtra(User.screen_name, cursor.getString(cursor.getColumnIndex(User.screen_name)));
		startActivity(intent);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		}
		return super.onOptionsItemSelected(item);
	}
}
