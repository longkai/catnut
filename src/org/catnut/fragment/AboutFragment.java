/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by longkai on 14-3-4.
 */
public class AboutFragment extends Fragment {

	private RequestQueue mRequestQueue;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mRequestQueue = CatnutApp.getTingtingApp().getRequestQueue();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.about, container, false);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		TextView about = (TextView) view.findViewById(R.id.about_body);
		about.setText(Html.fromHtml(getString(R.string.about_body)));
		mRequestQueue.add(new JsonObjectRequest(
				"https://api.500px.com/v1/photos?feature=popular&consumer_key=HocY5wY9GQaa9sdNO9HvagCGuGt34snyMTHckIQJ",
				null,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(getTag(), response.toString());
						JSONArray photos = response.optJSONArray("photos");
						int total = photos.length();
						int index = (int) (Math.random() * total) + 1;
						String image_url = photos.optJSONObject(index).optString("image_url");
						Drawable def = getResources().getDrawable(R.raw.starrynight);
						Picasso.with(getActivity())
								.load(image_url)
								.placeholder(def)
								.error(def)
								.into((ImageView) view.findViewById(R.id.fantasy));
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d(getTag(), error.getLocalizedMessage());
					}
				}
		));
	}
}
