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
import android.provider.BaseColumns;
import tingting.chen.R;
import tingting.chen.adapter.UsersAdapter;
import tingting.chen.metadata.User;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.tingting.TingtingProvider;
import tingting.chen.ui.MainActivity;
import tingting.chen.util.TingtingUtils;

/**
 * Created by longkai on 14-2-14.
 */
public class UsersFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "UsersFragment";

	private TingtingApp mApp;
	private MainActivity mActivity;
	private UsersAdapter mAdapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof MainActivity) {
			mActivity = (MainActivity) activity;
		}
		mApp = TingtingApp.getTingtingApp();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new UsersAdapter(getActivity());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText("no users!");
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}


	@Override
	public void onStart() {
		super.onStart();
		mActivity.getActionBar().setTitle(mActivity.getString(R.string.my_followings_title));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader cursorLoader = TingtingUtils.getCursorLoader(
			getActivity(),
			TingtingProvider.parse(User.MULTIPLE),
			new String[]{
				BaseColumns._ID,
				User.screen_name,
				User.profile_image_url,
				User.verified,
				User.location,
				User.description
			},
			User.following + "=1",
			null,
			User.TABLE,
			null,
			null,
			null
		);
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
}
