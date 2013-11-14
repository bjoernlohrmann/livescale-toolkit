package de.tuberlin.cit.livestream.android.activity.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.Videoplayer;

public class VideoplayerBufferingDialogFragment extends DialogFragment {

	private Videoplayer videoplayerActivity;
	private ProgressDialog dialog;

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		videoplayerActivity = (Videoplayer) getActivity();
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		this.dialog = new ProgressDialog(videoplayerActivity);
		dialog.setMessage(getString(R.string.videplayer_buffering));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}
}
