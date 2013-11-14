package de.tuberlin.cit.livestream.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import de.tuberlin.cit.livestream.android.Constants;

/**
 * Class to receive RECEIVE_BOOT_COMPLETED event when Android device is started
 * and starts BackgroundService if it is enabled
 * 
 * @author Stefan Werner
 */
public class OnBootReceiver extends BroadcastReceiver {
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	public void onReceive(Context arg0, Intent arg1) {
		if (PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean(
				Constants.PREF_BACKGROUND_SERVICE_LISTEN, true)) {
			Intent intent = new Intent(arg0, BackgroundService.class);
			arg0.startService(intent);
		}
	}
}