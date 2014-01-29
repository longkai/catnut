/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.metadata;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.android.volley.VolleyError;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 新浪微博调用API出错时的字段封装
 *
 * @author longkai
 * @date 2014-01-19
 */
public class WeiboAPIError implements Parcelable {

	private static final String TAG = "WeiboAPIError";

	public static final String ERROR_CODE = "error_code";
	public static final String REQUEST = "request";
	public static final String ERROR = "error";

	public final int error_code;
	public final String request;
	public final String error;

	public WeiboAPIError(int error_code, String request, String error) {
		this.error_code = error_code;
		this.request = request;
		this.error = error;
	}

	/**
	 * 从volley中解析错误
	 *
	 * @param volleyError
	 * @return WeiboAPIError
	 */
	public static WeiboAPIError fromVolleyError(VolleyError volleyError) {
		// 很可能出现response body为空的情况
		String jsonString;
		try {
			jsonString = new String(volleyError.networkResponse.data);
		} catch (Exception e) {
			Log.wtf(TAG, e);
			// fall back to http error!
			String msg = volleyError.getLocalizedMessage();
			return new WeiboAPIError(
				volleyError.networkResponse.statusCode,
				null,
				TextUtils.isEmpty(msg) ? "Known error!" : msg);
		}

		try {
			JSONObject jsonObject = new JSONObject(jsonString);
			return new WeiboAPIError(
				jsonObject.optInt(ERROR_CODE),
				jsonObject.optString(REQUEST),
				jsonObject.optString(ERROR)
			);
		} catch (JSONException e) {
			Log.wtf(TAG, e.getLocalizedMessage());
		}
		// never happen！
		return null;
	}

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
			return new WeiboAPIError(
				source.readInt(),
				source.readString(),
				source.readString()
			);
		}

		@Override
		public WeiboAPIError[] newArray(int size) {
			return new WeiboAPIError[size];
		}
	};
}
