/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import tingting.chen.metadata.User;
import tingting.chen.tingting.TingtingProvider;

/**
 * Created by longkai on 14-1-20.
 */
public class UsersFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private UsersAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new UsersAdapter(getActivity());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText("no data!");
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), TingtingProvider.parse(User.MULTIPLE), null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	private static class UsersAdapter extends CursorAdapter {

		public UsersAdapter(Context context) {
			super(context, null, 0);
		}

		private static class ViewHolder {
			TextView text;
			int textIndex;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null);
			ViewHolder holder = new ViewHolder();
			holder.text = (TextView) view.findViewById(android.R.id.text1);
			holder.textIndex = cursor.getColumnIndex(User.name);
			view.setTag(holder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.text.setText(cursor.getString(holder.textIndex));
		}
	}
}
