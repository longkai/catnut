/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.util.Constants;

/**
 * 查看fantasy
 *
 * @author longkai
 */
public class FantasyFragment extends Fragment {


	private static final String TAG = "FantasyFragment";

	private static final String DESC = "desc";

	private TextView mDesc;

	public static FantasyFragment getFragment(String url, String desc) {
		Bundle args = new Bundle();
		args.putString(TAG, url);
		args.putString(DESC, desc);
		FantasyFragment fantasyFragment = new FantasyFragment();
		fantasyFragment.setArguments(args);
		return fantasyFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fantasy, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Bundle args = getArguments();
		Picasso.with(getActivity())
				.load(args.getString(TAG))
				.placeholder(R.drawable.error)
				.error(R.drawable.error)
				.into((ImageView) view.findViewById(R.id.fantasy));
		mDesc= (TextView) view.findViewById(R.id.description);
		String desc = args.getString(DESC);
		mDesc.setText(Constants.NULL.equals(desc) ? null : Html.fromHtml(desc));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, R.id.toggle_desc, Menu.NONE, getString(R.string.toggle_desc))
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.toggle_desc:
				if (mDesc.getVisibility() == View.VISIBLE) {
					mDesc.setVisibility(View.GONE);
				} else {
					mDesc.setVisibility(View.VISIBLE);
				}
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
