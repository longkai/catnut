/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
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
import org.catnut.support.VividSearchView;
import org.json.JSONObject;
import org.catnut.R;
import org.catnut.adapter.UsersAdapter;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.User;
import org.catnut.ui.MainActivity;
import org.catnut.util.CatnutUtils;

/**
 * 用户列表
 *
 * @author longkai
 */
public abstract class UsersFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {

	private static final String TAG = "UsersFragment";

	protected MainActivity mActivity;
	protected UsersAdapter mAdapter;
	protected SharedPreferences mPref;
	protected RequestQueue mRequestQueue;

	/** api loading based cursor! */
	private int mNextCursor;
	/** 简单的加载指示，不至于到底的时候太突兀 */
	protected ProgressBar mLoadMore;
	/** 当前是否正在通过web进行加载 */
	protected boolean isLoadingMore;
	/** 当前的页码，每次加载更多就+1 */
	protected int mCurPage;
	/** 标志位，当前是否是显示搜索列表 */
	protected boolean isSearching;

	/** 搜索视图 */
	protected VividSearchView mSearchView;
	/** 当前搜索关键字 */
	protected String mCurFilter;

	protected Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			isLoadingMore = false;
			mNextCursor = response.optInt(User.next_cursor);
			if (response.optJSONArray(User.MULTIPLE).length() < getDefaultFetchSize()) {
				// no more data
				Toast.makeText(mActivity, mActivity.getString(R.string.no_more), Toast.LENGTH_SHORT).show();
				getListView().removeFooterView(mLoadMore);
			}
		}
	};

	protected Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			isLoadingMore = false;
			Log.e(TAG, "loading users from cloud error!", error);
			Toast.makeText(mActivity, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof MainActivity) {
			mActivity = (MainActivity) activity;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		CatnutApp app = CatnutApp.getTingtingApp();
		this.mPref = app.getPreferences();
		this.mRequestQueue = app.getRequestQueue();
		this.mLoadMore = new ProgressBar(mActivity);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setOnScrollListener(this);
		getListView().addFooterView(mLoadMore);
	}

	@Override
	public void onStart() {
		super.onStart();
		mActivity.getActionBar().setTitle(mActivity.getString(R.string.my_followings_title));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, R.id.refresh, Menu.NONE, R.string.refresh)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		// 搜索相关
		MenuItem search = menu.add(android.R.string.search_go);
		search.setIcon(R.drawable.ic_title_search_default);
		search.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mSearchView = VividSearchView.getSearchView(getActivity());
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setOnCloseListener(this);
		search.setActionView(mSearchView);
	}

	protected abstract void fetchFromCloud(int cursor);

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mLoadMore.isShown() && !isLoadingMore && !isSearching) {
			mCurPage++;
			if (mPref.getBoolean(getString(R.string.pref_keep_latest), true)) {
				Log.d(TAG, "loading more from cloud!");
				// 开启worker线程去web抓取数据
				isLoadingMore = true;
				this.fetchFromCloud(mNextCursor);
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

	/**
	 * 获取抓取微博的条目数
	 */
	protected int getDefaultFetchSize() {
		return CatnutUtils.resolveListPrefInt(
			mPref,
			getString(R.string.pref_default_fetch_size),
			mActivity.getResources().getInteger(R.integer.default_fetch_size)
		);
	}
}
