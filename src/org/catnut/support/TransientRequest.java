/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * 获取瞬时的用户列表，这意味着必须有网呀
 *
 * @author longkai
 */
public abstract class TransientRequest<T> extends JsonRequest<T> {

	private CatnutAPI mApi;

	public TransientRequest(CatnutAPI api, Response.Listener<T> listener, Response.ErrorListener errorListener) {
		super(api.method, api.uri, null, listener, errorListener);
		mApi = api;
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return mApi.authRequired ? CatnutApp.getAuthHeaders() : super.getHeaders();
	}

	@Override
	protected Map<String, String> getParams() throws AuthFailureError {
		return mApi.params;
	}
}

