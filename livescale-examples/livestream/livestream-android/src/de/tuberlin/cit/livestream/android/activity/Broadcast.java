package de.tuberlin.cit.livestream.android.activity;

import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ToggleButton;

import com.codegoogle.twitterandroid.TwitterApp;
import com.codegoogle.twitterandroid.TwitterAuthListener;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.jwetherell.quick_response_code.data.Contents;
import com.jwetherell.quick_response_code.qrcode.QRCodeEncoder;

import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.Helper;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.fragment.CameraSettingsDetectionDialogFragment;
import de.tuberlin.cit.livestream.android.activity.fragment.ErrorDialogFragment;
import de.tuberlin.cit.livestream.android.activity.fragment.ShareDialogFragment;
import de.tuberlin.cit.livestream.android.activity.fragment.ShowQRCodeDialogFragment;
import de.tuberlin.cit.livestream.android.broadcast.BCConstants;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener.ErrorReason;
import de.tuberlin.cit.livestream.android.broadcast.LoopbackToNetworkStreamer;
import de.tuberlin.cit.livestream.android.broadcast.MediaSettings;
import de.tuberlin.cit.livestream.android.broadcast.MediaSource;
import de.tuberlin.cit.livestream.android.broadcast.MediaSource.State;
import de.tuberlin.cit.livestream.android.broadcast.mp4.AvcConfigAutodetector;
import de.tuberlin.cit.livestream.android.view.SetupStreamingAsyncTask;
import de.tuberlin.cit.livestream.android.view.StatusOverlayView;
import de.tuberlin.cit.livestream.android.view.SurfaceHolderCallbackAdapter;

/**
 * Activity for sending video streams
 * 
 * @author Björn Lohrmann, Stefan Werner
 */
public class Broadcast extends FragmentActivity {

	/**
	 * log tag
	 */
	private final static String TAG = Broadcast.class.getSimpleName();

	/**
	 * streaming button
	 */
	private ToggleButton streamToggleButton;

	/**
	 * social sharing button
	 */
	private Button shareButton;

	/**
	 * video dater handler
	 */
	private LoopbackToNetworkStreamer streamer;

	/**
	 * view displaying camera live picture
	 */
	private SurfaceView cameraPreview;

	/**
	 * media recording service
	 */
	private MediaSource mediaSource;

	/**
	 * view for overlay text display
	 */
	private StatusOverlayView statusOverlay;

	/**
	 * preferences of the application
	 */
	private SharedPreferences mPrefs;

	/**
	 * ProgressDialog to show while connecting to stream server.
	 */
	public ProgressDialog progressDialog;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.broadcast_landscape);

		mediaSource = new MediaSource();

		cameraPreview = (SurfaceView) findViewById(R.id.cameraPreview);
		cameraPreview.getHolder().setType(
				SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		cameraPreview.getHolder().addCallback(
				new SurfaceHolderCallbackAdapter() {
					@Override
					public void surfaceCreated(SurfaceHolder holder) {
						try {
							mediaSource.initializePreview(cameraPreview
									.getHolder());
						} catch (IOException e) {
							Log.e(TAG,
									"Error when trying to initialize camera preview",
									e);
						}
						postDetectAvcSettingsAndEnableStreamButton(true);
					}
				});

		streamToggleButton = (ToggleButton) findViewById(R.id.streamToggleButton);
		streamToggleButton.setEnabled(false);
		streamToggleButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (streamToggleButton.isChecked()) {
					setupStreamingInAsyncTask();
				} else {
					postStopStreaming();
				}
			}
		});

		shareButton = (Button) findViewById(R.id.streamShareButton);
		shareButton.setEnabled(false);
		shareButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				postShareDialog();
			}
		});

		statusOverlay = (StatusOverlayView) findViewById(R.id.overlay);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();
		// getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		postStopStreaming();
		postStopPreview();
	}

	/**
	 * Runs video format autodetection if not already done
	 * 
	 * @param runAutodetectionIfNecessary
	 */
	public void postDetectAvcSettingsAndEnableStreamButton(
			final boolean runAutodetectionIfNecessary) {
		cameraPreview.post(new Runnable() {
			@Override
			public void run() {
				if (detectAvcSettings()) {
					streamToggleButton.setEnabled(true);
					shareButton.setEnabled(true);
				} else if (runAutodetectionIfNecessary) {
					postShowSettingsDetectionDialog();
				}
			}
		});
	}

	/**
	 * Detects available AVC settings
	 * 
	 * @return success status
	 */
	private boolean detectAvcSettings() {
		boolean success = false;

		if (AvcConfigAutodetector.canDetect()) {
			MediaSettings
					.initAvcConfig(AvcConfigAutodetector.detectAvcConfig());
			Log.i(TAG, "AVC settings successfully autodetected");
			success = true;
		}

		return success;
	}

	/**
	 * Stops the camera preview
	 */
	private void postStopPreview() {
		cameraPreview.post(new Runnable() {
			@Override
			public void run() {
				if (mediaSource.getState() == State.PREVIEWING) {
					mediaSource.stopPreview();
				}
			}
		});
	}

	/**
	 * Sets streaming components up
	 */
	private void setupStreamingInAsyncTask() {
		streamToggleButton.setEnabled(false);
		shareButton.setEnabled(false);
		showProgressDialog();
		SetupStreamingAsyncTask asyncTask = new SetupStreamingAsyncTask(this,
				getStringPref(Constants.PREF_USERNAME),
				getStringPref(Constants.PREF_PASSWORD));
		asyncTask.execute();
	}

	private void showProgressDialog() {
		progressDialog = ProgressDialog.show(this, null,
				getString(R.string.msg_connecting), true, false);
		progressDialog.setOwnerActivity(this);
		progressDialog.setCancelable(true);
		progressDialog.show();
	}

	public void dismissProgressDialogIfPresent() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	/**
	 * Starts the actual streaming
	 * 
	 * @param streamer
	 *            video data handler to use
	 */
	public void startStreaming(LoopbackToNetworkStreamer streamer) {
		try {
			dismissProgressDialogIfPresent();
			this.streamer = streamer;
			mediaSource.prepareCapture(streamer.getLoopbackTargetFD(),
					cameraPreview.getHolder());
			streamer.startStreaming();
			mediaSource.startCapture();
			streamToggleButton.setEnabled(true);
			shareButton.setEnabled(true);
		} catch (IOException e) {
			Log.e(TAG, "Error when starting streaming", e);
			handleStreamingFailure(ErrorReason.INTERNAL_ERROR);
		}
	}

	/**
	 * Callback for failure handling
	 * 
	 * @param failureReason
	 *            reason of the error
	 */
	public void handleStreamingFailure(ErrorReason failureReason) {
		dismissProgressDialogIfPresent();
		postStopStreaming();

		String errorMessage;
		if (failureReason == ErrorReason.SERVER_CONNECTION_FAILURE) {
			errorMessage = getString(R.string.connection_failed_message);
		} else {
			errorMessage = getString(R.string.internal_error_message);
		}
		postShowErrorDialog(errorMessage);
	}

	/**
	 * Stops the streaming components
	 */
	private void postStopStreaming() {
		cameraPreview.post(new Runnable() {
			@Override
			public void run() {
				streamToggleButton.setEnabled(false);
				shareButton.setEnabled(false);

				// disable error reporting because stopping the
				// camera capture causes the MP4 unpackaging thread
				// to crash (caused by the android camera seeking back in its
				// file descriptor and writing the MP4 container's MOOV-atom)
				if (streamer != null) {
					streamer.setErrorReporting(false);
					streamer.setPackageForwarding(false);
					Log.d(TAG,
							"deactivated error reporting and package forwarding");
				}

				// stops recording video
				if (mediaSource.getState() == State.CAPTURING) {
					mediaSource.stopCapture();
					Log.d(TAG, "stopped capture");
				}

				// stops the streaming threads and
				// releases any resources still in use by the streamer (sockets,
				// loopback, etc)
				if (streamer != null) {
					streamer.stopStreaming();
					Log.d(TAG, "stopped streaming threads");
					streamer.releaseResources();
					Log.d(TAG, "released resources");
					streamer = null;
				}

				statusOverlay.setMessage(null);
				statusOverlay.postInvalidate();
				streamToggleButton.setChecked(false);
				streamToggleButton.setEnabled(true);
				shareButton.setEnabled(true);
			}
		});
	}

	/**
	 * Gets the current MediaSource
	 * 
	 * @return the current MediaSource
	 */
	public MediaSource getMediaSource() {
		return mediaSource;
	}

	/**
	 * Shows info dialog about settings being detected
	 */
	public void postShowSettingsDetectionDialog() {
		cameraPreview.post(new Runnable() {
			@Override
			public void run() {
				FragmentTransaction ft = (FragmentTransaction) getSupportFragmentManager()
						.beginTransaction();
				CameraSettingsDetectionDialogFragment dialog = new CameraSettingsDetectionDialogFragment();
				dialog.show(ft, "settingsDetectionDialog");
			}
		});
	}

	/**
	 * Shows error dialog with given error message
	 * 
	 * @param errorMessage
	 *            message to display
	 */
	public void postShowErrorDialog(final String errorMessage) {
		cameraPreview.post(new Runnable() {
			@Override
			public void run() {
				FragmentTransaction ft = getSupportFragmentManager()
						.beginTransaction();
				ErrorDialogFragment dialog = new ErrorDialogFragment(
						errorMessage);
				dialog.show(ft, "errorDialog");
			}
		});
	}

	/**
	 * Shows social sharing dialog
	 */
	public void postShareDialog() {
		cameraPreview.post(new Runnable() {
			@Override
			public void run() {
				FragmentTransaction ft = getSupportFragmentManager()
						.beginTransaction();
				ShareDialogFragment dialog = new ShareDialogFragment();
				dialog.show(ft, "shareDialog");
			}
		});
	}

	/**
	 * Updates the status text inside the video
	 * 
	 * @param framesPerSecond
	 *            framerate to display
	 * @param bytesPerSecond
	 *            byterate to display
	 */
	public void postUpdateStatusOverlay(final double framesPerSecond,
			final double bytesPerSecond) {
		String message = String.format("%.1f fps\n%.1f kb/s", framesPerSecond,
				bytesPerSecond / 1024);
		statusOverlay.setMessage(message);
		statusOverlay.postInvalidate();
	}

	// ///////////
	// / SHARE ///
	// ///////////

	/**
	 * Called to select a certain share provider
	 * 
	 * @param id
	 *            the share provider to run
	 */
	public void runShareProvider(int id) {
		switch (id) {
		case 0: // Facebook
			shareFacebook();
			break;
		case 1: // Twitter
			shareTwitter();
			break;
		case 2: // QR Code
			shareQRCode();
			break;
		case 3: // Another App
			shareIntent();
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (mShareFacebookInstance != null)
			mShareFacebookInstance.authorizeCallback(requestCode, resultCode,
					data);
	}

	// / FACEBOOK ///

	/**
	 * instance of the Facebook SDK
	 */
	private Facebook mShareFacebookInstance;

	/**
	 * Facebook callback handler
	 */
	protected AsyncFacebookRunner mShareFacebookAsyncFacebookRunner;

	/**
	 * Facebook authentication token
	 */
	private String mShareFacebookAccessToken;

	/**
	 * token expiration time
	 */
	private long mShareFacebookAccessTokenExpires = 0;

	/**
	 * Called to start the Facebook share provider
	 */
	public void shareFacebook() {
		if (mShareFacebookInstance == null) {
			mShareFacebookInstance = new Facebook(Constants.SHARE_FACEBOOK_KEY);
			mShareFacebookAsyncFacebookRunner = new AsyncFacebookRunner(
					mShareFacebookInstance);
		}
		mShareFacebookAccessToken = mPrefs.getString(
				Constants.PREF_SHARE_FACEBOOK_ACCESS_TOKEN, null);
		mShareFacebookAccessTokenExpires = mPrefs.getLong(
				Constants.PREF_SHARE_FACEBOOK_ACCESS_TOKEN_EXPIRES, 0);
		if (mShareFacebookAccessToken != null) {
			mShareFacebookInstance.setAccessToken(mShareFacebookAccessToken);
		}
		if (mShareFacebookAccessTokenExpires != 0) {
			mShareFacebookInstance
					.setAccessExpires(mShareFacebookAccessTokenExpires);
		}

		if (!mShareFacebookInstance.isSessionValid()) {
			mShareFacebookInstance.authorize(this, new DialogListener() {
				@Override
				public void onComplete(Bundle values) {
					Editor editor = mPrefs.edit();
					editor.putString(
							Constants.PREF_SHARE_FACEBOOK_ACCESS_TOKEN,
							mShareFacebookInstance.getAccessToken());
					editor.putLong(
							Constants.PREF_SHARE_FACEBOOK_ACCESS_TOKEN_EXPIRES,
							mShareFacebookInstance.getAccessExpires());
					editor.commit();
					shareFacebookPostToWall();
				}

				@Override
				public void onFacebookError(FacebookError error) {
				}

				@Override
				public void onError(DialogError e) {
				}

				@Override
				public void onCancel() {
				}
			});
		} else
			shareFacebookPostToWall();
	}

	/**
	 * Called by callback method to do the actual posting to the user's Facebook
	 * Wall
	 */
	private void shareFacebookPostToWall() {
		Bundle params = new Bundle();
		params.putString("caption", getString(R.string.app_name));
		params.putString("name",
				getString(R.string.share_facebook_feed_caption));
		String brokerAddress = mPrefs.getString(Constants.PREF_SERVER_ADDRESS,
				"none.invalid");
		int brokerPort = Integer.parseInt(mPrefs.getString(
				Constants.PREF_SERVER_PORT, "5672"));
		params.putString("link", Helper.encodeHTTPWatchURL(brokerAddress,
				brokerPort, BCConstants.STREAM_SERVER_RCV_TOKEN));
		params.putString("description",
				getString(R.string.share_facebook_feed_msg));
		mShareFacebookInstance.dialog(this, "feed", params,
				new DialogListener() {

					@Override
					public void onComplete(Bundle values) {
					}

					@Override
					public void onFacebookError(FacebookError e) {
					}

					@Override
					public void onError(DialogError e) {
					}

					@Override
					public void onCancel() {
					}
				});
	}

	// / Twitter ///

	/**
	 * instance of the TwitterApp helper
	 */
	private TwitterApp mTwitterInstance;

	/**
	 * Called to start the Twitter share provider
	 */
	public void shareTwitter() {
		if (mTwitterInstance == null) {
			mTwitterInstance = new TwitterApp(this,
					Constants.SHARE_TWITTER_KEY, Constants.SHARE_TWITTER_SECRET);
			mTwitterInstance.setListener(new TwitterAuthListener() {

				@Override
				public void onComplete(String value) {
					shareTwitterTweet();
				}

				@Override
				public void onError(String value) {
					mTwitterInstance.resetAccessToken();
				}
			});
		} else {
			if (mTwitterInstance.hasAccessToken()) {
				shareTwitterTweet();
			} else {
				mTwitterInstance.authorize();
			}
		}
	}

	/**
	 * Called by callback method to do the actual sending of the Twitter message
	 */
	public void shareTwitterTweet() {
		String brokerAddress = mPrefs.getString(Constants.PREF_SERVER_ADDRESS,
				"none.invalid");
		int brokerPort = Integer.parseInt(mPrefs.getString(
				Constants.PREF_SERVER_PORT, "5672"));
		String message = getString(R.string.share_twitter_tweet_msg)
				+ Helper.encodeHTTPWatchURL(brokerAddress, brokerPort,
						BCConstants.STREAM_SERVER_RCV_TOKEN);
		try {
			mTwitterInstance.updateStatus(message);
		} catch (Exception e) {
			// currently ignored
		}
	}

	// / QR Code ///

	/**
	 * Called to start the QR-Code share provider and showing the QR-Code for
	 * the current video stream
	 */
	private void shareQRCode() {
		try {
			Display display = getWindowManager().getDefaultDisplay();
			int width = display.getWidth();
			int height = display.getHeight();
			int maxDimension = width > height ? height * 3 / 4 : width * 3 / 4;
			String brokerAddress = mPrefs.getString(
					Constants.PREF_SERVER_ADDRESS, "none.invalid");
			int brokerPort = Integer.parseInt(mPrefs.getString(
					Constants.PREF_SERVER_PORT, "5672"));
			final QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(
					Helper.encodeCustomWatchURL(brokerAddress, brokerPort,
							BCConstants.STREAM_SERVER_RCV_TOKEN), null,
					Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(),
					maxDimension);

			cameraPreview.post(new Runnable() {
				@Override
				public void run() {
					FragmentTransaction ft = getSupportFragmentManager()
							.beginTransaction();
					ShowQRCodeDialogFragment dialog;
					try {
						dialog = new ShowQRCodeDialogFragment(qrCodeEncoder
								.encodeAsBitmap());
						dialog.show(ft, "showQRCodeDialog");
					} catch (WriterException e) {
						Log.e(TAG, "Could not encode barcode", e);
					}
				}
			});
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Could not encode barcode", e);
		}
	}

	/**
	 * Gets a given preference from application preferences
	 * 
	 * @param key
	 *            key to get preference for
	 * @return the corresponding preference
	 */
	private String getStringPref(String key) {
		try {
			return mPrefs.getString(key, "");
		} catch (ClassCastException e) {
			return "";
		}
	}

	// / Other apps ///

	/**
	 * Called to fire an general ACTION_SEND Intent containing information about
	 * the current video stream
	 */
	private void shareIntent() {
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		String brokerAddress = mPrefs.getString(Constants.PREF_SERVER_ADDRESS,
				"none.invalid");
		int brokerPort = Integer.parseInt(mPrefs.getString(
				Constants.PREF_SERVER_PORT, "5672"));
		shareIntent.putExtra(
				android.content.Intent.EXTRA_TEXT,
				getString(R.string.share_intent_msgprefix)
						+ Helper.encodeHTTPWatchURL(brokerAddress, brokerPort,
								BCConstants.STREAM_SERVER_RCV_TOKEN));
		startActivity(Intent.createChooser(shareIntent,
				getString(R.string.share_intent_title)));
	}
}