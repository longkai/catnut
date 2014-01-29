/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.ActionBar;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import tingting.chen.R;
import tingting.chen.TingtingApp;
import tingting.chen.metadata.AccessToken;
import tingting.chen.beans.WeiboAPIError;
import tingting.chen.util.GsonRequest;
import tingting.chen.util.HttpUtils;
import tingting.chen.util.Manifest;

/**
 * 使用Oauth2的方式获取新浪微博的认证
 *
 * @author longkai
 * @date 2014-01-18
 */
public class OAuthFragment extends Fragment {

	public static final String TAG = "OAuthFragment";

	private WebView mWebView;
	private ActionBar mActionBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// set actionbar
		mActionBar = getActivity().getActionBar();
		setHasOptionsMenu(true);

		// set the auth webview
		mWebView = new WebView(getActivity());
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		mWebView.setLayoutParams(params);

		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				String code = Uri.parse(url).getQueryParameter("code");
				Log.d(TAG, "the auth code is " + code);
				String accessTokenUri = Manifest.getAccessTokenUri(code);
				TingtingApp.getRequestQueue()
					.add(new GsonRequest<AccessToken>(
						Request.Method.POST,
						accessTokenUri,
						AccessToken.class,
						null,
						new Response.Listener<AccessToken>() {
							@Override
							public void onResponse(AccessToken accessToken) {
								Log.i(TAG, "auth success with uid: " + accessToken.uid);
								TingtingApp.getTingtingApp().saveAccessToken(accessToken);
								// todo 优雅地处理授权成功后的跳转
								getFragmentManager().beginTransaction()
									.replace(R.id.fragment_container, new Fragment() {
										@Override
										public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
											return inflater.inflate(R.layout.main, container, false);
										}
									}).commit();
							}
						},
						new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								Log.wtf(TAG, "auth fail!", error);
								WeiboAPIError e = HttpUtils.fromVolleyError(error);
								ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(null, e.error);
								fragment.show(getFragmentManager(), null);
							}
						}
					));
				return true;
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return mWebView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// clear cache
		WebSettings settings = mWebView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		settings.setSaveFormData(false);
		mWebView.loadUrl(Manifest.getOAuthUri());

		// set actionbar title
		mActionBar.setTitle(getString(R.string.oauth));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem refresh = menu.add(Menu.NONE, R.id.refresh, Menu.NONE, getString(R.string.refresh));
		refresh.setIcon(R.drawable.navigation_refresh_light);
		refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				mWebView.reload();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		mWebView.clearCache(true);
		super.onDestroy();
	}
}
