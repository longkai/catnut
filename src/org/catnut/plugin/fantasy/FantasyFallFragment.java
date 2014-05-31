/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.fantasy;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Spinner;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.etsy.android.grid.StaggeredGridView;
import org.catnut.R;
import org.catnut.api._500pxAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.ui.HelloActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.ColorSwicher;
import org.catnut.util.Constants;
import org.json.JSONObject;

/**
 * 500px 瀑布流
 *
 * @author longkai
 */
public class FantasyFallFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener, AdapterView.OnItemSelectedListener, SwipeRefreshLayout.OnRefreshListener {

	public static final String TAG = FantasyFallFragment.class.getSimpleName();

	private static final String[] PROJECTION = new String[]{
			BaseColumns._ID,
			Photo.image_url,
			Photo.name,
			Photo.width,
			Photo.height
	};

	protected SwipeRefreshLayout mSwipeRefreshLayout;
	private Spinner mSwitcher;

	private StaggeredGridView mFall;
	private FantasyFallAdapter mAdapter;

	private String mChoice = Photo.FEATURE_POPULAR;

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
		mSwitcher = (Spinner) inflater.inflate(R.layout.fantasy_fall_spinner, null);
		return inflater.inflate(R.layout.fantasy_fall, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mSwitcher.setOnItemSelectedListener(this);
		mFall = (StaggeredGridView) view.findViewById(R.id.fall);
		mFall.addHeaderView(mSwitcher);
		mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
		int[] colors = ColorSwicher.ramdomColors(4);
		mSwipeRefreshLayout.setColorScheme(colors[0], colors[1], colors[2], colors[3]);
		mSwipeRefreshLayout.setOnRefreshListener(this);
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
				Photo.feature + "=" + CatnutUtils.quote(mChoice),
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
		if (!mSwipeRefreshLayout.isRefreshing()) {
			if (mAdapter.getCount() == 0 || Photo.shouldRefresh()) { // 如果木有的话，那么来一发~
				mSwipeRefreshLayout.setRefreshing(true);
				onRefresh();
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
	public void onRefresh() {
		CatnutApp.getTingtingApp().getRequestQueue().add(new CatnutRequest(
				getActivity(),
				_500pxAPI.photos(mChoice, (int) (Math.random() * 400)),
				new Photo._500pxProcessor(mChoice),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						mSwipeRefreshLayout.setRefreshing(false);
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						mSwipeRefreshLayout.setRefreshing(false);
					}
				}
		));
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		String last = mChoice;
		switch (position) {
			default:
			case 0:
				mChoice = Photo.FEATURE_POPULAR;
				break;
			case 1:
				mChoice = Photo.FEATURE_UPCOMING;
				break;
			case 2:
				mChoice = Photo.FEATURE_FRESH_TODAY;
				break;
			case 3:
				mChoice = Photo.FEATURE_EDITORS;
				break;
			case 4:
				mChoice = Photo.FEATURE_FRESH_YESTERDAY;
				break;
			case 5:
				mChoice = Photo.FEATURE_FRESH_WEEK;
				break;
			case 6: // switch pager mode
				startActivity(new Intent(getActivity(), HelloActivity.class)
						.putExtra(HelloActivity.TAG, HelloActivity.TAG));
				mSwitcher.setSelection(0);
				return;
		}
		if (!mChoice.equals(last)) {
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// no-op
	}
}
