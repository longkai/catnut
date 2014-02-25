/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.content.Context;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProcessor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

/**
 * 支持上传图片的request！
 *
 * @author longkai
 */
public class MultiPartRequest extends Request<JSONObject> {

	private static final String TAG = "MultiPartRequest";

	private static String delimiter = "--";
	private String boundary = "SwA" + System.currentTimeMillis() + "SwA";

	private ByteArrayOutputStream os;

	private Context mContext;
	private CatnutAPI mApi;
	private CatnutProcessor<JSONObject> mProcessor;
	private Response.Listener<JSONObject> mListener;

	public MultiPartRequest(Context context, CatnutAPI api, CatnutProcessor<JSONObject> processor,
							Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		super(api.method, api.uri, errorListener);
		mContext = context;
		mApi = api;
		mProcessor = processor;
		mListener = listener;
		os = new ByteArrayOutputStream();
	}

	@Override
	public String getBodyContentType() {
		return "multipart/form-data; boundary=" + boundary;
	}

	@Override
	protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
		try {
			String jsonString =
					new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			Response<JSONObject> success = Response.success(new JSONObject(jsonString),
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
			return Response.error(new VolleyError(response));
		}
	}

	@Override
	public byte[] getBody() throws AuthFailureError {
		return os.toByteArray();
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return mApi.authRequired ? CatnutApp.getAuthHeaders() : Collections.EMPTY_MAP;
	}

	@Override
	protected void deliverResponse(JSONObject response) {
		if (mListener != null) {
			mListener.onResponse(response);
		} else {
			Log.d(TAG, "finish multipart request without response on main-thread!");
		}
	}

	public MultiPartRequest addFormPart(String paramName, String value) throws Exception {
		os.write((delimiter + boundary + "\r\n").getBytes());
		os.write("Content-Type: text/plain\r\n".getBytes());
		os.write(("Content-Disposition: form-data; name=\"" + paramName + "\"\r\n").getBytes());
		os.write(("\r\n" + value + "\r\n").getBytes());
		return this;
	}

	public MultiPartRequest addFilePart(String paramName, String fileName, byte[] data) throws Exception {
		os.write((delimiter + boundary + "\r\n").getBytes());
		os.write(("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
		os.write(("Content-Type: application/octet-stream\r\n").getBytes());
		os.write(("Content-Transfer-Encoding: binary\r\n").getBytes());
		os.write("\r\n".getBytes());

		os.write(data);

		os.write("\r\n".getBytes());
		return this;
	}
}
