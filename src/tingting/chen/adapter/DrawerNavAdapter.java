/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import tingting.chen.R;
import tingting.chen.util.TingtingUtils;

/**
 * drawer list view with headers
 *
 * @author longkai
 */
public class DrawerNavAdapter extends BaseAdapter {

	private static final Boolean HEADER = Boolean.TRUE;

	private Context mContext;
	private String[] mStrings;
	private int[] mHeaderIndexes;

	public DrawerNavAdapter(Context context, int stringArrayResId, int headerIndexesResId) {
		this.mContext = context;
		this.mStrings = context.getResources().getStringArray(stringArrayResId);
		this.mHeaderIndexes = context.getResources().getIntArray(headerIndexesResId);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		for (int i : mHeaderIndexes) {
			if (position == i) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int getCount() {
		return mStrings.length;
	}

	@Override
	public Object getItem(int position) {
		return mStrings[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// header goes first!
		if (!isEnabled(position)) {
			View header = convertView;
			if (header == null || header.getTag() != HEADER) {
				header = LayoutInflater.from(mContext).inflate(R.layout.list_view_header, parent, false);
				header.setTag(HEADER);
			}
			TingtingUtils.setText(header, R.id.list_header, mStrings[position]);
			return header;
		}
		// for list items
		View item = convertView;
		if (item == null || item.getTag() == HEADER) {
			item = LayoutInflater.from(mContext).inflate(R.layout.nav_text_row, parent, false);
			item.setTag(!HEADER);
		}
		TingtingUtils.setText(item, R.id.nav_text, mStrings[position]);
		return item;
	}
}
