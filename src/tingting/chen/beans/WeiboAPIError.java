/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.beans;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 新浪微博调用API出错时的字段封装
 *
 * @author longkai
 * @date 2014-01-19
 */
public class WeiboAPIError implements Parcelable {

	public int error_code;
	public String request;
	public String error;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(error_code);
		dest.writeString(request);
		dest.writeString(error);
	}

	public static final Parcelable.Creator<WeiboAPIError> CREATOR = new Creator<WeiboAPIError>() {
		@Override
		public WeiboAPIError createFromParcel(Parcel source) {
			WeiboAPIError error = new WeiboAPIError();
			error.error_code = source.readInt();
			error.request = source.readString();
			error.error = source.readString();
			return error;
		}

		@Override
		public WeiboAPIError[] newArray(int size) {
			return new WeiboAPIError[size];
		}
	};
}
