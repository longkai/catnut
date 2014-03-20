/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.zhihu;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
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
import org.catnut.ui.PluginsActivity;
import org.catnut.util.Constants;
import org.json.JSONArray;
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
			"substr(" + Zhihu.ANSWER + ",0,80) as " + Zhihu.ANSWER, // [0-80)子串
			Zhihu.ANSWER_ID,
			Zhihu.NICK
	};

	private PullToRefreshLayout mPullToRefreshLayout;

	private RequestQueue mRequestQueue;
	private ZhihuItemsAdapter mAdapter;

	// 当前items数目，有可能超过的哦
	private int mCount = PAGE_SIZE;
	// 本地items总数
	private int mTotal;

	private boolean mUsePagerMode = false;

	// 载入本地items总数线程
	private Runnable mLoadTotalCount = new Runnable() {
		@Override
		public void run() {
			Cursor cursor = getActivity().getContentResolver().query(
					CatnutProvider.parse(Zhihu.MULTIPLE),
					Constants.COUNT_PROJECTION,
					null, null, null
			);
			if (cursor.moveToNext()) {
				mTotal = cursor.getInt(0);
			}
			cursor.close();

			if (mTotal == 0) {
				// 如果一条也没有，那么去load一回吧...
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						mPullToRefreshLayout.setRefreshing(true);
						refresh();
					}
				});
			}
		}
	};

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
		setEmptyText(getString(R.string.zhihu_refresh_hint));
		getListView().setOnScrollListener(this);
		getLoaderManager().initLoader(0, null, this);
		new Thread(mLoadTotalCount).start(); // 载入总数
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, R.id.refresh, Menu.NONE, R.string.refresh)
				.setIcon(R.drawable.ic_action_retry)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.add(Menu.NONE, R.id.pager, Menu.NONE, R.string.pager_mode)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
			case R.id.pager:
				mUsePagerMode = !mUsePagerMode;
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.pager);
		item.setTitle(mUsePagerMode ? getString(R.string.simple_mode) : getString(R.string.pager_mode));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor c = (Cursor) mAdapter.getItem(position);
		long answer_id = c.getLong(c.getColumnIndex(Zhihu.ANSWER_ID));
		// 跳转
		PluginsActivity activity = (PluginsActivity) getActivity();
		if (mUsePagerMode) {
			Toast.makeText(activity, "not stability yet:-(", Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(activity, PluginsActivity.class);
			intent.putExtra(PagerItemFragment.ORDER_ID, id);
			intent.putExtra(Constants.ID, answer_id);
			intent.setAction(PluginsActivity.ACTION_ZHIHU_PAGER);
			startActivity(intent);
//			activity.flipCard(PagerItemFragment.getFragment(answer_id, id), null, true);
		} else {
			activity.flipCard(ZhihuItemFragment.getFragment(answer_id), null, true);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE
				&& getListView().getLastVisiblePosition() == mAdapter.getCount() - 1
				&& mAdapter.getCount() < mTotal
				&& !mPullToRefreshLayout.isRefreshing()) {
			mPullToRefreshLayout.setRefreshing(true);
			mCount += PAGE_SIZE;
			getLoaderManager().restartLoader(0, null, this);
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
		if (mPullToRefreshLayout.isRefreshing()) {
			mPullToRefreshLayout.setRefreshComplete();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onRefreshStarted(View view) {
		refresh();
	}

	// 刷新
	private void refresh() {
		mRequestQueue.add(new CatnutArrayRequest(
				getActivity(),
				new CatnutAPI(Request.Method.GET, Zhihu.fetchUrl(1), false, null),
				Zhihu.ZhihuProcessor.getProcessor(),
				new Response.Listener<JSONArray>() {
					@Override
					public void onResponse(JSONArray response) {
						mCount = response.length();
						new Thread(mLoadTotalCount).start();
						mPullToRefreshLayout.setRefreshComplete();
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						mPullToRefreshLayout.setRefreshComplete();
					}
				}
		));
	}
}
