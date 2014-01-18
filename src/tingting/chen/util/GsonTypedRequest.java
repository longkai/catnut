/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Volley adapter for JSON requests that will be parsed into Java objects by Gson.
 */
public class GsonTypedRequest<T> extends Request<T> {
	private final Gson gson = HttpUtils.getGson();
	private final Type type;
	private final Map<String, String> headers;
	private final Listener<T> listener;

	/**
	 * Make a GET request and return a parsed object from JSON.
	 *
	 * @param url     URL of the request to make
	 * @param type    Relevant class object, for Gson's reflection
	 * @param headers Map of request headers
	 */
	public GsonTypedRequest(String url, Type type, Map<String, String> headers,
							Listener<T> listener, ErrorListener errorListener) {
		super(Method.GET, url, errorListener);
		this.type = type;
		this.headers = headers;
		this.listener = listener;
	}

	/**
	 * Make a REST request and return a parsed object from JSON.
	 *
	 * @param method  HTTP Method
	 * @param url     URL of the request to make
	 * @param type    Relevant type object, for Gson's reflection
	 * @param headers Map of request headers
	 */
	public GsonTypedRequest(int method, String url, Type type, Map<String, String> headers,
							Listener<T> listener, ErrorListener errorListener) {
		super(method, url, errorListener);
		this.type = type;
		this.headers = headers;
		this.listener = listener;
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return headers != null ? headers : super.getHeaders();
	}

	@Override
	protected void deliverResponse(T response) {
		listener.onResponse(response);
	}

	@Override
	protected Response parseNetworkResponse(NetworkResponse response) {
		try {
			String json = new String(
				response.data, HttpHeaderParser.parseCharset(response.headers));
			return Response.success(
				gson.fromJson(json, type), HttpHeaderParser.parseCacheHeaders(response));
		} catch (UnsupportedEncodingException e) {
			return Response.error(new ParseError(e));
		} catch (JsonSyntaxException e) {
			return Response.error(new ParseError(e));
		}
	}

}