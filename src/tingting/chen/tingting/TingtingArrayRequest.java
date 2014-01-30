/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.tingting;

import android.content.Context;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import org.json.JSONArray;
import org.json.JSONException;
import tingting.chen.TingtingApp;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * http json array请求抽象，子类需要持久化json array，并且进行潜在的错误处理。
 *
 * @author longkai
 */
public class TingtingArrayRequest extends JsonRequest<JSONArray> {

	public static final String TAG = "TingtingArrayRequest";

	protected Context mContext;
	protected TingtingAPI mApi;
	protected Response.Listener<JSONArray> mListener;
	protected TingtingProcessor<JSONArray> mProcessor;

	public TingtingArrayRequest(Context context,
								TingtingAPI api,
								TingtingProcessor<JSONArray> processor,
								Response.Listener<JSONArray> listener,
								Response.ErrorListener errorListener) {
		super(api.method, api.uri, null, listener, errorListener);
		mContext = context;
		mApi = api;
		mProcessor = processor;
		mListener = listener;
	}

	@Override
	protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
		try {
			String jsonString =
					new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			Response<JSONArray> success = Response.success(new JSONArray(jsonString),
					HttpHeaderParser.parseCacheHeaders(response));
			if (mProcessor != null) {
				// do in background...
				mProcessor.asyncProcess(mContext, success.result);
			}
			return success;
		} catch (UnsupportedEncodingException e) {
			return Response.error(new ParseError(e));
		} catch (JSONException je) {
			return Response.error(new ParseError(je));
		}
	}

	@Override
	protected void deliverResponse(JSONArray response) {
		if (mListener == null) {
			Log.d(TAG, "finish http request without response on main-thread!");
		} else {
			mListener.onResponse(response);
		}
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return mApi.authRequired ? TingtingApp.getAuthHeaders() : super.getHeaders();
	}

	@Override
	protected Map<String, String> getParams() throws AuthFailureError {
		return mApi.params == null ? super.getParams() : mApi.params;
	}
}
