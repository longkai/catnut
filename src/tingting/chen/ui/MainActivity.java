/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Process;
import android.view.Menu;
import android.view.MenuItem;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import tingting.chen.R;
import tingting.chen.fragment.HomeTimelineFragment;
import tingting.chen.metadata.User;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.tingting.TingtingProvider;

/**
 * 应用程序主界面。
 * todo 后面需要好好设计界面
 *
 * @author longkai
 */
public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	private TingtingApp mApp;
	private ActionBar mActionBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			mApp = TingtingApp.getTingtingApp();
			mActionBar = getActionBar();
			setHeader();
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new HomeTimelineFragment())
				.commit();
		}
	}

	/**
	 * 设置顶部，关联用户的头像和昵称
	 */
	private void setHeader() {
		new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor != null && cursor.moveToNext()) {
					mActionBar.setDisplayUseLogoEnabled(true);
					mActionBar.setTitle(cursor.getString(0));
					mApp.getImageLoader()
						.get(cursor.getString(1), new ImageLoader.ImageListener() {
							@Override
							public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
								mActionBar.setIcon(new BitmapDrawable(getResources(), response.getBitmap()));
							}

							@Override
							public void onErrorResponse(VolleyError error) {
							}
						});
					cursor.close();
				}
			}
		}.startQuery(
			0,
			null,
			TingtingProvider.parse(User.MULTIPLE, String.valueOf(mApp.getAccessToken().uid)),
			new String[]{
				User.screen_name,
				User.profile_image_url
			},
			null,
			null,
			null
		);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// 登出，kill掉本app的进程，不同于按下back按钮，这个不保证回到上一个back stack
			case R.id.logout:
				new AlertDialog.Builder(this)
					.setMessage(getString(R.string.logout_confirm))
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Process.killProcess(Process.myPid());
						}
					})
					.setNegativeButton(android.R.string.no, null)
					.show();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}