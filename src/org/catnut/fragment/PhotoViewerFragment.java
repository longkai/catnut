/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import org.catnut.R;
import org.catnut.support.TouchImageView;
import org.catnut.util.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 照片查看器
 *
 * @author longkai
 */
public class PhotoViewerFragment extends Fragment {

	private static final String TAG = "PhotoViewerFragment";

	private Handler mHandler = new Handler();

	private String mUri;

	private TouchImageView mImageView;
	private MenuItem mShare;
	private ShareActionProvider mShareActionProvider;

	// see picasso issues, keep it a member field. https://github.com/square/picasso/issues/38
	private Target mTarget = new Target() {
		@Override
		public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
			mImageView.setImageBitmap(bitmap);
			if (mShare == null) {
				return;
			}
			new Thread(new Runnable() {
				@Override
				public void run() {
					File dir = new File(getActivity().getExternalCacheDir().getPath() + "/" + Constants.IMAGE_DIR);
					if (!dir.exists() && !dir.mkdirs()) {
						return;
					}
					// can share this file!
					FileOutputStream os = null;
					try {
						String path = dir.getPath() + "/" + Uri.parse(mUri).getLastPathSegment();
						final File image = new File(path);
						if (image.length() > 10) { // 10是随机取的，>0就ok了
							// 文件已经存在，没有必要再来一次了
							return;
						}
						os = new FileOutputStream(image);
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								Intent intent = new Intent(Intent.ACTION_SEND).setType("image/*");
								intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(image));
								mShareActionProvider.setShareIntent(intent);
							}
						});
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
		}

		@Override
		public void onBitmapFailed(Drawable errorDrawable) {
			mImageView.setImageDrawable(errorDrawable);
		}

		@Override
		public void onPrepareLoad(Drawable placeHolderDrawable) {
			mImageView.setImageDrawable(placeHolderDrawable);
		}
	};

	public static PhotoViewerFragment getFragment(String url) {
		Bundle args = new Bundle();
		args.putString(TAG, url);
		PhotoViewerFragment fragment = new PhotoViewerFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		ActionBar bar = activity.getActionBar();
		bar.setTitle(activity.getString(R.string.view_photos));
		bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_overlay)));
		bar.setIcon(R.drawable.ic_title_content_picture_dark);
		mUri = getArguments().getString(TAG);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mImageView = new TouchImageView(getActivity());
		mImageView.setMaxZoom(3.5f); // 放大一些，有些长微博畸形啊。。。
		mImageView.setBackground(new ColorDrawable(getResources().getColor(R.color.black_background)));
		return mImageView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Picasso.with(getActivity())
				.load(mUri)
				.error(R.drawable.error)
				.placeholder(R.drawable.error)
				.into(mTarget);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		mShare = menu.add(Menu.NONE, R.id.action_share, Menu.NONE, R.string.share);
		mShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		mShareActionProvider = new ShareActionProvider(getActivity());
		mShare.setActionProvider(mShareActionProvider);
		mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
			@Override
			public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
				startActivity(intent);
				return true;
			}
		});
		menu.add(Menu.NONE, R.id.action_save, Menu.NONE, getString(R.string.save_photo))
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_save:
				String uri = getActivity().getExternalCacheDir()
						+ "/" + Constants.IMAGE_DIR + Uri.parse(mUri).getLastPathSegment();
				Toast.makeText(getActivity(), getString(R.string.save_at, uri), Toast.LENGTH_LONG).show();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
