/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import tingting.chen.R;
import tingting.chen.util.Constants;

/**
 * 错误提示信息对话框，仅仅是弹出信息提示对话框而已，并不做任何处理！
 * 至少需要提供一个参数，错误信息，另一个可选参数，标题
 *
 * @author longkai
 * @date 2014-01-20
 */
public class ErrorDialogFragment extends DialogFragment {

	/**
	 * 简化构造一个错误提示框
	 * @param title
	 * @param message
	 * @return ErrorDialogFragment
	 */
	public static ErrorDialogFragment newInstance(String title, String message) {
		assert message != null;
		Bundle args = new Bundle();
		args.putString(Constants.TITLE, title);
		args.putString(Constants.MESSAGE, message);
		ErrorDialogFragment fragment = new ErrorDialogFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		// todo: what about the different themes?
		return new AlertDialog.Builder(getActivity())
			.setIcon(R.drawable.alerts_and_states_error_light)
			.setTitle(args.getString(Constants.TITLE, getString(R.string.alert_error)))
			.setMessage(args.getString(Constants.MESSAGE))
			.setNeutralButton(android.R.string.ok, null)
			.create();
	}
}
