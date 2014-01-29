/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import tingting.chen.R;
import tingting.chen.TingtingApp;
import tingting.chen.beans.AccessToken;
import tingting.chen.fragment.OAuthFragment;
import tingting.chen.fragment.StatusesFragment;
import tingting.chen.fragment.UsersFragment;
import tingting.chen.metadata.Status;
import tingting.chen.processor.StatusProcessor;
import tingting.chen.processor.UserProcessor;
import tingting.chen.tingting.TingtingRequest;

/**
 * 应用程序主界面。
 * todo 后面需要好好设计界面
 *
 * @author longkai
 * @date 2014-01-18
 */
public class MainActivity extends Activity {

	public static final String TAG = "MainActivity";

	private TingtingApp mApp;

	private static AccessToken sAccessToken;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_container);
		mApp = TingtingApp.getTingtingApp();

		if (savedInstanceState == null) {
			Fragment fragment;
			sAccessToken = mApp.getAccessToken();
			// 判断以下是否已经认证，跳转不同的界面
			if (sAccessToken != null) {
//				fragment = new Fragment() {
//					@Override
//					public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//						return inflater.inflate(R.layout.main, container, false);
//					}
//				};
				fragment = new UsersFragment();
			} else {
				fragment = new OAuthFragment();
			}
			getFragmentManager().beginTransaction()
					.replace(R.id.fragment_container, fragment)
					.commit();
		}
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private String uri() {
		StringBuilder uri = new StringBuilder("https://api.weibo.com/2/friendships/friends.json");
		uri.append("?access_token=").append(sAccessToken.access_token)
				.append("&uid=").append(sAccessToken.uid);
		return uri.toString();
	}

	private static String uri2() {
		StringBuilder uri = new StringBuilder("https://api.weibo.com/2/statuses/user_timeline.json");
		uri.append("?access_token=").append(sAccessToken.access_token)
				.append("&uid=").append(sAccessToken.uid)
				.append("&count=100");
		return uri.toString();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem b1 = menu.add(Menu.NONE, android.R.id.button1, Menu.NONE, "b1");
		b1.setIcon(android.R.drawable.ic_input_add);
		b1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		MenuItem b2 = menu.add(Menu.NONE, android.R.id.button2, Menu.NONE, "b2");
		b2.setIcon(android.R.drawable.ic_media_previous);
		b2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				String uri = uri();
				Log.i(TAG, uri);
				TingtingApp.getRequestQueue().add(
						new TingtingRequest(
								this,
								Request.Method.GET,
								uri,
								null,
								new UserProcessor.MyFriendsProcessor(),
								null,
								new Response.ErrorListener() {
									@Override
									public void onErrorResponse(VolleyError error) {
										Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
									}
								}
						)
				);
				break;
			case android.R.id.button1:
				String url = uri2();
				Log.d(TAG, url);
				TingtingApp.getRequestQueue()
						.add(new TingtingRequest(
								this,
								Request.Method.GET,
								uri2(),
								null,
								new StatusProcessor.MyTweetsProcessor(),
								null,
								new Response.ErrorListener() {
									@Override
									public void onErrorResponse(VolleyError error) {
										Toast.makeText(MainActivity.this, "error", Toast.LENGTH_LONG).show();
									}
								}
						));
				break;
			case android.R.id.button2:
				getFragmentManager().beginTransaction()
						.replace(R.id.fragment_container, new StatusesFragment())
						.addToBackStack(null)
						.commit();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class StatusAdapter extends CursorAdapter {

		public StatusAdapter(Context context) {
			super(context, null, 0);
		}

		private static class ViewHoler {
			TextView userId;
			int userIdIndex;
			TextView text;
			int textIndex;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = LayoutInflater.from(context).inflate(R.layout.main, null);
			ViewHoler holer = new ViewHoler();
			holer.userId = (TextView) v.findViewById(android.R.id.title);
			holer.userIdIndex = cursor.getColumnIndex(Status.uid);
			holer.text = (TextView) v.findViewById(android.R.id.content);
			holer.textIndex = cursor.getColumnIndex(Status.columnText);
			v.setTag(holer);
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHoler holer = (ViewHoler) view.getTag();
			holer.userId.setText(cursor.getString(holer.userIdIndex));
			holer.text.setText(cursor.getString(holer.textIndex));
		}
	}
}