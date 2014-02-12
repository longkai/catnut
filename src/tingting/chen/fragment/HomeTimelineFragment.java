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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
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
	implements LoaderManager.LoaderCallbacks<Cursor>, OnRefreshListener, AbsListView.OnScrollListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {

	private static final String TAG = "HomeTimelineFragment";

	/** 待检索的列 */
	private static final String[] COLUMNS = new String[]{
		"s._id",
		Status.columnText,
		Status.thumbnail_pic,
		Status.comments_count,
		Status.reposts_count,
		Status.attitudes_count,
		Status.source,
		"s." + Status.created_at,
		User.screen_name,
		User.profile_image_url
	};

	private SharedPreferences mPref;
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
	/** 标志位，当前是否是显示搜索列表 */
	private boolean isSearching;

	/** 搜索视图 */
	private SearchView mSearchView;
	/** 当前搜索关键字 */
	private String mCurFilter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
		mPref = TingtingApp.getTingtingApp().getPreferences();
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
		// 搜索相关
		MenuItem search = menu.add(android.R.string.search_go);
		search.setIcon(R.drawable.ic_title_search_default);
		search.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
			| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mSearchView = new SearchView(mActivity);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setOnCloseListener(this);
		mSearchView.setIconifiedByDefault(false);
		mSearchView.setQueryHint(getString(R.string.search_hint));
		mSearchView.setIconified(true);
		int searchPlateId = mSearchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
		View searchPlate = mSearchView.findViewById(searchPlateId);
		if (searchPlate != null) {
			// 修改搜索文字的颜色
			int searchTextId = searchPlate.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
			TextView searchText = (TextView) searchPlate.findViewById(searchTextId);
			if (searchText != null) {
				searchText.setTextColor(Color.WHITE);
				searchText.setHintTextColor(Color.WHITE);
			}
		}
		// 修改搜索hint图标，这里有bug，所以不得已搜索的空间又小了
		int searchButtonId = mSearchView.getContext().getResources().getIdentifier("android:id/search_mag_icon", null, null);
		ImageView searchButton = (ImageView) mSearchView.findViewById(searchButtonId);
		if (searchButton != null) {
			searchButton.setImageResource(R.drawable.ic_search_hint);
		}
		// 修改清除图标
		int clearId = mSearchView.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null);
		ImageView closeButton = (ImageView) mSearchView.findViewById(clearId);
		if (closeButton != null) {
			closeButton.setImageResource(R.drawable.ic_clear);
		}
		search.setActionView(mSearchView);
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
		// 先尝试去新浪抓一把，避免那啥，empty*_*
		boolean autoFetch = mPref.getBoolean(PrefFragment.AUTO_FETCH_ON_START, true);
		boolean firstRun = mPref.getBoolean(PrefFragment.IS_FIRST_RUN, true);
		if (autoFetch || firstRun) {
			mPullToRefreshLayout.setRefreshing(true);
			fetchTweets(true, 0);
			if (firstRun) {
				mPref.edit().putBoolean(PrefFragment.IS_FIRST_RUN, false).commit();
			}
		}
		// 接下来办正事
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
		String where = null;
		int size = TingtingUtils.resolveListPrefInt(mPref,
			PrefFragment.DEFAULT_FETCH_SIZE, mActivity.getResources().getInteger(R.integer.default_fetch_size));
		String limit = String.valueOf(size * (mCurPage + 1));
		// 搜索只能是本地搜索
		if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.trim().length() != 0) {
			where = Status.columnText + " like " + TingtingUtils.like(mCurFilter) +
				" or " +
				User.screen_name + " like " + TingtingUtils.like(mCurFilter);
			limit = null;
			if (!isSearching) {
				getListView().removeFooterView(mLoadMore);
			}
			isSearching = true;
		} else if (isSearching) {
			isSearching = false;
			getListView().addFooterView(mLoadMore);
		}

		CursorLoader cursorLoader = TingtingUtils.getCursorLoader(
			mActivity,
			TingtingProvider.parse(Status.MULTIPLE),
			COLUMNS,
			where,
			null,
			Status.TABLE + " as s",
			"inner join " + User.TABLE + " as u on s.uid=u._id",
			"s._id desc",
			limit
		);
		// 清除加载更多标志位
		if (mCurPage != 0) {
			mLoading = false;
		}
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// 为何微博如此之少Orz
		if (data.getCount() < 20) {
			getListView().removeFooterView(mLoadMore);
		}
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
		int count = TingtingUtils.resolveListPrefInt(mPref,
			PrefFragment.DEFAULT_FETCH_SIZE,
			mActivity.getResources().getInteger(R.integer.default_fetch_size));
		TingtingAPI api = isRefresh ? TweetAPI.homeTimeline(offset, 0, count, 0, 0, 0, 0)
			: TweetAPI.homeTimeline(0, offset, count, 0, 0, 0, 0);
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
		if (mLoadMore.isShown() && !mLoading && !isSearching) {
			mLoading = true;
			mCurPage++;
			if (mPref.getBoolean(PrefFragment.AUTO_LOAD_MORE_FROM_CLOUD, true)) {
				Log.d(TAG, "loading more from cloud!");
				// 开启worker线程去web抓取数据，
				fetchTweets(false, mAdapter.getItemId(mAdapter.getCount() - 2)); // -2 因为从0开始并且有一个footer view
			} else {
				Log.d(TAG, "loading more from local!");
			}
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		// the search filter, and restart the loader to do a new query
		// with this filter.
		String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
		// Don't do anything if the filter hasn't actually changed.
		// Prevents restarting the loader when restoring state.
		if (TextUtils.isEmpty(mCurFilter) && TextUtils.isEmpty(newFilter)) {
			return true;
		}
		if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.equals(newFilter)) {
			return true;
		}
		mCurFilter = newFilter;
		getLoaderManager().restartLoader(0, null, this);
		return true;
	}

	@Override
	public boolean onClose() {
		if (!TextUtils.isEmpty(mSearchView.getQuery())) {
			mSearchView.setQuery(null, true);
		}
		return true;
	}
}
