/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.zhihu;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.catnut.R;
import org.catnut.util.CatnutUtils;

/**
 * 知乎列表适配
 *
 * @author longkai
 */
public class ZhihuItemsAdapter extends CursorAdapter {

	public ZhihuItemsAdapter(Context context) {
		super(context, null, 0);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = LayoutInflater.from(context).inflate(R.layout.zhihu_item_row, null);
		ViewHolder holder = new ViewHolder();

		holder.hasRead = (ImageView) view.findViewById(R.id.has_read);
		holder.hasReadIndex = cursor.getColumnIndex(Zhihu.HAS_READ);

		holder.title = (TextView) view.findViewById(android.R.id.title);
		holder.titleIndex = cursor.getColumnIndex(Zhihu.TITLE);

		holder.digest = (TextView) view.findViewById(android.R.id.text1);
		holder.digestIndex = cursor.getColumnIndex(Zhihu.ANSWER);

		holder.lastAlterDate = (TextView) view.findViewById(R.id.last_alter_date);
		holder.lastAlterDateIndex = cursor.getColumnIndex(Zhihu.LAST_ALTER_DATE);

		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (CatnutUtils.getBoolean(cursor, Zhihu.HAS_READ)) {
			holder.hasRead.setVisibility(View.VISIBLE);
		} else {
			holder.hasRead.setVisibility(View.INVISIBLE);
		}
		holder.title.setText(cursor.getString(holder.titleIndex));
		holder.digest.setText(Html.fromHtml(cursor.getString(holder.digestIndex)));
		holder.lastAlterDate.setText(DateUtils.getRelativeTimeSpanString(cursor.getLong(holder.lastAlterDateIndex)));
	}

	private static class ViewHolder {
		ImageView hasRead;
		int hasReadIndex;
		TextView title;
		int titleIndex;
		TextView digest;
		int digestIndex;
		TextView lastAlterDate;
		int lastAlterDateIndex;
	}
}
