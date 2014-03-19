/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.zhihu;

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.catnut.R;
import org.catnut.core.CatnutProvider;
import org.catnut.support.HtmlImageGetter;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

/**
 * 知乎条目
 *
 * @author longkai
 */
public class ZhihuItemFragment extends Fragment {
	public static final String TAG = ZhihuItemFragment.class.getSimpleName();

	private static final String[] PROJECTION = new String[]{
			Zhihu.QUESTION_ID,
			Zhihu.ANSWER,
			Zhihu.DESCRIPTION,
			Zhihu.TITLE,
			Zhihu.LAST_ALTER_DATE,
			Zhihu.NICK,
	};

	private Handler mHandler = new Handler();

	private long mId;

	public static ZhihuItemFragment getFragment(long id) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, id);
		ZhihuItemFragment fragment = new ZhihuItemFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mId = getArguments().getLong(Constants.ID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.zhihu_item, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		final TextView title = (TextView) view.findViewById(android.R.id.title);
		final TextView question = (TextView) view.findViewById(R.id.question);
		final TextView author = (TextView) view.findViewById(R.id.author);
		final TextView content = (TextView) view.findViewById(android.R.id.content);
		final TextView lastAlterDate = (TextView) view.findViewById(R.id.last_alter_date);
		(new Thread(new Runnable() {
			@Override
			public void run() {
				Cursor cursor = getActivity().getContentResolver().query(
						CatnutProvider.parse(Zhihu.MULTIPLE),
						PROJECTION,
						Zhihu.ANSWER_ID + "=" + mId,
						null,
						null
				);
				if (cursor.moveToNext()) {
					final String _title = cursor.getString(cursor.getColumnIndex(Zhihu.TITLE));
					final String _question = cursor.getString(cursor.getColumnIndex(Zhihu.DESCRIPTION));
					final String _nick = cursor.getString(cursor.getColumnIndex(Zhihu.NICK));
					final String _content = cursor.getString(cursor.getColumnIndex(Zhihu.ANSWER));
					final long _lastAlterDate = cursor.getLong(cursor.getColumnIndex(Zhihu.LAST_ALTER_DATE));
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							title.setText(_title);
							if (TextUtils.isEmpty(_question)) {
								question.setVisibility(View.GONE);
							} else {
								question.setText(Html.fromHtml(_question));
							}
							CatnutUtils.removeLinkUnderline(question);
							question.setMovementMethod(LinkMovementMethod.getInstance());
							author.setText(_nick);
							content.setText(Html.fromHtml(_content, new HtmlImageGetter(content, null), null));
							content.setMovementMethod(LinkMovementMethod.getInstance());
							CatnutUtils.removeLinkUnderline(content);
							lastAlterDate.setText(DateUtils.getRelativeTimeSpanString(_lastAlterDate));
						}
					});
				}
				cursor.close();
			}
		})).start();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getString(R.string.read_zhihu));
	}
}
