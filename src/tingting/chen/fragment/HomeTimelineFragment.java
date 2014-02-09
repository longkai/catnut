/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.*;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import tingting.chen.R;
import tingting.chen.adapter.TweetAdapter;
import tingting.chen.api.TweetAPI;
import tingting.chen.metadata.Status;
import tingting.chen.metadata.User;
import tingting.chen.processor.StatusProcessor;
import tingting.chen.tingting.TingtingAPI;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.tingting.TingtingProvider;
import tingting.chen.tingting.TingtingRequest;
import tingting.chen.util.TingtingUtils;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * 当前登录用户及其所关注用户的最新微博时间线
 *
 * @author longkai
 */
public class HomeTimelineFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>, OnRefreshListener, AbsListView.OnScrollListener {

	private static final String TAG = "HomeTimelineFragment";

	/** 默认每页微博数量 */
	private static final int TWEETS_SIZE = 20;

	private RequestQueue mRequestQueue;
	private Activity mActivity;
	private TweetAdapter mAdapter;

	private PullToRefreshLayout mPullToRefreshLayout;
	/** footer view for loading more */
	private ProgressBar mLoadMore;
	/** 当前的页码，每次加载更多就+1 */
	private int mCurPage;
	/** 当前是否在进行加载更多 */
	private boolean mLoading;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
		mRequestQueue = TingtingApp.getTingtingApp().getRequestQueue();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new TweetAdapter(mActivity);
		mLoadMore = new ProgressBar(mActivity);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ViewGroup viewGroup = (ViewGroup) view;
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
		ActionBarPullToRefresh.from(mActivity)
			.insertLayoutInto(viewGroup)
			.theseChildrenArePullable(android.R.id.list, android.R.id.empty)
			.listener(this)
			.setup(mPullToRefreshLayout);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, R.id.refresh, Menu.NONE, R.string.refresh)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				mPullToRefreshLayout.setRefreshing(true);
				onRefreshStarted(null);
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setOnScrollListener(this);
		getListView().addFooterView(mLoadMore);
		setEmptyText(mActivity.getString(R.string.no_tweets));
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onStop() {
		mRequestQueue.cancelAll(TAG);
		super.onStop();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader cursorLoader = TingtingUtils.getCursorLoader(
			mActivity,
			TingtingProvider.parse(Status.MULTIPLE),
			new String[]{
				"s._id",
				Status.columnText,
				Status.thumbnail_pic,
				Status.comments_count,
				Status.reposts_count,
				"s." + Status.created_at,
				User.screen_name,
				User.profile_image_url
			},
			null,
			null,
			Status.TABLE + " as s",
			"inner join " + User.TABLE + " as u on s.uid=u._id",
			"s._id desc",
			String.valueOf(TWEETS_SIZE * (mCurPage + 1))
		);
		// 清除加载更多标志位
		if (mCurPage != 0) {
			mLoading = false;
		}
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onRefreshStarted(View view) {
		fetchTweets(true, 0);
	}

	/**
	 * 去web抓取微博
	 *
	 * @param isRefresh 是否刷新微博，否则加载更多
	 * @param offset    从那条开始加载或者刷新？ 不知道就0吧*_*
	 */
	private void fetchTweets(boolean isRefresh, long offset) {
		TingtingAPI api = isRefresh ? TweetAPI.homeTimeline(offset, 0, 0, 0, 0, 0, 0)
			: TweetAPI.homeTimeline(0, offset, 0, 0, 0, 0, 0);
		mRequestQueue.add(new TingtingRequest(
			mActivity,
			api,
			new StatusProcessor.MyTweetsProcessor(),
			success,
			error
		)).setTag(TAG);
	}

	private Response.Listener success = new Response.Listener() {
		@Override
		public void onResponse(Object response) {
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	private Response.ErrorListener error = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			mPullToRefreshLayout.setRefreshComplete();
			Toast.makeText(mActivity, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mLoadMore.isShown() && !mLoading) {
			mLoading = true;
			mCurPage++;
			// 开启worker线程去web抓取数据，
			// todo: 这里可以设置偏好，此时网络是否开启（wifi or data）去更新数据还是只看本地缓存的，假如是本地的，没有更多了要告知用户
			fetchTweets(false, mAdapter.getItemId(mAdapter.getCount() - 2)); // -2 因为从0开始并且有一个footer view
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}
}
