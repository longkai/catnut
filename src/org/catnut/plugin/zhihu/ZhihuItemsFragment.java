/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.zhihu;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.catnut.R;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutArrayRequest;
import org.catnut.core.CatnutProvider;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * 知乎列表界面
 *
 * @author longkai
 */
public class ZhihuItemsFragment extends ListFragment implements
		AbsListView.OnScrollListener, LoaderManager.LoaderCallbacks<Cursor>,OnRefreshListener {

	public static final String TAG = ZhihuItemsFragment.class.getSimpleName();
	private static final int PAGE_SIZE = 10; // 每次加载10条吧

	private static final String[] PROJECTION = new String[]{
			BaseColumns._ID,
			Zhihu.HAS_READ,
			Zhihu.TITLE,
			"substr(" + Zhihu.ANSWER + ",0,80) as " + Zhihu.ANSWER, // 0-50子串
			Zhihu.ANSWER_ID,
			Zhihu.LAST_ALTER_DATE
	};

	private PullToRefreshLayout mPullToRefreshLayout;

	private RequestQueue mRequestQueue;
	private ZhihuItemsAdapter mAdapter;

	// 当前列表长度
	private int mCount = PAGE_SIZE;
	// 本地items总数
	private int mTotal;

	public static ZhihuItemsFragment getFragment() {
		return new ZhihuItemsFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new ZhihuItemsAdapter(getActivity());
		mRequestQueue = CatnutApp.getTingtingApp().getRequestQueue();
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
		setListAdapter(mAdapter);
		getListView().setOnScrollListener(this);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, R.id.refresh, Menu.NONE, R.string.refresh)
				.setIcon(R.drawable.ic_action_retry)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				mRequestQueue.add(new CatnutArrayRequest(
						getActivity(),
						new CatnutAPI(Request.Method.GET, Zhihu.fetchUrl(1), false, null),
						Zhihu.ZhihuProcessor.getProcessor(),
						null,
						new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
							}
						}
				));
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE
				&& getListView().getLastVisiblePosition() != mTotal) {
			Log.d(TAG, "load more...");
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// no-op
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(
				getActivity(),
				CatnutProvider.parse(Zhihu.MULTIPLE),
				PROJECTION,
				null,
				null,
				BaseColumns._ID + " desc limit " + mCount
		);
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

	}
}
