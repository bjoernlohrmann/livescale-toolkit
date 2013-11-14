package de.tuberlin.cit.livestream.android.activity.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ImageView;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.Broadcast;

/**
 * Dialog Class for displaying a QR-Code representing an URL so access the
 * currently broadcasted video stream
 * 
 * @author Stefan Werner
 */
public class ShowQRCodeDialogFragment extends DialogFragment {

	/**
	 * Broadcast activity opening this dialog
	 */
	private Broadcast mBroadcastActivity;

	/**
	 * The Bitmap containing the QR-Code
	 */
	private Bitmap bitmap;

	/**
	 * Constructor
	 * 
	 * @param bitmap
	 *            the QR-Code image to display
	 */
	public ShowQRCodeDialogFragment(Bitmap bitmap) {
		super();
		this.bitmap = bitmap;
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
		View dialogQRCodeLayout = getActivity().getLayoutInflater().inflate(
				R.layout.dialog_image, null);
		ImageView qrImageView = (ImageView) dialogQRCodeLayout
				.findViewById(R.id.dialog_qrcode_iv);
		qrImageView.setImageBitmap(bitmap);

		AlertDialog dialog = new AlertDialog.Builder(mBroadcastActivity)
				.setTitle(R.string.dialog_qrcode_title)
				.setView(dialogQRCodeLayout)
				.setNegativeButton(getString(R.string.button_close),
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