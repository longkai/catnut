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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;
import org.catnut.R;
import org.catnut.core.CatnutProvider;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

import java.io.File;

/**
 * 清除缓存
 *
 * @author longkai 
 */
public class ClearCacheBoxFragment extends DialogFragment {

	private static final String TAG = ClearCacheBoxFragment.class.getSimpleName();

	private Handler mHandler = new Handler();

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.clear_cache, null);
		final CheckBox clearImages = (CheckBox) view.findViewById(R.id.clear_images);
		clearImages.setText(getString(R.string.clear_images, ""));
		final CheckBox clearFantasy = (CheckBox) view.findViewById(R.id.clear_fantasies);
		clearFantasy.setText(getString(R.string.clear_fantasies, ""));
		final CheckBox clearSqlite = (CheckBox) view.findViewById(R.id.clear_sqlite);
		new Thread(new Runnable() {
			@Override
			public void run() {
				File imagesDir = new File(getActivity().getExternalCacheDir() + File.separator + Constants.IMAGE_DIR);
				long total = 0;
				if (imagesDir.exists() && imagesDir.isDirectory()) {
					for (File file : imagesDir.listFiles()) {
						total += file.length();
					}
				}
				final float imagesMb = CatnutUtils.scaleNumber(total * 1.f / 1024 / 1024, 2);

				File fantasyDir = new File(getActivity().getExternalCacheDir() + File.separator + Constants.FANTASY_DIR);
				total = 0;
				if (fantasyDir.exists() && fantasyDir.isDirectory()) {
					for (File file : fantasyDir.listFiles()) {
						total += file.length();
					}
				}
				final float fantasiesMb = CatnutUtils.scaleNumber(total * 1.f / 1024 / 1024, 2);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						clearImages.setText(getString(R.string.clear_images, imagesMb + "m"));
						clearFantasy.setText(getString(R.string.clear_fantasies, fantasiesMb + "m"));
					}
				});
			}
		}).start();
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.clear_cache)
				.setView(view)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						clear(clearImages.isChecked(), clearFantasy.isChecked(), clearSqlite.isChecked());
					}
				})
				.create();
		return dialog;
	}

	private void clear(final boolean images, final boolean fantasies, final boolean sqlite) {
		final ProgressDialog dialog = ProgressDialog.show(getActivity(), null, getString(R.string.clearing), true, false);
		dialog.show();
		final Activity activity = getActivity();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (images) {
						File imagesDir = new File(getActivity().getExternalCacheDir() + File.separator + Constants.IMAGE_DIR);
						if (imagesDir.exists()) {
							for (File file : imagesDir.listFiles()) {
								file.delete();
							}
						}
					}
					if (fantasies) {
						File fantasyDir = new File(getActivity().getExternalCacheDir() + File.separator + Constants.FANTASY_DIR);
						if (fantasyDir.exists()) {
							for (File file : fantasyDir.listFiles()) {
								file.delete();
							}
						}
					}
					if (sqlite) {
						getActivity().getContentResolver().delete(CatnutProvider.clear(), null, null);
					}
				} catch (final Exception e) {
					Log.e(TAG, "clear cache error!", e);
					dialog.dismiss();
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						}
					});
					return;
				}

				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(activity, activity.getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show();
						dialog.dismiss();
					}
				});
			}
		}).start();
	}
}
