/*
 * Copyright 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tuberlin.cit.livestream.android.view.actionbar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.R;

/**
 * An extension of {@link ActionBarHelper} that provides Android 3.0-specific
 * functionality for Honeycomb tablets. It thus requires API level 11.
 */
public class ActionBarHelperHoneycomb extends ActionBarHelper {
	private Menu mOptionsMenu;
	private View mRefreshIndeterminateProgressView = null;

	protected ActionBarHelperHoneycomb(Activity activity) {
		super(activity);
	}

	private SharedPreferences prefs = null;

	private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {
			if (key.equals(Constants.PREF_CONNECTION_STATUS)) {
				setConnectionStateIcon(getConnectionState());
			}
		}
	};

	private void setConnectionStateIcon(boolean state) {
		if (getConnectionState())
			setRefreshActionItemState(false);
		else
			setRefreshActionItemState(true);
	}

	private boolean getConnectionState() {
		return prefs.getBoolean(Constants.PREF_CONNECTION_STATUS, false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (prefs == null) {
			prefs = PreferenceManager
					.getDefaultSharedPreferences(getActionBarThemedContext());
			prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
		}
		mOptionsMenu = menu;
		boolean returnValue = super.onCreateOptionsMenu(menu);
		setRefreshActionItemState(!getConnectionState());
		return returnValue;
	}

	@Override
	public void setRefreshActionItemState(boolean refreshing) {
		// On Honeycomb, we can set the state of the refresh button by giving it
		// a custom
		// action view.
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				if (mRefreshIndeterminateProgressView == null) {
					LayoutInflater inflater = (LayoutInflater) getActionBarThemedContext()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					mRefreshIndeterminateProgressView = inflater.inflate(
							R.layout.actionbar_indeterminate_progress, null);
				}

				refreshItem.setActionView(mRefreshIndeterminateProgressView);
			} else {
				refreshItem.setActionView(null);
			}
		}
	}

	/**
	 * Returns a {@link Context} suitable for inflating layouts for the action
	 * bar. The implementation for this method in {@link ActionBarHelperICS}
	 * asks the action bar for a themed context.
	 */
	protected Context getActionBarThemedContext() {
		return mActivity;
	}
}
