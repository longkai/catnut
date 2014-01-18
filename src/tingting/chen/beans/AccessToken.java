/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.beans;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 获得新浪的OAuth2授权后字段。
 *
 * @author longkai
 * @date 2013-01-18
 */
public class AccessToken implements Parcelable {

	public long uid;
	public long remind_in; // seconds!
	public long expires_in;
	public String access_token;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(uid);
		dest.writeLong(remind_in);
		dest.writeLong(expires_in);
		dest.writeString(access_token);
	}

	public static final Parcelable.Creator<AccessToken> CREATOR = new Creator<AccessToken>() {

		@Override
		public AccessToken createFromParcel(Parcel source) {
			AccessToken accessToken = new AccessToken();
			accessToken.uid = source.readLong();
			accessToken.remind_in = source.readLong();
			accessToken.expires_in = source.readLong();
			accessToken.access_token = source.readString();
			return accessToken;
		}

		@Override
		public AccessToken[] newArray(int size) {
			return new AccessToken[size];
		}
	};
}
