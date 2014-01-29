/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.beans;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 微博配图地址（目测是多图）
 *
 * @author longkai
 */
public class PicUrl implements Parcelable {

	public String thumbnail_pic;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

	}
}
