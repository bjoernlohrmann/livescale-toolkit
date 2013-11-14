package de.tuberlin.cit.livestream.android;

public class Constants {

	/**
	 * Debug Output
	 */
	public final static boolean DEBUG = true;

	// Some of the following PREF_ Strings have corresponding android:key values
	// set in xml/preferences.xml - be careful when changing them

	/**
	 * SharedPreferences setting key: AMQP server address
	 */
	public final static String PREF_CAMERA_DEVICE = "camera";

	/**
	 * SharedPreferences setting key: AMQP server port
	 */
	public final static String PREF_VIDEO_QUALITY = "quality";

	/**
	 * SharedPreferences setting key: AMQP server address
	 */
	public final static String PREF_SERVER_ADDRESS = "server_address";

	/**
	 * SharedPreferences setting key: AMQP server port
	 */
	public final static String PREF_SERVER_PORT = "server_port";

	/**
	 * SharedPreferences setting key: Current username
	 */
	public final static String PREF_USERNAME = "username";

	/**
	 * SharedPreferences setting key: Corresponding password
	 */
	public final static String PREF_PASSWORD = "password";

	/**
	 * SharedPreferences setting key: Has the app previously run (to show
	 * welcome screen and preferences only once)?
	 */
	public final static String PREF_FIRST_RUN = "first_run";

	/**
	 * SharedPreferences setting key: State of C2DM background service
	 */
	public final static String PREF_BACKGROUND_SERVICE_LISTEN = "bg_listen";

	/**
	 * SharedPreferences setting key: Current stream favorite state
	 */
	public final static String PREF_CUR_STREAM_FAVORITE = "current_stream_favorite";

	/**
	 * SharedPreferences setting key: Current connection status
	 */
	public final static String PREF_CONNECTION_STATUS = "connection_status";

	/**
	 * SharedPreferences setting key: Current user registration status
	 */
	public final static String PREF_REGISTRATION_OK = "reg_ok";

	/**
	 * SharedPreferences setting key: Last registration message sent by server
	 */
	public final static String PREF_REGISTRATION_MSG = "reg_msg";

	/**
	 * SharedPreferences setting key: Timestamp of last preferences updated
	 */
	public final static String PREF_UPDATE_TIME = "update_time";

	/**
	 * SharedPreferences setting key: Facebook token to access user's data
	 */
	public final static String PREF_SHARE_FACEBOOK_ACCESS_TOKEN = "facebook_access_token";

	/**
	 * SharedPreferences setting key: Expiration time of Facebook access token
	 */
	public final static String PREF_SHARE_FACEBOOK_ACCESS_TOKEN_EXPIRES = "facebook_access_token_expires";

	/**
	 * C2DM Hashkey: Type of message received
	 */
	public final static String C2DM_KEY_MSG_TYPE = "msg_type";

	/**
	 * C2DM Hashkey: Attached data
	 */
	public final static String C2DM_KEY_DATA = "data";

	/**
	 * C2DM Hashkey: A username (field currently only used für msg_type_fav
	 * message)
	 */
	public final static String C2DM_KEY_USERNAME = "username";

	/**
	 * C2DM message type: New stream from favourit message
	 */
	public final static String C2DM_MSG_TYPE_FAV = "msg_type_fav";

	/**
	 * C2DM registration intent endpoint
	 */
	public final static String C2DM_INTENT_REG = "com.google.android.c2dm.intent.REGISTER";

	/**
	 * C2DM registration reply intent filter
	 */
	public final static String C2DM_INTENTFILTER_REG = "com.google.android.c2dm.intent.REGISTRATION";

	/**
	 * C2DM message intent filter
	 */
	public final static String C2DM_INTENTFILTER_RCV = "com.google.android.c2dm.intent.RECEIVE";

	/**
	 * C2DM category intent filter (receive only messages assigned to this app)
	 */
	public final static String C2DM_INTENTFILTER_CATEGORY = "de.tuberlin.cit.livestream.android";

	/**
	 * C2DM sender account to receive messages from (C2DM is deprecated and has been moved to GCM)
	 * @deprecated
	 */
	public final static String C2DM_SENDER = "tuberlincit@gmail.com";
	
	/**
	 * GCM Project Number ID-Token to identify at GCM
	 */
	public static final String GCM_PROJECT_NUMBER = "822944291165";
	
	/**
	 * Regular expression to clean receive token from illegal characters
	 */
	public final static String RCVTOKEN_ILLEGAL_CHARS_REGEXP = "[^a-z,A-Z,0-9,-]";

	/**
	 * Custom URL scheme used by app ("citstreamer://")
	 */
	public final static String URL_CUSTOM_SCHEME = "citstreamer";

	/**
	 * Facebook application key used to authenticate app against Facebook
	 */
	public final static String SHARE_FACEBOOK_KEY = "243072799139347";

	/**
	 * Twitter application key used to authenticate app against Twitter
	 */
	public final static String SHARE_TWITTER_KEY = "HelEde8KfiLdW7Eh4Vo3sA";

	/**
	 * Twitter application secret used to authenticate app against Twitter
	 */
	public final static String SHARE_TWITTER_SECRET = "lidLenHhP9H6K5EO16k0sFGRaYwhMwMzSWJxaeME";
}