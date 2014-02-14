/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.metadata;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 获得新浪的OAuth2授权后字段。
 *
 * @author longkai
 * @date 2013-01-18
 */
public class AccessToken implements Parcelable {

	public static final String UID = "uid";
	public static final String EXPIRES_IN = "expires_in";
	public static final String ACCESS_TOKEN = "access_token";

	public final long uid;
	// 从新浪那里拿回来是秒，但是如果保存用户信息后便会还原为毫秒值！！！
	public final long expires_in;
	public final String access_token;

	public AccessToken(long uid, long expires_in, String access_token) {
		this.uid = uid;
		this.expires_in = expires_in;
		this.access_token = access_token;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(uid);
		dest.writeLong(expires_in);
		dest.writeString(access_token);
	}

	public static final Parcelable.Creator<AccessToken> CREATOR = new Creator<AccessToken>() {

		@Override
		public AccessToken createFromParcel(Parcel source) {
			return new AccessToken(source.readLong(),
				source.readLong(),
				source.readString());
		}

		@Override
		public AccessToken[] newArray(int size) {
			return new AccessToken[size];
		}
	};
}
