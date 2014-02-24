/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.toolbox.ImageLoader;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.metadata.User;

/**
 * 发微博
 *
 * @author longkai
 */
public class ComposeTweetActivity extends Activity {

	public static final String TAG = "ComposeTweetActivity";

	private CatnutApp mApp;

	// widgets
	private ImageView mAvatar;
	private TextView mScreenName;
	private TextView mTextCounter;
	private EditText mText;
	private ImageView mGallery;
	private ImageView mGeo;
	private ImageView mCamera;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = CatnutApp.getTingtingApp();
		injectLayout();

		ActionBar bar = getActionBar();
		bar.setIcon(R.drawable.ic_title_compose);
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.compose);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.compose, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateUpTo(getIntent());
				break;
			case R.id.action_send:
				Log.d(TAG, "TODO");
				break;
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PREF));
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void injectLayout() {
		setContentView(R.layout.compose);
		mAvatar = (ImageView) findViewById(R.id.avatar);
		mScreenName = (TextView) findViewById(R.id.screen_name);
		mTextCounter = (TextView) findViewById(R.id.text_counter);
		mText = (EditText) findViewById(R.id.text);
		mGallery = (ImageView) findViewById(R.id.action_gallery);
		mCamera = (ImageView) findViewById(R.id.action_camera);
		mGeo = (ImageView) findViewById(R.id.action_geo);
		// set data to layout...
		new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					mApp.getImageLoader().get(cursor.getString(cursor.getColumnIndex(User.avatar_large)),
							ImageLoader.getImageListener(mAvatar, R.drawable.error, R.drawable.error));
					mScreenName.setText("@" + cursor.getString(cursor.getColumnIndex(User.screen_name)));
				}
				cursor.close();
			}
		}.startQuery(0, null,
				CatnutProvider.parse(User.MULTIPLE, mApp.getAccessToken().uid),
				new String[]{User.avatar_large, User.screen_name}, null, null, null);
	}
}