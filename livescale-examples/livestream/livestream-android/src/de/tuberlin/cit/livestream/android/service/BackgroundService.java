/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tuberlin.cit.livestream.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.activity.Main;
import de.tuberlin.cit.livestream.android.messaging.Messaging;

/**
 * Background service Class to handle C2DM communication
 * 
 * @author Stefan Werner
 */
public class BackgroundService extends Service {

	// Vars

	/**
	 * application preferences
	 */
	private SharedPreferences mPrefs = null;

	/**
	 * current C2DM Registration ID
	 */
	private String mC2DMKey = null;

	// / METHODES FROM SUPERCLASS ///

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		connectToPreferences();
		setServiceRunning(true);
		connectToC2DM();

		return START_STICKY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		mC2DMKey = "";
		sendC2DMKey();
		unregisterReceiver(c2DMRegistrationCallback);
		unregisterReceiver(c2DMMessageCallback);
		setServiceRunning(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// / OWN METHODES ///

	/**
	 * Connects to application preferences
	 */
	private void connectToPreferences() {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mPrefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
	}

	/**
	 * Registers receivers to get answers from C2DM and sends registration
	 * request
	 */
	private void connectToC2DM() {
		IntentFilter intentFilterReg = new IntentFilter(
				Constants.C2DM_INTENTFILTER_REG);
		intentFilterReg.addCategory(Constants.C2DM_INTENTFILTER_CATEGORY);
		IntentFilter intentFilterRcv = new IntentFilter(
				Constants.C2DM_INTENTFILTER_RCV);
		intentFilterRcv.addCategory(Constants.C2DM_INTENTFILTER_CATEGORY);
		registerReceiver(c2DMRegistrationCallback, intentFilterReg);
		registerReceiver(c2DMMessageCallback, intentFilterRcv);

		Intent c2DMIntent = new Intent(Constants.C2DM_INTENT_REG);
		c2DMIntent.putExtra("app",
				PendingIntent.getBroadcast(this, 0, new Intent(), 0));
		c2DMIntent.putExtra("sender", Constants.GCM_PROJECT_NUMBER);
		startService(c2DMIntent);
	}

	/**
	 * Calls the communication component to send current C2DM registration ID to
	 * a CITstreamer Dispatcher
	 */
	private void sendC2DMKey() {
		String username = getStringPref(Constants.PREF_USERNAME);
		String password = getStringPref(Constants.PREF_PASSWORD);
		if (mC2DMKey != null && !username.isEmpty() && !password.isEmpty()) {
			Messaging messaging = Messaging.getInstance();
			messaging.attach(this);
			if (messaging.isReady()) {
				messaging.sendDispatcherC2dm(username, password, mC2DMKey);
				messaging.detach(this);
			}
		}
	}

	/**
	 * Shows a Notification about a new video stream from a favorite user
	 * 
	 * @param text
	 *            text to be displayed with the notification
	 * @param url
	 *            URL of the video stream
	 */
	private void showNotification(String text, String url) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.icon;
		CharSequence tickerText = getString(R.string.notification_new_stream_ticker);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		Context context = getApplicationContext();
		CharSequence contentTitle = getString(R.string.notification_title);
		Intent notificationIntent = new Intent(this, Main.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		notificationIntent.setData(Uri.parse(url));
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, text,
				contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotificationManager
				.notify((new Long(System.currentTimeMillis())).intValue(),
						notification);
	}

	/**
	 * Stores current service status within the apllication preferences
	 * 
	 * @param state
	 *            current service status
	 */
	private void setServiceRunning(boolean state) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean("service_running", state);
		editor.commit();
	}

	/**
	 * Listener for changed preference values
	 */
	private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {
			if (key.equals(Constants.PREF_CONNECTION_STATUS)) {
				if (sharedPrefs.getBoolean(Constants.PREF_CONNECTION_STATUS,
						false)) {
					sendC2DMKey();
				}
			} else if (key.equals(Constants.PREF_USERNAME)
					|| key.equals(Constants.PREF_PASSWORD)
					|| key.equals(Constants.PREF_REGISTRATION_OK))
				sendC2DMKey();
			else if (key.equals(Constants.PREF_BACKGROUND_SERVICE_LISTEN))
				if (!mPrefs.getBoolean(
						Constants.PREF_BACKGROUND_SERVICE_LISTEN, true))
					stopSelf();
		}
	};

	/**
	 * receives registration acknowledgments from C2DM
	 */
	private BroadcastReceiver c2DMRegistrationCallback = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Constants.C2DM_INTENTFILTER_REG)) {
				String tmpKey = intent.getStringExtra("registration_id");
				if (tmpKey != null) {
					mC2DMKey = tmpKey;
					sendC2DMKey();
				}
			}
		}
	};

	/**
	 * receives messages from C2DM
	 */
	private BroadcastReceiver c2DMMessageCallback = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				String action = intent.getAction();
				if (Constants.C2DM_INTENTFILTER_RCV.equals(action)) {
					String type = intent
							.getStringExtra(Constants.C2DM_KEY_MSG_TYPE);
					String username = intent
							.getStringExtra(Constants.C2DM_KEY_USERNAME);
					String data = intent
							.getStringExtra(Constants.C2DM_KEY_DATA);
					if (type.equals(Constants.C2DM_MSG_TYPE_FAV))
						showNotification(
								context.getString(R.string.notification_new_stream_text_prefix)
										+ username, Constants.URL_CUSTOM_SCHEME
										+ "://-/-/watch/" + data);
				}
			} catch (Exception e) {
				if (Constants.DEBUG) {
					System.out.println("Error on C2DM msg receive:");
					e.printStackTrace();
				}
			}
		}
	};

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