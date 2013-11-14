package de.tuberlin.cit.livestream.android.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.Helper;
import de.tuberlin.cit.livestream.android.R;
import de.tuberlin.cit.livestream.android.messaging.Messaging;
import de.tuberlin.cit.livestream.android.service.BackgroundService;
import de.tuberlin.cit.livestream.android.view.actionbar.ActionBarActivity;

/**
 * Main activity of the CITstreamer application. This activity is shown whenever
 * a user starts the app.
 * 
 * @author Stefan Werner
 */
public class Main extends ActionBarActivity {

	// Vars

	/**
	 * application preferences
	 */
	private SharedPreferences mPrefs = null;

	/**
	 * list of runnables started when the communication subsystem connects
	 */
	private ArrayList<Runnable> mConnectEventRunnables = new ArrayList<Runnable>();

	// Layout Elements

	/**
	 * button to start video broadcasting
	 */
	private Button mButtonBroadcast;

	/**
	 * button to start video receiving after manually entering a receive token
	 */
	private Button mButtonManualRcv;

	/**
	 * form to manually enter a receive token
	 */
	private EditText mEditTextRcvToken;

	/**
	 * ProgressDialog to show while waiting for answer from communication
	 * subsystem
	 */
	public static ProgressDialog mProgressDialog;

	// / METHODES FROM SUPERCLASS ///

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livestream.android.view.actionbar.ActionBarActivity#onCreate(android.os.Bundle
	 * )
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		connectToLayout();
		connectToPreferences();
		Helper.setApplicationContext(this.getApplicationContext()); // to have a
																	// reference
																	// to
																	// application
																	// Context

		Messaging.getInstance().attach(this);
		if (checkForReceiveIntent())
			return;
		else {
			checkForService();
			checkForFirstRun();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		checkForReceiveIntent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livestream.android.view.actionbar.ActionBarActivity#onCreateOptionsMenu(android
	 * .view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			break;
		case R.id.menu_refresh:
			String server = getStringPref(Constants.PREF_SERVER_ADDRESS) + ":"
					+ getStringPref(Constants.PREF_SERVER_PORT);
			Toast.makeText(this,
					getString(R.string.msg_broker_connection_info) + server,
					Toast.LENGTH_SHORT).show();
			break;
		case R.id.menu_prefs:
			Intent intent = new Intent(this, Preferences.class);
			startActivity(intent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		dismissProgress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Messaging.getInstance().detach(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		finish();
	}

	// / OWN METHODES ///

	/**
	 * Loads the Layout and connect layout elements to objects
	 */
	private void connectToLayout() {
		setContentView(R.layout.main);
		mButtonBroadcast = (Button) findViewById(R.id.main_button_broadcast);
		mButtonBroadcast.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!Messaging.getInstance().isReady())
					showToast(getString(R.string.main_error_not_connected));
				else {
					Intent broadcastIntent = new Intent(Main.this,
							Broadcast.class);
					broadcastIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(broadcastIntent);
				}
			}
		});
		mEditTextRcvToken = (EditText) findViewById(R.id.main_et_rcvtoken);
		mButtonManualRcv = (Button) findViewById(R.id.main_button_manualrcv);
		mButtonManualRcv.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String rcvToken = mEditTextRcvToken.getText().toString();
				if (!Messaging.getInstance().isReady())
					showToast(getString(R.string.main_error_not_connected));
				else if (rcvToken.isEmpty())
					showToast(getString(R.string.main_error_manualrcv_empty));
				else if (rcvToken.matches(".*"
						+ Constants.RCVTOKEN_ILLEGAL_CHARS_REGEXP + ".*"))
					showToast(getString(R.string.main_error_manualrcv_illegal));
				else {
					Messaging.getInstance().sendDispatcherRequestStreamRcv(rcvToken);
				}
			}
		});
	}

	/**
	 * Connects to application preferences
	 */
	private void connectToPreferences() {
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mPrefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
	}

	/**
	 * Checks if this the first run of this application if that the case the
	 * Preference Activity is called
	 */
	private void checkForFirstRun() {
		if (mPrefs.getBoolean(Constants.PREF_FIRST_RUN, true)) {
			Intent intent = new Intent(this, Preferences.class);
			startActivity(intent);
		}
	}

	/**
	 * Checks if the BackgroundService needs to be startet depending on the
	 * preferences
	 */
	private void checkForService() {
		if (mPrefs.getBoolean(Constants.PREF_BACKGROUND_SERVICE_LISTEN, true))
			startService(new Intent(this, BackgroundService.class));
	}

	/**
	 * Checks if the application was called by an Intent containing a
	 * citstreamer:// receive URL e.g. if the user hit a notification or used
	 * the CITstreamer Webserver If a receive URL is found, the application
	 * tries to play the corresponding videostream
	 * 
	 * @return true if there was a receive URL found, false otherwise
	 */
	@SuppressWarnings("unused")
	private boolean checkForReceiveIntent() {
		Intent intent = getIntent();
		if (intent == null)
			return false;
		Uri data = getIntent().getData();
		if (data == null)
			return false;
		String scheme = data.getScheme();
		String host = data.getHost(); // currently ignored
		List<String> path = data.getPathSegments();
		if (path.size() < 3)
			return false;
		String port = path.get(0); // currently ignored
		String watch = path.get(1);
		final String rcvToken = path.get(2);
		if ((scheme.equals("http") || scheme
				.equals(Constants.URL_CUSTOM_SCHEME))
				&& watch.equals("watch")
				&& !rcvToken.isEmpty()
				&& !rcvToken.matches(".*"
						+ Constants.RCVTOKEN_ILLEGAL_CHARS_REGEXP + ".*")) {
			synchronized (mConnectEventRunnables) {
				if (Constants.DEBUG)
					System.out.println("Sending rcvToken " + rcvToken);
				if (!Messaging.getInstance().isReady()) {
					mConnectEventRunnables.add(new Runnable() {

						@Override
						public void run() {
							Messaging.getInstance().
								sendDispatcherRequestStreamRcv(rcvToken);
						}
					});
				} else {
					Messaging.getInstance().sendDispatcherRequestStreamRcv(rcvToken);
				}
			}
			intent.setData(null);
			showProgress();
			return true;

		} else
			return false;
	}

	/**
	 * Runs the Runnables in the List for connection event
	 */
	public void runConnectEventRunnables() {
		for (Runnable r : mConnectEventRunnables) {
			r.run();
		}
		mConnectEventRunnables.clear();
	}

	/**
	 * Listener for changed preference values
	 */
	private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {
			if (key.equals(Constants.PREF_CONNECTION_STATUS)) {
				if (mPrefs.getBoolean(Constants.PREF_CONNECTION_STATUS, false)) {
					synchronized (mConnectEventRunnables) {
						runConnectEventRunnables();
					}
				}
			} else if (key.equals(Constants.PREF_BACKGROUND_SERVICE_LISTEN)) {
				checkForService();
			}
		}
	};

	// / HELPERS ///

	/**
	 * Shows a Dialog containing given text
	 * 
	 * @param text
	 *            the text to display
	 */
	@SuppressWarnings("unused")
	private void showDialog(String text) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(text)
				.setCancelable(false)
				.setPositiveButton(getString(R.string.button_close),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(this);
		alert.show();
	}

	/**
	 * Shows a ProgressDialog
	 */
	public void showProgress() {
		mProgressDialog = ProgressDialog.show(Main.this, null,
				getString(R.string.msg_connecting), true, false);
		mProgressDialog.setOwnerActivity(this);
		mProgressDialog.setCancelable(true);
		mProgressDialog.show();
		getWindow().getDecorView().postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
			}
		}, 3000);
	}

	/**
	 * Dismisses a ProgressDialog
	 */
	public void dismissProgress() {
		if (mProgressDialog != null && mProgressDialog.isShowing())
			mProgressDialog.dismiss();
	}

	/**
	 * Shows a Toast containig the given text
	 * 
	 * @param msg
	 *            the text to show
	 */
	private void showToast(final String msg) {
		Runnable toastRunnable = new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG)
						.show();
			}
		};
		runOnUiThread(toastRunnable);
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
}