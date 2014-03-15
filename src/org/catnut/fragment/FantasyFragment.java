/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
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

	private Bitmap mBitmap;
	private MyShareActionProvider mShareActionProvider;

	private Target target = new Target() {
		@Override
		public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
			mBitmap = bitmap;
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
		RequestCreator creator = Picasso.with(getActivity())
				.load(mUrl);
		if (getArguments().getBoolean(FIT_XY)) {
			creator.placeholder(R.drawable.default_fantasy);
		}
		creator.error(R.drawable.error)
				.into(target);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem share = menu.add(Menu.NONE, R.id.action_share, 0, R.string.share);
		share.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		try {
			mShareActionProvider = new MyShareActionProvider(getActivity(), mUrl);
		} catch (Exception e) {
			Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
		share.setActionProvider(mShareActionProvider);
		mShareActionProvider.setShareIntent(mShareActionProvider.getShareIntent());
	}

	private class MyShareActionProvider extends ShareActionProvider {

		private String mDir;
		private File mImage;

		public MyShareActionProvider(Context context, String url) throws Exception {
			super(context);
			mDir = CatnutUtils.createFantasyDir(context);
			String[] paths = Uri.parse(url).getPath().split("/");
			mImage = new File(mDir + File.separator + paths[2] + Constants.JPG);
		}

		public Intent getShareIntent() {
			Intent intent = new Intent(Intent.ACTION_SEND).setType(getString(R.string.mime_image));
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mImage));
			return intent;
		}

		@Override
		public View onCreateActionView() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (mBitmap == null) {
						return;
					}
					FileOutputStream os = null;
					try {
						if (mImage.length() > 10) { // 10是随机取的，>0就ok了
							// 文件已经存在，没有必要再来一次了
							return;
						}
						os = new FileOutputStream(mImage);
						mBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
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
			return super.onCreateActionView();
		}

		@Override
		public void setOnShareTargetSelectedListener(OnShareTargetSelectedListener listener) {
			if (mBitmap != null) {
				super.setOnShareTargetSelectedListener(listener);
			}
		}
	}
}
