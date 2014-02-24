/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.core;

import android.content.Context;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * http json array请求抽象，子类需要持久化json array，并且进行潜在的错误处理。
 *
 * @author longkai
 */
public class CatnutArrayRequest extends Request<JSONArray> {

	public static final String TAG = "CatnutArrayRequest";

	protected Context mContext;
	protected CatnutAPI mApi;
	protected Response.Listener<JSONArray> mListener;
	protected CatnutProcessor<JSONArray> mProcessor;

	public CatnutArrayRequest(
		Context context,
		CatnutAPI api,
		CatnutProcessor<JSONArray> processor,
		Response.Listener<JSONArray> listener,
		Response.ErrorListener errorListener) {
		super(api.method, api.uri, errorListener);
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
		} catch (Exception ex) {
			return Response.error(new VolleyError(ex));
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
		return mApi.authRequired ? CatnutApp.getAuthHeaders() : super.getHeaders();
	}

	@Override
	protected Map<String, String> getParams() throws AuthFailureError {
		return mApi.params == null ? super.getParams() : mApi.params;
	}
}
