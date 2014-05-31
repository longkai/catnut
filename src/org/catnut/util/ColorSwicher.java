/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.util;

import android.support.v4.widget.SwipeRefreshLayout;
import org.catnut.R;

import java.util.Random;

/**
 * a color switcher for the swipe refresh layout
 *
 * @author longkai
 */
public class ColorSwicher {
	static Random random = new Random();

	public static final int[] COLORS = new int[]{
			android.R.color.holo_blue_light,
			android.R.color.holo_green_light,
			android.R.color.holo_orange_light,
			android.R.color.holo_purple,
			android.R.color.holo_red_light,
			R.color.actionbar_background,
			R.color.cardTableColor,
			R.color.tab_selected
	};

	public static void injectColor(SwipeRefreshLayout swipeRefreshLayout) {
		int[] colors = ramdomColors(4);
		swipeRefreshLayout.setColorScheme(colors[0], colors[1], colors[2], colors[3]);
	}

	public static int[] ramdomColors(int n) {
		int[] colors = new int[n];
		for (int i = 0; i < n; i++) {
			int c = random.nextInt(COLORS.length - i);
			swap(COLORS, c, COLORS.length - i - 1);
			colors[i] = COLORS[COLORS.length - i - 1];
		}
		return colors;
	}

	private static void swap(int[] a, int i, int j) {
		if (i != j) {
			a[i] += a[j];
			a[j] = a[i] - a[j];
			a[i] = a[i] - a[j];
		}
	}
}
