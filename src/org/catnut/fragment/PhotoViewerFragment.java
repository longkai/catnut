/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.ShareActionProvider;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import org.catnut.R;
import org.catnut.support.TouchImageView;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * 照片查看器
 *
 * @author longkai
 */
public class PhotoViewerFragment extends Fragment {

	private static final String TAG = "PhotoViewerFragment";

	private String mUri;

	private TouchImageView mImageView;
	private MenuItem mShare;
	private ShareActionProvider mShareActionProvider;

	private File mImage;
	private Intent mIntent;
	private boolean mSaved;

	// see picasso issues, keep it a member field. https://github.com/square/picasso/issues/38
	private Target mTarget = new Target() {
		@Override
		public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					FileOutputStream os = null;
					try {
						String dir = CatnutUtils.mkdir(getActivity(), Constants.IMAGE_DIR);
						mImage = new File(dir + File.separator + Uri.parse(mUri).getLastPathSegment());
					} catch (Exception e) {
						return;
					}
					try {
						if (mImage.length() > 10) { // 10是随机取的，>0就ok了
							// 文件已经存在，没有必要再来一次了
							mIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mImage));
							mSaved = true;
						} else {
							os = new FileOutputStream(mImage);
							bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
							mIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mImage));
							mSaved = true;
						}
					} catch (FileNotFoundException e) {
						Log.e(TAG, "io error!", e);
					} finally {
						CatnutUtils.closeIO(os);
					}
				}
			}).start();
			mImageView.setImageBitmap(bitmap);
		}

		@Override
		public void onBitmapFailed(Drawable errorDrawable) {
			mImageView.setImageDrawable(errorDrawable);
		}

		@Override
		public void onPrepareLoad(Drawable placeHolderDrawable) {
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
		bar.setDisplayShowHomeEnabled(true);
		bar.setIcon(R.drawable.ic_title_content_picture_dark);
		mUri = getArguments().getString(TAG);
		setHasOptionsMenu(true);
		mIntent = new Intent(Intent.ACTION_SEND).setType(getString(R.string.mime_image));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.photo, container, false);
		mImageView = (TouchImageView) view.findViewById(R.id.image);
		mImageView.setMaxZoom(7f); // 放大一些，有些长微博畸形啊。。。
		mImageView.setBackground(new ColorDrawable(getResources().getColor(R.color.black_background)));
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Picasso.with(getActivity())
				.load(mUri)
				.error(R.drawable.error)
				.into(mTarget);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		mShare = menu.add(Menu.NONE, R.id.action_share, Menu.NONE, R.string.share);
		mShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		mShareActionProvider = new ShareActionProvider(getActivity());
		mShareActionProvider.setShareIntent(mIntent);
		mShare.setActionProvider(mShareActionProvider);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		mShare.setEnabled(mSaved);
	}
}
