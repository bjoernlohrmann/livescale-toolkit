package de.tuberlin.cit.livestream.android;

import android.content.Context;

public class Helper {

	/**
	 * the current applicationContext
	 */
	private static Context mAppContext;

	/**
	 * Gets the current application Context
	 * 
	 * @return Context of current Application
	 */
	public static Context getApplicationContext() {
		return mAppContext;
	}

	/**
	 * Sets the current application Context
	 * 
	 * @param appContext
	 *            Context of current Application
	 */
	public static void setApplicationContext(Context appContext) {
		mAppContext = appContext;
	}

	/**
	 * Uses given information to build a custom citstreamer:// URL for watching
	 * a stream
	 * 
	 * @param rcvHost
	 *            the host to connect to
	 * @param rcvPort
	 *            the port to connect to
	 * @param rcvToken
	 *            the receive Token
	 * @return custom citstreamer:// URL
	 */
	public static String encodeCustomWatchURL(String rcvHost, int rcvPort,
			String rcvToken) {
		return Constants.URL_CUSTOM_SCHEME + "://" + rcvHost + "/" + rcvPort
				+ "/watch/" + rcvToken;
	}

	/**
	 * Uses given information to build an http:// URL for connection to a
	 * CITstreamer Webserver within an Dispatcher installation to watch a
	 * videostream
	 * 
	 * @param dispatcherHost
	 *            the host to connect to
	 * @param dispatcherPort
	 *            the port to connect to
	 * @param rcvToken
	 *            the receive Token
	 * @return http:// URL
	 */
	public static String encodeHTTPWatchURL(String dispatcherHost,
			int dispatcherPort, String rcvToken) {
		return "http://" + dispatcherHost + "/" + dispatcherPort + "/watch/"
				+ rcvToken;
	}

	/**
	 * Uses given information to build an http:// URL for receiving videostream
	 * data
	 * 
	 * @param rcvHost
	 *            the host to connect to
	 * @param rcvPort
	 *            the port to connect to
	 * @param rcvToken
	 *            the receive Token
	 * @return http:// URL
	 */
	public static String encodeHTTPDirectStreamURL(String rcvHost, int rcvPort,
			String rcvToken) {
		// return "http://" + rcvHost + ":" + rcvPort + "/" + rcvToken +
		// "/livestream.ts"; // old style
		return "http://" + rcvHost + ":" + rcvPort + "/live.ts?auth="
				+ rcvToken;
	}
}
