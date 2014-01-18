/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import tingting.chen.R;
import tingting.chen.beans.AccessToken;
import tingting.chen.fragments.OAuthFragment;
import tingting.chen.util.GsonRequest;

/**
 * 应用程序主界面。
 * todo 后面需要好好设计界面
 *
 * @author longkai
 * @date 2014-01-18
 */
public class MainActivity extends Activity {

	public static final String TAG = "MainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_container);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, new OAuthFragment())
				.commit();
		}
	}

}