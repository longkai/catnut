/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import org.catnut.R;
import org.catnut.support.TouchImageView;
import org.catnut.ui.HelloActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 查看fantasy
 *
 * @author longkai
 */
public class FantasyFragment extends Fragment {

	private static final String TAG = "FantasyFragment";
	private static final String NAME = "name";
	private static final String FIT_XY = "fit_xy";

	private String mUrl;
	private TouchImageView mFantasy;

	private MenuItem mShare;
	private Intent mIntent;
	private ShareActionProvider mShareActionProvider;
	private File mImage;
	private boolean mSaved;

	private Target target = new Target() {
		@Override
		public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					FileOutputStream os = null;
					try {
						String dir = CatnutUtils.mkdir(getActivity(), Constants.FANTASY_DIR);
						String[] paths = Uri.parse(mUrl).getPath().split("/");
						mImage = new File(dir + File.separator + paths[2] + Constants.JPG);
					} catch (Exception e) {
						Log.e(TAG, "create dir error!", e);
						return;
					}
					try {
						if (mImage.length() > 10) { // 10是随机取的，>0就ok了
							// 文件已经存在，没有必要再来一次了
							mIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mImage));
							mSaved = true;
							return;
						}
						os = new FileOutputStream(mImage);
						bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
						mIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mImage));
						mSaved = true;
					} catch (FileNotFoundException e) {
						Log.e(TAG, "io error!", e);
					} finally {
						if (os != null) {
							try {
								os.close();
							} catch (IOException e) {
								Log.e(TAG, "io closing error!", e);
							}
						}
					}
				}
			}).start();
			mFantasy.setImageBitmap(bitmap);
		}

		@Override
		public void onBitmapFailed(Drawable errorDrawable) {
			mFantasy.setImageDrawable(errorDrawable);
		}

		@Override
		public void onPrepareLoad(Drawable placeHolderDrawable) {
			if (placeHolderDrawable != null) {
				mFantasy.setImageDrawable(placeHolderDrawable);
			}
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Bundle args = getArguments();
		mUrl = args.getString(TAG);
		mIntent = new Intent(Intent.ACTION_SEND)
				.setType(getString(R.string.mime_image))
				.putExtra(Intent.EXTRA_TITLE, args.getString(NAME))
				.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.fantasy_share, args.getString(NAME)));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	public static FantasyFragment getFragment(String url, String name, boolean fitXY) {
		Bundle args = new Bundle();
		args.putString(TAG, url);
		args.putString(NAME, name);
		args.putBoolean(FIT_XY, fitXY);
		FantasyFragment fantasyFragment = new FantasyFragment();
		fantasyFragment.setArguments(args);
		return fantasyFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.photo, container, false);
		mFantasy = (TouchImageView) view.findViewById(R.id.image);
		if (getArguments().getBoolean(FIT_XY)) {
			mFantasy.setScaleType(ImageView.ScaleType.FIT_XY);
		}
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		boolean fitXY = getArguments().getBoolean(FIT_XY);
		if (getActivity() instanceof HelloActivity) {
			if (!((HelloActivity) getActivity()).isNetworkAvailable()) {
				if (fitXY) {
					Toast.makeText(getActivity(), R.string.network_unavailable, Toast.LENGTH_SHORT).show();
					mFantasy.setImageResource(R.drawable.default_fantasy);
					return; // 没有网络，直接结束第一张fantasy
				}
			}
		}
		RequestCreator creator = Picasso.with(getActivity()).load(mUrl);
		if (fitXY) {
			creator.placeholder(R.drawable.default_fantasy);
		}
		creator.error(R.drawable.error)
				.into(target);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		mShare.setEnabled(mSaved);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fantasy, menu);
		mShare = menu.findItem(R.id.action_share);
		if (!Intent.ACTION_MAIN.equals( getActivity().getIntent().getAction())) {
			menu.findItem(R.id.home).setVisible(false);
		}
		mShareActionProvider = (ShareActionProvider) mShare.getActionProvider();
		mShare.setActionProvider(mShareActionProvider);

		mShareActionProvider.setShareIntent(mIntent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (getActivity() instanceof HelloActivity) {
			((HelloActivity) getActivity()).onMenuItemSelected(item);
		}
		return true;
	}
}
