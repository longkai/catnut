/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;
import org.catnut.R;
import org.catnut.adapter.DraftsAdapter;
import org.catnut.core.CatnutProvider;
import org.catnut.metadata.Draft;
import org.catnut.service.ComposeTweetService;

/**
 * 我的草稿
 *
 * @author longkai
 */
public class DraftFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

	private static final String TAG = "DraftFragment";

	public static DraftFragment getFragment() {
		DraftFragment fragment = new DraftFragment();
		return fragment;
	}

	private DraftsAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new DraftsAdapter(getActivity());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getText(R.string.no_more));
		setListAdapter(mAdapter);
		getListView().setOnScrollListener(this);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setTitle(getString(R.string.my_drafts));
	}

	@Override
	public void onListItemClick(ListView l, View v, final int position, final long id) {
		new AlertDialog.Builder(getActivity())
				.setMessage(getString(R.string.send_draft_confirm))
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Cursor cursor = (Cursor) mAdapter.getItem(position);
						Draft draft = new Draft();
						String pic = cursor.getString(cursor.getColumnIndex(Draft.PIC));
						if (!TextUtils.isEmpty(pic)) {
							draft.pic = Uri.parse(pic);
						}
						draft.id = id;
						draft._long = cursor.getFloat(cursor.getColumnIndex(Draft._LONG));
						draft.lat = cursor.getFloat(cursor.getColumnIndex(Draft.LAT));
						draft.status = cursor.getString(cursor.getColumnIndex(Draft.STATUS));
						sendDraft(draft);
					}
				}).show();
	}

	private void sendDraft(Draft draft) {
		Toast.makeText(getActivity(), R.string.background_sending, Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(getActivity(), ComposeTweetService.class);
		intent.putExtra(Draft.DRAFT, draft);
		getActivity().startService(intent);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), CatnutProvider.parse(Draft.MULTIPLE), null, null, null, "_id desc");
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
	public void onScrollStateChanged(AbsListView view, int scrollState) {

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		Log.d(TAG, "id: " + mAdapter.getItemId(firstVisibleItem));
	}
}
