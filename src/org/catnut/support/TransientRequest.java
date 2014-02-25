/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;

import java.util.Map;

/**
 * 获取瞬时的用户列表，这意味着必须有网呀
 *
 * @author longkai
 */
public abstract class TransientRequest<T> extends Request<T> {

	private CatnutAPI mApi;
	private Response.Listener<T> mListener;

	public TransientRequest(CatnutAPI api, Response.Listener<T> listener, Response.ErrorListener errorListener) {
		super(api.method, api.uri, errorListener);
		mListener = listener;
		mApi = api;
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return mApi.authRequired ? CatnutApp.getAuthHeaders() : super.getHeaders();
	}

	@Override
	protected void deliverResponse(T response) {
		if (mListener != null) {
			mListener.onResponse(response);
		}
	}
}

