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
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
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
public class MainActivity extends Activity implements DrawerLayout.DrawerListener, ListView.OnItemClickListener {

	private static final String TAG = "MainActivity";

	private TingtingApp mApp;
	private ActionBar mActionBar;

	private ListView mDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			setContentView(R.layout.main);

			mApp = TingtingApp.getTingtingApp();
			mActionBar = getActionBar();
			prepareActionBar();

			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
			mDrawerLayout.setDrawerListener(this);

			mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.open_drawer, R.string.close_drawer);

			mDrawer = (ListView) findViewById(R.id.drawer);
			mDrawer.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
				new String[]{"1", "2", "3"}));
			mDrawer.setOnItemClickListener(this);

			getFragmentManager()
				.beginTransaction()
					.replace(R.id.fragment_container, new HomeTimelineFragment())
				.commit();
		}
	}

	/**
	 * 设置顶部，关联用户的头像和昵称
	 */
	private void prepareActionBar() {
		// for drawer
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
		// for user' s profile
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
		// open or close the drawer
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onDrawerOpened(View drawerView) {
		mDrawerToggle.onDrawerOpened(drawerView);
	}

	@Override
	public void onDrawerClosed(View drawerView) {
		mDrawerToggle.onDrawerClosed(drawerView);
	}

	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
	}

	@Override
	public void onDrawerStateChanged(int newState) {
		mDrawerToggle.onDrawerStateChanged(newState);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Toast.makeText(MainActivity.this, position + " click!", Toast.LENGTH_SHORT).show();
		mDrawerLayout.closeDrawer(mDrawer);
	}
}