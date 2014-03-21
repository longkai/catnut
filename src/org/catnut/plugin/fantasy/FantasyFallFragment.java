/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.fantasy;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.etsy.android.grid.StaggeredGridView;
import org.catnut.R;
import org.catnut.api._500pxAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONObject;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;

/**
 * 500px 瀑布流
 *
 * @author longkai
 */
public class FantasyFallFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener, OnRefreshListener {

	public static final String TAG = FantasyFallFragment.class.getSimpleName();

	private static final String[] PROJECTION = new String[]{
			BaseColumns._ID,
			Photo.image_url,
			Photo.name,
			Photo.width,
			Photo.height
	};

	protected PullToRefreshLayout mPullToRefreshLayout;

	private StaggeredGridView mFall;
	private FantasyFallAdapter mAdapter;

	public static FantasyFallFragment getFragment() {
		return new FantasyFallFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new FantasyFallAdapter(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fantasy_fall, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mFall = (StaggeredGridView) view.findViewById(R.id.fall);

		mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.pull2refresh);
		ActionBarPullToRefresh.from(getActivity())
				.options(Options.create().build())
				.allChildrenArePullable()
				.listener(this)
				.useViewDelegate(GridView.class, new AbsListViewDelegate())
				.setup(mPullToRefreshLayout);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mFall.setAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(Photo.MULTIPLE),
				PROJECTION,
				null,
				null,
				Photo.TABLE,
				null,
				Constants.RANDOM_ORDER,
				null
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		if (!mPullToRefreshLayout.isRefreshing()) {
			if (mAdapter.getCount() == 0 || Photo.shouldRefresh()) { // 如果木有的话，那么来一发~
				mPullToRefreshLayout.setRefreshing(true);
				onRefreshStarted(null);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE
				&& mFall.getLastVisiblePosition() == mAdapter.getCount()) {
			Log.d(TAG, "load more 500px...");
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

	}

	@Override
	public void onRefreshStarted(final View view) {
		CatnutApp.getTingtingApp().getRequestQueue().add(new CatnutRequest(
				getActivity(),
				_500pxAPI.photos(Photo.FEATURE_POPULAR, 0),
				new Photo._500pxProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						mPullToRefreshLayout.setRefreshComplete();
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						mPullToRefreshLayout.setRefreshComplete();
					}
				}
		));
	}
}
