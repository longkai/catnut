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
import org.json.JSONException;
import org.json.JSONObject;
import tingting.chen.TingtingApp;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * http json object请求抽象，子类需要对请求成功后的json object做进一步处理（如持久化等），该处理会在后台线程中执行。
 * <p/>
 * 如果需要在处理结束后更新ui，请传入非空的{@link com.android.volley.Response.Listener}，否则传入null
 *
 * @author longkai
 */
public class TingtingRequest extends JsonRequest<JSONObject> {

	public static final String TAG = "TingtingRequest";

	protected Context mContext;
	protected TingtingAPI mApi;
	protected TingtingProcessor<JSONObject> mProcessor;
	protected Response.Listener<JSONObject> mListener;

	public TingtingRequest(Context context,
						   TingtingAPI api,
						   TingtingProcessor<JSONObject> processor,
						   Response.Listener<JSONObject> listener,
						   Response.ErrorListener errorListener) {
		super(api.method, api.uri, null, listener, errorListener);
		mContext = context;
		mApi = api;
		mProcessor = processor;
		mListener = listener;
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
			return Response.error(new ParseError(ex));
		}
	}

	@Override
	protected void deliverResponse(JSONObject response) {
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
