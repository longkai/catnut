/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */

package org.catnut.support;

import com.android.volley.toolbox.HurlStack;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * okhttp with volley!
 *
 * @author longkai
 */
public class OkHttpStack extends HurlStack {
	/** singleton */
	private final OkHttpClient client;
	public OkHttpStack(OkHttpClient client) {
		if (client == null) {
			throw new NullPointerException("client cannot be null!");
		}
		okHttpClient = client;
		this.client = client;
		this.urlFactory = new OkUrlFactory(this.client);
	}

	public OkHttpStack() {
		this(getOkHttpClient());
	}

	private static OkUrlFactory urlFactory;
	private static OkHttpClient okHttpClient;


	public static OkHttpClient getOkHttpClient() {
		if (okHttpClient == null) {
			okHttpClient = new OkHttpClient();
		}
		return okHttpClient;
	}

	public static OkUrlFactory getOkUrlFactory() {
		return urlFactory;
	}

	@Override
	protected HttpURLConnection createConnection(URL url) throws IOException {
		return urlFactory.open(url);
	}
}
