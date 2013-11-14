package de.tuberlin.cit.livestream.android.activity;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;
import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.fragment.VideoplayerBufferingDialogFragment;
import de.tuberlin.cit.livestream.android.messaging.Messaging;
import de.tuberlin.cit.livestream.android.view.SurfaceHolderCallbackAdapter;

/**
 * Activity for receiving video streams
 * 
 * @author Björn Lohrmann, Stefan Werner
 */
public class Videoplayer extends FragmentActivity {

	// Constants

	/**
	 * log tag
	 */
	private final static String TAG = Videoplayer.class.getSimpleName();

	// Vars

	/**
	 * favorite status of the currently played video stream
	 */
	private int mCurrentStreamFavorite = 0;

	/**
	 * application preferences
	 */
	private SharedPreferences mPrefs = null;

	/**
	 * reference to Android MediaPlayer Class for playing video data
	 */
	private MediaPlayer mediaPlayer;

	/**
	 * View for displaying video data
	 */
	private SurfaceView videoSurface;

	/**
	 * Dialog shown while the media player is buffering
	 */
	private VideoplayerBufferingDialogFragment bufferingDialog;

	/**
	 * URL of the currently played video stream
	 */
	private String streamUrl = "";

	/**
	 * username of the sender of the currently played video stream
	 */
	private String mSenderUsername = "";

	// Layout Elements

	/**
	 * Button to change favorite status
	 */
	private ToggleButton mFavoriteToggleButton;

	// / METHODES FROM SUPERCLASS ///

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		try {
			streamUrl = getIntent().getExtras().getString("MAP_KEY_URL");
			if (streamUrl == null || streamUrl.isEmpty())
				throw new Exception("Error: Cannot play without Stream URL!");
		} catch (Exception e) {
			finish();
		}

		connectToLayout();
		connectToPreferences();

		mSenderUsername = getIntent().getExtras().getString(
				// ClientCommunicationManager.MAP_KEY_USER_NAME_SENDER); TODO removeMe / constant?
				"user_name_sender");
				
		mPrefs.edit().putInt("current_stream_favorite", 0).commit();
		updateFavoriteToggleBotton();
		checkAndSwitchFavoriteStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();
		postStopStreaming();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.e(TAG, String.format("Error: %d / %d", what, extra));
				return false;
			}
		});
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				if (bufferingDialog != null) {
					bufferingDialog.dismiss();
					bufferingDialog = null;
				}
				mediaPlayer.start();
			}
		});
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				finish();
			}
		});

		videoSurface.getHolder().addCallback(
				new SurfaceHolderCallbackAdapter() {
					@Override
					public void surfaceCreated(SurfaceHolder holder) {
						mediaPlayer.setDisplay(videoSurface.getHolder());
						mediaPlayer.setScreenOnWhilePlaying(true);
					}
				});

		try {
			// this.mediaPlayer.setDataSource(streamer.getLoopbackSourceFD());
			mediaPlayer.setDataSource(streamUrl);
			// this.mediaPlayer.setDataSource("/sdcard/livestream.ts");
			mediaPlayer.prepareAsync();
			postShowBufferingDialog();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	// / OWN METHODES ///

	/**
	 * Loads the Layout and connect layout elements to objects
	 */
	private void connectToLayout() {
		setContentView(R.layout.videoplayer_landscape);
		videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
		// videoSurface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mFavoriteToggleButton = (ToggleButton) findViewById(R.id.videoplayerFavoriteToggleButton);
		mFavoriteToggleButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				checkAndSwitchFavoriteStatus();
			}
		});
	}

	/**
	 * Connects to application preferences
	 */
	private void connectToPreferences() {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mPrefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
	}

	/**
	 * checks the current favorite status and switches it if necessary
	 * CurrentStreamFavorite action 0 status unknown -> negotiation message 1
	 * favorite set -> unset -1 favorite unset -> set
	 */
	private void checkAndSwitchFavoriteStatus() {
		String username = getStringPref(Constants.PREF_USERNAME);
		String password = getStringPref(Constants.PREF_PASSWORD);
		int type = 0;
		if (mCurrentStreamFavorite == -1)
			type = 1;
		else if (mCurrentStreamFavorite == 1)
			type = -1;
		Messaging.getInstance().sendDispatcherRequestFollower(username,
				password, mSenderUsername, type);
	}

	/**
	 * Updates status of Button upon answer from communication component
	 */
	private void updateFavoriteToggleBotton() {
		if (mCurrentStreamFavorite == 0) {
			mFavoriteToggleButton.setChecked(false);
		} else if (mCurrentStreamFavorite == -1) {
			mFavoriteToggleButton.setChecked(false);
		} else if (mCurrentStreamFavorite == 1) {
			mFavoriteToggleButton.setChecked(true);
		}
	}

	/**
	 * Listener for changed preference values
	 */
	private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {
			if (key.equals(Constants.PREF_CUR_STREAM_FAVORITE)
					|| key.equals(Constants.PREF_UPDATE_TIME)) {
				mCurrentStreamFavorite = mPrefs.getInt(
						Constants.PREF_CUR_STREAM_FAVORITE, 0);
				updateFavoriteToggleBotton();
			}
		}
	};

	/**
	 * Displays a Dialog while buffering video data
	 */
	public void postShowBufferingDialog() {
		videoSurface.post(new Runnable() {
			@Override
			public void run() {
				FragmentTransaction ft = (FragmentTransaction) getSupportFragmentManager()
						.beginTransaction();
				bufferingDialog = new VideoplayerBufferingDialogFragment();
				bufferingDialog.show(ft, "bufferingDialog");
			}
		});
	}

	/**
	 * Stops playing a video stream
	 */
	private void postStopStreaming() {
		mediaPlayer.reset();
		mediaPlayer.release();
		mediaPlayer = null;
	}

	// / HELPERS ///

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
}