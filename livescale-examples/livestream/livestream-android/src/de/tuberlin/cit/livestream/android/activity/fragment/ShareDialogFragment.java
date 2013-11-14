package de.tuberlin.cit.livestream.android.activity.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.Broadcast;

/**
 * Dialog Class for displaying a list of available social sharing providers
 * 
 * @author Stefan Werner
 */
public class ShareDialogFragment extends DialogFragment {

	/**
	 * Broadcast activity opening this dialog
	 */
	private Broadcast mBroadcastActivity;

	/**
	 * Constructor
	 */
	public ShareDialogFragment() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.DialogFragment#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		mBroadcastActivity = (Broadcast) getActivity();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog dialog = new AlertDialog.Builder(mBroadcastActivity)
				.setTitle(R.string.dialog_share_title)
				.setItems(R.array.share_providers,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mBroadcastActivity.runShareProvider(which);
								dialog.dismiss();
							}
						})
				.setNegativeButton(getString(R.string.button_cancel),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						}).create();
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}
}