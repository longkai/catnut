/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.util.Constants;
import org.json.JSONObject;

/**
 * 转发界面
 *
 * @author longkai
 */
public class RetweetBoxFragment extends DialogFragment implements DialogInterface.OnClickListener {

	private static final String TAG = "RetweetBoxFragment";

	// app stuff
	private CatnutApp mApp;
	// 被转发的微博id
	private long mId;
	private int mRetweetOption;
	private String mScreenNameText;
	private String mTweetText;

	// widgets
	private TextView mScreenName;
	private TextView mText;
	private TextView mTextCounter;
	private EditText mRetweetText;
	private Spinner mRetweetOpions;

	private Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			Toast.makeText(getActivity(), getString(R.string.retweet_success), Toast.LENGTH_SHORT).show();
		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Log.e(TAG, "retweet error!", error);
			WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
			Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_SHORT).show();
		}
	};

	public static RetweetBoxFragment getFragment(long id, String text, String screenName) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, id);
		args.putString(Status.text, text);
		args.putString(User.screen_name, screenName);
		RetweetBoxFragment fragment = new RetweetBoxFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mApp = CatnutApp.getTingtingApp();
		Bundle args = getArguments();
		mId = args.getLong(Constants.ID);
		mScreenNameText = args.getString(User.screen_name);
		mTweetText = args.getString(Status.text);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.retweet, null);
		mScreenName = (TextView) view.findViewById(R.id.screen_name);
		mText = (TextView) view.findViewById(R.id.text);
		mText.setText(mTweetText);
		mScreenName.setText(mScreenNameText);
		mTextCounter = (TextView) view.findViewById(R.id.text_counter);
		mRetweetText = (EditText) view.findViewById(R.id.retweet_text);
		mRetweetOpions = (Spinner) view.findViewById(R.id.retweet_options);
		mRetweetOpions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Log.d(TAG, "pos: " + position);
				mRetweetOption = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// no-op
			}
		});
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
				.setView(view)
				.setTitle(R.string.retweet)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.ok, this)
				.create();
		alertDialog.getWindow().setSoftInputMode(
						WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		return alertDialog;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// todo
	}
}
