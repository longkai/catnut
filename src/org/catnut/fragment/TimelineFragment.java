/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.support.ConfirmBarController;
import org.catnut.support.SwipeDismissListViewTouchListener;
import org.catnut.support.VividSearchView;
import org.catnut.util.CatnutUtils;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * 时间线
 *
 * @author longkai
 */
public abstract class TimelineFragment extends Fragment implements
		ConfirmBarController.ConfirmListener, OnRefreshListener, AbsListView.OnScrollListener,
		LoaderManager.LoaderCallbacks<Cursor>, SwipeDismissListViewTouchListener.DismissCallbacks,
		AdapterView.OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {

	private static final String TAG = "TimelineFragment";

	public static final String SEARCH_TWEET = "search_tweet";
	public static final String TOTAL_NUMBER = "total_number";

	protected CatnutApp mApp;
	protected SharedPreferences mPreferences;
	protected PullToRefreshLayout mPullToRefreshLayout;

	protected ListView mListView;
	protected ConfirmBarController mConfirmBarController;
	protected SwipeDismissListViewTouchListener mSwipeDismissListViewTouchListener;

	// Only one error listener!
	protected Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Log.d(TAG, "error loading data from cloud!", error);
			WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
			Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_LONG).show();
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	// 搜索视图
	protected VividSearchView mSearchView;
	// 当前搜索关键字
	protected String mCurFilter;

	protected CursorAdapter mAdapter;

	// empty text view if the adapter is empty
	private TextView mEmptyText;
	private ConnectivityManager mConnectivityManager;

	/** 刷新 */
	protected abstract void refresh();

	/**
	 * 加载更多
	 *
	 * @param max_id 比max_id更小的条目
	 */
	protected abstract void loadMore(long max_id);

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mApp = CatnutApp.getTingtingApp();
		mPreferences = mApp.getPreferences();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.confirm_bar, container, false);
		mListView = (ListView) view.findViewById(android.R.id.list);
		mConfirmBarController = new ConfirmBarController(view.findViewById(R.id.confirmbar), this);
		mEmptyText = (TextView) inflater.inflate(R.layout.empty_list, null);
		mListView.setEmptyView(mEmptyText); // empty text view
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		ViewGroup viewGroup = (ViewGroup) view;
		viewGroup.addView(mEmptyText);
		mSwipeDismissListViewTouchListener = new SwipeDismissListViewTouchListener(mListView, this);
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
		mListView.setOnTouchListener(mSwipeDismissListViewTouchListener);
		mListView.setOnScrollListener(this);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();
//		mPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, R.id.refresh, Menu.NONE, R.string.refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); // prefer actionbar refresh
		// 本地微博搜索
		MenuItem search = menu.add(android.R.string.search_go);
		search.setIcon(R.drawable.ic_title_search_default);
		search.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mSearchView = VividSearchView.getSearchView(getActivity());
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setOnCloseListener(this);
		search.setActionView(mSearchView);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				if (!mPullToRefreshLayout.isRefreshing()) {
					mPullToRefreshLayout.setRefreshing(true);
					refresh();
				}
				break;
			default:
				break;
		}
		return true;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		mSwipeDismissListViewTouchListener.setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// no-op
	}

	@Override
	public void onRefreshStarted(View view) {
		refresh();
	}

	@Override
	public void onConfirm(Bundle args) {
		// no-op
	}

	@Override
	public boolean canDismiss(int position) {
		return false;
	}

	@Override
	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
		// no-op
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// sub-class impl it...
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// sub-class impl it...
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return true;
	}

	@Override
	public boolean onClose() {
		if (!TextUtils.isEmpty(mSearchView.getQuery())) {
			mSearchView.setQuery(null, true);
		}
		return true;
	}

	public void setEmptyText(CharSequence text) {
		mEmptyText.setText(text);
	}

	public int getFetchSize() {
		return CatnutUtils.resolveListPrefInt(
				mPreferences,
				getString(R.string.pref_default_fetch_size),
				getResources().getInteger(R.integer.default_fetch_size)
		);
	}

	public boolean isNetworkAvailable() {
		NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		return activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();
	}

	// 显示没有更多了的次数，避免一直toast...
	private int mShowToastTimes = 0;
	private static final int MAX_SHOW_TOAST_TIMES = 2;

	public void loadAllDone() {
		if (mShowToastTimes < MAX_SHOW_TOAST_TIMES) {
			Toast.makeText(getActivity(), R.string.no_more, Toast.LENGTH_SHORT).show();
			mShowToastTimes++;
		}
	}
}
