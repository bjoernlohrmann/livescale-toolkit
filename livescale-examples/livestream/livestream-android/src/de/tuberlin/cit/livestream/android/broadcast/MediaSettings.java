package de.tuberlin.cit.livestream.android.broadcast;

import android.content.SharedPreferences;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.coremedia.iso.boxes.h264.AvcConfigurationBox;

import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.Helper;

public class MediaSettings {

	private final static String TAG = MediaSettings.class.getSimpleName();

	private int cameraDevice;

	private int videoWidth;

	private int videoHeight;

	private int videoFrameRate;

	private int videoBitRate;

	private int outputContainerFormat;

	private int videoEncoder;

	private AvcConfigurationBox avcConfig;

	private static MediaSettings singletonInstance;

	private SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(Helper.getApplicationContext());

	private MediaSettings() {
		// These settings work for the Google Nexus S
		// If they are not correct, the application will keep crashing.

		/*
		 * CamcorderProfile profile;
		 * 
		 * if (android.os.Build.VERSION.SDK_INT > 10) { if
		 * (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
		 * Log.i(TAG, "Using camcorder profile QUALITY_480P"); profile =
		 * CamcorderProfile.get(CamcorderProfile.QUALITY_480P); } else if
		 * (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF)) {
		 * Log.i(TAG, "Using camcorder profile QUALITY_CIF"); profile =
		 * CamcorderProfile.get(CamcorderProfile.QUALITY_CIF); } else if
		 * (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA)) {
		 * Log.i(TAG, "Using camcorder profile QUALITY_QVGA"); profile =
		 * CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA); } else if
		 * (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QCIF)) {
		 * Log.i(TAG, "Using camcorder profile QUALITY_QCIF"); profile =
		 * CamcorderProfile.get(CamcorderProfile.QUALITY_QCIF); } else {
		 * Log.i(TAG, "Using camcorder profile QUALITY_HIGH"); profile =
		 * CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH); }
		 * initializeFromProfile(profile); } else {
		 * initializeFromProfile(CamcorderProfile.get(0,
		 * CamcorderProfile.QUALITY_HIGH)); }
		 */

		initialize();
		prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
	}

	public void initialize() {
		int camcorderProfile = Integer.parseInt(prefs.getString(
				Constants.PREF_VIDEO_QUALITY, "0"));
		int camera = Integer.parseInt(prefs.getString(
				Constants.PREF_CAMERA_DEVICE, "0"));
		cameraDevice = camera;
		Log.i(TAG, "Using camcorder profile " + camcorderProfile
				+ " on camera " + camera);
		CamcorderProfile profile = CamcorderProfile.get(camera,
				camcorderProfile);
		this.videoWidth = profile.videoFrameWidth;
		this.videoHeight = profile.videoFrameHeight;
		this.videoFrameRate = profile.videoFrameRate;
		this.videoBitRate = profile.videoBitRate;
		// this.videoBitRate = 409600; // 50 KB/s
		this.outputContainerFormat = MediaRecorder.OutputFormat.MPEG_4;
		this.videoEncoder = MediaRecorder.VideoEncoder.H264;
	}

	// TODO: Holding a Listener here inside of a static object seems to be not
	// the best solution for the preferences update problem
	private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {
			if (key.equals(Constants.PREF_CAMERA_DEVICE)
					|| key.equals(Constants.PREF_VIDEO_QUALITY)) {
				initialize();
			}
		}
	};

	public static MediaSettings getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new MediaSettings();
		}
		return singletonInstance;
	}

	public static void initAvcConfig(AvcConfigurationBox avcConfig) {
		getInstance().avcConfig = avcConfig;
	}

	public int getCameraDevice() {
		return cameraDevice;
	}

	public int getVideoWidth() {
		return videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	public int getVideoFrameRate() {
		return videoFrameRate;
	}

	public int getVideoBitRate() {
		return videoBitRate;
	}

	public AvcConfigurationBox getAvcConfig() {
		return avcConfig;
	}

	public int getOutputContainerFormat() {
		return outputContainerFormat;
	}

	public int getVideoEncoder() {
		return videoEncoder;
	}
}
