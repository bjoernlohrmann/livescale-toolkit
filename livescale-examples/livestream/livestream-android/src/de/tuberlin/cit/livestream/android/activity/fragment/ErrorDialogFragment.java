package de.tuberlin.cit.livestream.android.activity.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.Broadcast;

public class ErrorDialogFragment extends DialogFragment {
	// private static final String TAG =
	// ErrorDialogFragment.class.getSimpleName();

	private Broadcast broadcastActivity;

	private String errorMessage;

	public ErrorDialogFragment(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		broadcastActivity = (Broadcast) getActivity();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog dialog = new AlertDialog.Builder(broadcastActivity)
				.setMessage(errorMessage)
				.setPositiveButton(getString(R.string.button_ok),
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dismiss();
							}
						}).setCancelable(false).create();

		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}
}
