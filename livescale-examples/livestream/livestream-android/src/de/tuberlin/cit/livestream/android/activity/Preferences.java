package de.tuberlin.cit.livestream.android.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.broadcast.mp4.AvcConfigAutodetector;
import de.tuberlin.cit.livestream.android.messaging.Messaging;

/**
 * Activity for displaying and managing user preferences like username, password
 * and connection settings.
 * 
 * @author Stefan Werner
 */
public class Preferences extends PreferenceActivity {

	/**
	 * log tag
	 */
	private final static String TAG = Preferences.class.getSimpleName();

	// Vars

	/**
	 * application preferences
	 */
	private SharedPreferences mPrefs = null;

	// Layout Elements

	/**
	 * Preference item to show server status
	 */
	private Preference mStatusPref = null;

	/**
	 * Preference item to show user registration status
	 */
	private Preference mRegPref = null;

	/**
	 * ListPreference item to select camera device
	 */
	private ListPreference mCameraPref = null;

	/**
	 * ListPreference item to select video quality
	 */
	private ListPreference mQualityPref = null;

	// / METHODES FROM SUPERCLASS ///

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		connectToPreferences();
		connectToLayout();

		Messaging.getInstance().attach(this);
		checkForFirstRun();
		registerUser();
		showServerStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Messaging.getInstance().detach(this);
	}

	// / OWN METHODES ///

	/**
	 * Loads the Layout and connect layout elements to objects
	 */
	private void connectToLayout() {
		addPreferencesFromResource(R.xml.preferences);
		mRegPref = (Preference) findPreference("register");
		mStatusPref = (Preference) findPreference("status");
		mRegPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.preference.Preference.OnPreferenceClickListener#
			 * onPreferenceClick(android.preference.Preference)
			 */
			@Override
			public boolean onPreferenceClick(Preference preference) {
				registerUser();
				return true;
			}
		});
		mQualityPref = (ListPreference) findPreference("quality");
		fillVideoQualitiesPref();
		mCameraPref = (ListPreference) findPreference("camera");
		fillCamerasPref();
	}

	/**
	 * Encapsulating Class to store CamcorderProfile information
	 */
	private class PrefCamcorderProfile implements
			Comparable<PrefCamcorderProfile> {

		/**
		 * base CamcorderProfile
		 */
		public CamcorderProfile mProfile;

		/**
		 * description of profile properies
		 */
		public String mDescription;

		/**
		 * quality ID of the profile
		 */
		public String mId;

		/**
		 * Constructor
		 * 
		 * @param profile
		 *            base CamcorderProfile
		 */
		public PrefCamcorderProfile(CamcorderProfile profile) {
			this.mProfile = profile;
			this.mId = String.valueOf(profile.quality);
			this.mDescription = getProfileDescription(profile);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(PrefCamcorderProfile another) {
			if (mProfile.videoFrameWidth != another.mProfile.videoFrameWidth)
				return Integer.valueOf(mProfile.videoFrameWidth).compareTo(
						Integer.valueOf(another.mProfile.videoFrameWidth));

			else if (mProfile.videoFrameHeight != another.mProfile.videoFrameHeight)
				return Integer.valueOf(mProfile.videoFrameHeight).compareTo(
						Integer.valueOf(another.mProfile.videoFrameHeight));

			else if (mProfile.videoFrameRate != another.mProfile.videoFrameRate)
				return Integer.valueOf(mProfile.videoFrameRate).compareTo(
						Integer.valueOf(another.mProfile.videoFrameRate));

			else if (mProfile.videoBitRate != another.mProfile.videoBitRate)
				return Integer.valueOf(mProfile.videoBitRate).compareTo(
						Integer.valueOf(another.mProfile.videoBitRate));

			else
				return 0;
		}
	}

	/**
	 * Gets a String describing properties of an CamcorderProfiles
	 * 
	 * @param profile
	 *            the profile to describe
	 * @return profile description
	 */
	protected String getProfileDescription(CamcorderProfile profile) {
		return profile.videoFrameWidth + "x" + profile.videoFrameHeight + ", "
				+ profile.videoFrameRate + "fps, "
				+ (profile.videoBitRate / 8192) + "kB/s";
	}

	/**
	 * Fills the list of available video qualities
	 */
	private void fillVideoQualitiesPref() {
		TreeSet<PrefCamcorderProfile> profiles = new TreeSet<PrefCamcorderProfile>();
		int currentCameraDevice = Integer.parseInt(mPrefs.getString(
				Constants.PREF_CAMERA_DEVICE, "0"));
		if (android.os.Build.VERSION.SDK_INT > 10) {
			if (CamcorderProfile.hasProfile(currentCameraDevice,
					CamcorderProfile.QUALITY_480P)) {
				Log.i(TAG, "Adding camcorder profile QUALITY_480P");
				profiles.add(new PrefCamcorderProfile(CamcorderProfile.get(
						currentCameraDevice, CamcorderProfile.QUALITY_480P)));
			} else if (CamcorderProfile.hasProfile(currentCameraDevice,
					CamcorderProfile.QUALITY_CIF)) {
				Log.i(TAG, "Adding camcorder profile QUALITY_CIF");
				profiles.add(new PrefCamcorderProfile(CamcorderProfile.get(
						currentCameraDevice, CamcorderProfile.QUALITY_CIF)));
			} else if (CamcorderProfile.hasProfile(currentCameraDevice,
					CamcorderProfile.QUALITY_QVGA)) {
				Log.i(TAG, "Adding camcorder profile QUALITY_QVGA");
				profiles.add(new PrefCamcorderProfile(CamcorderProfile.get(
						currentCameraDevice, CamcorderProfile.QUALITY_QVGA)));
			} else if (CamcorderProfile.hasProfile(currentCameraDevice,
					CamcorderProfile.QUALITY_QCIF)) {
				Log.i(TAG, "Adding camcorder profile QUALITY_QCIF");
				profiles.add(new PrefCamcorderProfile(CamcorderProfile.get(
						currentCameraDevice, CamcorderProfile.QUALITY_QCIF)));
			}
		}
		// default / SDK < 11 profiles
		// TODO: Trial and Exception? There must be a better way!
		try {
			profiles.add(new PrefCamcorderProfile(CamcorderProfile.get(
					currentCameraDevice, CamcorderProfile.QUALITY_HIGH)));
			Log.i(TAG, "Adding camcorder profile QUALITY_HIGH");
		} catch (Exception e) {
		}
		try {
			profiles.add(new PrefCamcorderProfile(CamcorderProfile.get(
					currentCameraDevice, CamcorderProfile.QUALITY_LOW)));
			Log.i(TAG, "Adding camcorder profile QUALITY_LOW");
		} catch (Exception e) {
		}

		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> ids = new ArrayList<String>();
		for (PrefCamcorderProfile profile : profiles) {
			names.add(profile.mDescription);
			ids.add(profile.mId);
		}
		mQualityPref.setEntries(names.toArray(new String[0]));
		mQualityPref.setEntryValues(ids.toArray(new String[0]));
	}

	/**
	 * Resets the video qualities list and opens the corresponding dialog to
	 * select one
	 */
	private void resetVideoQualities() {
		fillVideoQualitiesPref();
		// reset video qualities
		if (mQualityPref.getEntryValues().length > 0)
			mQualityPref.setValueIndex(0);
		// TODO: opening the qualities dialog would be better but can lead to FC
		// if done to fast
		// getPreferenceScreen().onItemClick(null, null,
		// mQualityPref.getOrder()-1, 0);
	}

	/**
	 * Fills the list of available camera devices
	 */
	private void fillCamerasPref() {
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> ids = new ArrayList<String>();
		for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
			names.add(getString(R.string.prefs_camera) + (i + 1));
			ids.add(String.valueOf(i));
		}
		mCameraPref.setEntries(names.toArray(new String[0]));
		mCameraPref.setEntryValues(ids.toArray(new String[0]));
	}

	/**
	 * Connects to application preferences
	 */
	private void connectToPreferences() {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mPrefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
	}

	/**
	 * Checks if this the first run of this application if that the case a
	 * welcome message dialog is shown
	 */
	private void checkForFirstRun() {
		if (mPrefs.getBoolean(Constants.PREF_FIRST_RUN, true)) {
			showDialog(getString(R.string.msg_first_run));
			mPrefs.edit().putBoolean(Constants.PREF_FIRST_RUN, false).commit();
		}
	}

	/**
	 * Called to register / verify a user
	 */
	private void registerUser() {
		String username = getStringPref(Constants.PREF_USERNAME);
		String password = getStringPref(Constants.PREF_PASSWORD);
		if (!username.isEmpty() && !password.isEmpty()) {
			if (Messaging.getInstance().isReady()) {
				Messaging.getInstance().sendDispatcherRegistration(username, password);
			}
		} else
			mRegPref.setSummary(getString(R.string.prefs_register_summary_empty_username_password));
	}

	/**
	 * Updates the Layout Element that displays the current server status
	 */
	private void showServerStatus() {
		if (mPrefs.getBoolean(Constants.PREF_CONNECTION_STATUS, false)) {
			mStatusPref
					.setSummary(getString(R.string.prefs_register_summary_connected));
			mRegPref.setSummary(getString(R.string.prefs_register_summary_no_answer));
		} else {
			mStatusPref
					.setSummary(getString(R.string.prefs_status_summary_not_connected));
			mRegPref.setSummary(getString(R.string.prefs_register_summary_not_connected));
		}
	}

	/**
	 * Updates the Layout Element that displays the current user registration
	 * status
	 */
	private void showRegistrationStatus() {
		String msg = getStringPref(Constants.PREF_REGISTRATION_MSG);
		/*
		 * boolean status = prefs.getBoolean("reg_ok", true); final String
		 * defaultMsg = "Tap to register/check username on Server"; if
		 * (msg.isEmpty()) regPref.setSummary(defaultMsg); else {
		 */
		mRegPref.setSummary(getString(R.string.prefs_register_summary_answer_prefix)
				+ msg);
	}

	/**
	 * Listener for changed preference values
	 */
	private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {
			if (key.equals(Constants.PREF_CONNECTION_STATUS)) {
				showServerStatus();
			} else if (key.equals(Constants.PREF_REGISTRATION_OK)
					|| key.equals(Constants.PREF_REGISTRATION_MSG)
					|| key.equals(Constants.PREF_UPDATE_TIME)) {
				showRegistrationStatus();
			} else if (key.equals(Constants.PREF_CAMERA_DEVICE)) {
				resetVideoQualities();
			} else if (key.equals(Constants.PREF_VIDEO_QUALITY)) {
				removeCameraDetectionFile();
			}
		}
	};

	// / HELPERS ///

	/**
	 * Removes the camera format detection file (e.g. /sdcard/detect.mp4) see
	 * also
	 * {@link de.tuberlin.cit.livestream.android.broadcast.mp4.AvcConfigAutodetector}
	 */
	private void removeCameraDetectionFile() {
		File detectFile = new File(AvcConfigAutodetector.AUTODETECTION_FILE);
		if (detectFile.exists()) {
			try {
				detectFile.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	/**
	 * Shows a Dialog containing given text
	 * 
	 * @param text
	 *            the text to display
	 */
	public void showDialog(String text) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(text)
				.setCancelable(false)
				.setPositiveButton(getString(R.string.button_close),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
