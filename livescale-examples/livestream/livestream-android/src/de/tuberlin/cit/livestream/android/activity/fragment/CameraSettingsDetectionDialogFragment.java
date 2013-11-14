package de.tuberlin.cit.livestream.android.activity.fragment;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.SurfaceView;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.Broadcast;
import de.tuberlin.cit.livestream.android.broadcast.MediaSource;
import de.tuberlin.cit.livestream.android.broadcast.mp4.AvcConfigAutodetector;

public class CameraSettingsDetectionDialogFragment extends DialogFragment {

	private static final String TAG = CameraSettingsDetectionDialogFragment.class
			.getSimpleName();

	private MediaSource mediaSource;

	private Broadcast broadcastActivity;

	private FileOutputStream detectionFileOut;

	private ProgressDialog dialog;

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		broadcastActivity = (Broadcast) getActivity();
		mediaSource = (MediaSource) broadcastActivity.getMediaSource();
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		this.dialog = new ProgressDialog(broadcastActivity);
		// dialog.setTitle(R.string.login_title);
		dialog.setMessage(getString(R.string.camera_settings_detection_in_progress));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}

	public void onStart() {
		super.onStart();
		postStartSettingsDetection();
	}

	private void postStopSettingsDetection(long delay) {
		broadcastActivity.findViewById(R.id.cameraPreview).postDelayed(
				new Runnable() {
					@Override
					public void run() {
						try {
							mediaSource.stopCapture();
							detectionFileOut.close();
							if (!AvcConfigAutodetector.canDetect()) {
								dialog.setMessage(getString(R.string.camera_settings_detection_failed));
								Thread.sleep(300);
							} else {
								broadcastActivity
										.postDetectAvcSettingsAndEnableStreamButton(false);
							}
						} catch (IOException e) {
							Log.e(TAG,
									"Error when trying to stop AVC configuration capture",
									e);
						} catch (InterruptedException e) {
						}
						dismiss();
					}
				}, delay);
	}

	private void postStartSettingsDetection() {
		broadcastActivity.findViewById(R.id.cameraPreview).post(new Runnable() {
			@Override
			public void run() {
				try {
					SurfaceView cameraPreview = (SurfaceView) broadcastActivity
							.findViewById(R.id.cameraPreview);

					detectionFileOut = new FileOutputStream(
							AvcConfigAutodetector.getDetectionFile(), false);
					mediaSource.prepareCapture(detectionFileOut.getFD(),
							cameraPreview.getHolder());
					mediaSource.startCapture();
					postStopSettingsDetection(1500);
				} catch (Exception e) {
					Log.e(TAG,
							"Error when trying to start AVC configuration capture",
							e);
				}
			}
		});
	}
}
