/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
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
import tingting.chen.tingting.TingtingApp;
import tingting.chen.ui.MainActivity;
import tingting.chen.util.TingtingUtils;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * 时间线
 *
 * @author longkai
 */
public abstract class TimelineFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>, OnRefreshListener,
	AbsListView.OnScrollListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener, SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "TimelineFragment";

	protected MainActivity mActivity;
	protected TweetAdapter mAdapter;
	protected SharedPreferences mPref;
	protected RequestQueue mRequestQueue;

	protected PullToRefreshLayout mPullToRefreshLayout;
	/** footer view for loading more */
	protected ProgressBar mLoadMore;
	/** 当前的页码，每次加载更多就+1 */
	protected int mCurPage;
	/** 当前是否在进行加载更多 */
	protected boolean mLoading;
	/** 标志位，当前是否是显示搜索列表 */
	protected boolean isSearching;

	/** 搜索视图 */
	protected SearchView mSearchView;
	/** 当前搜索关键字 */
	protected String mCurFilter;

	/**
	 * 去web抓取微博
	 *
	 * @param isRefresh 是否刷新微博，否则加载更多
	 * @param offset    从那条开始加载或者刷新？ 不知道就0吧*_*
	 */
	protected abstract void fetchTweetsFromCloud(boolean isRefresh, long offset);

	protected Response.Listener success = new Response.Listener() {
		@Override
		public void onResponse(Object response) {
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	protected Response.ErrorListener error = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			mPullToRefreshLayout.setRefreshComplete();
			Toast.makeText(mActivity, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mActivity = (MainActivity) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		TingtingApp app = TingtingApp.getTingtingApp();
		this.mPref = app.getPreferences();
		this.mRequestQueue = app.getRequestQueue();
		this.mLoadMore = new ProgressBar(mActivity);
		this.mPref.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ViewGroup viewGroup = (ViewGroup) view;
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
		ActionBarPullToRefresh.from(getActivity())
			.insertLayoutInto(viewGroup)
			.theseChildrenArePullable(android.R.id.list, android.R.id.empty)
			.listener(this)
			.setup(mPullToRefreshLayout);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setOnScrollListener(this);
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

	/**
	 * 获取抓取微博的条目数
	 */
	protected int getDefaultFetchSize() {
		int count = TingtingUtils.resolveListPrefInt(mPref,
			PrefFragment.DEFAULT_FETCH_SIZE,
			mActivity.getResources().getInteger(R.integer.default_fetch_size));
		return count;
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
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mLoadMore.isShown() && !mLoading && !isSearching) {
			mLoading = true;
			mCurPage++;
			if (mPref.getBoolean(PrefFragment.AUTO_LOAD_MORE_FROM_CLOUD, true)) {
				Log.d(TAG, "loading more from cloud!");
				// 开启worker线程去web抓取数据，
				this.fetchTweetsFromCloud(false, mAdapter.getItemId(mAdapter.getCount() - 2));
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
	public void onRefreshStarted(View view) {
		fetchTweetsFromCloud(true, 0);
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
