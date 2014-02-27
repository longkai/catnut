/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import org.catnut.R;
import org.catnut.metadata.User;

/**
 * 艾特查询，备注和id
 *
 * @author longkai
 */
public class MentionSearchAdapter extends CursorAdapter {

	public MentionSearchAdapter(Context context) {
		super(context, null, 0);
	}

	private static class ViewHolder {
		TextView item;
		int screenNameIndex;
		int remarkIndex;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null);
		ViewHolder holder = new ViewHolder();
		holder.item = (TextView) view.findViewById(android.R.id.text1);
		holder.screenNameIndex = cursor.getColumnIndex(User.screen_name);
		holder.remarkIndex = cursor.getColumnIndex(User.remark);
		view.setTag(holder);
		return view;
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(User.screen_name));
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		String screenName = cursor.getString(holder.screenNameIndex);
		String remark = cursor.getString(holder.remarkIndex);
		String text = TextUtils.isEmpty(remark)
				? screenName
				: context.getString(R.string.mention_schema, screenName, remark);
		holder.item.setText(text);
	}
}
