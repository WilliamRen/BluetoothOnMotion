package com.elsewhat.android.onmotion;

import android.content.Context;
import android.content.SharedPreferences;

public class BluetoothOnMotionPreferences {
	public final String PREFERENCES_ID="onmotion";

	//these are the types of notification with locations we support
	public final static String NOTIFICATION_WITH_LOCATION_DISABLED="Disabled";
	public final static String NOTIFICATION_WITH_LOCATION_MAPS="Maps";
	public final static String NOTIFICATION_WITH_LOCATION_STREETVIEW="Streetview";
	public final static String NOTIFICATION_WITH_LOCATION_RADAR="Radar";	
	
	//Default values of the application
	private final static int DEFAULT_MIN_TIME_NETWORK=60;//60000
	private final static int DEFAULT_MIN_TIME_GPS=60;//60000
	private final static int DEFAULT_MIN_DISTANCE_NETWORK=500;//500
	private final static int DEFAULT_MIN_DISTANCE_GPS=500;//500
	private final static int DEFAULT_MIN_SPEED_FOR_CHANGE=30;
	private final static boolean DEAFULT_SERVICE_START_ON_BOOT=false;
	private final static boolean DEFAULT_CREATE_NOTIFICATION_WITH_LOCATION=true;
	private final static boolean DEFAULT_CREATE_NOTIFICATION_ON_TOGGLE=true;
	private final static String DEFAULT_NOTIFICATION_WITH_LOCATION_MULTI=NOTIFICATION_WITH_LOCATION_DISABLED;

	//Keys for the values
	private final static String KEY_MIN_TIME_NETWORK="minTimeNetwork";
	private final static String KEY_MIN_TIME_GPS="minTimeGPS";
	private final static String KEY_MIN_DISTANCE_NETWORK="minDistanceNetwork";
	private final static String KEY_MIN_DISTANCE_GPS="minDistanceGPS";
	private final static String KEY_MIN_SPEED_FOR_CHANGE="minSpeedForChange";
	private final static String KEY_SERVICE_START_ON_BOOT="doServiceStartOnBoot";	
	private final static String KEY_CREATE_NOTIFICATION_WITH_LOCATION="doNotificationWithLocation";
	private final static String KEY_CREATE_NOTIFICATION_ON_TOGGLE="doNotificationOnToggle";
	private final static String KEY_CREATE_NOTIFICATION_WITH_LOCATION_MULTI="typeOfNotificationWithLocation";
	
	public final static String HELP_URL="http://code.google.com/p/android-bluetooth-on-motion/wiki/UserGuide";
	
	private SharedPreferences preferences;
	
	public BluetoothOnMotionPreferences(Context context){
		//read the SharedPreferences which is used for the application
		preferences = context.getSharedPreferences(PREFERENCES_ID, 0);
	}
	
	/**
	 * Store the preferences in a SharedPreferences object
	 * 
	 * @param bDoServiceStartOnBoot
	 * @param bCreateNotificationWithLocation
	 * @param minSpeedForChange
	 * @param minTimeForNetwork
	 * @param minDistanceForNetwork
	 * @param minTimeForGPS
	 * @param minDistanceForGPS
	 */
	public void storePreferences(boolean bDoServiceStartOnBoot, boolean bCreateNotificationWithLocation, String notificationWithLocationType,boolean bCreateNotificationOnToggle,int minSpeedForChange, int minTimeForNetwork,int minDistanceForNetwork, int minTimeForGPS,int minDistanceForGPS){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(KEY_MIN_TIME_NETWORK, minTimeForNetwork);
		editor.putInt(KEY_MIN_TIME_GPS, minTimeForGPS);
		editor.putInt(KEY_MIN_DISTANCE_NETWORK, minDistanceForNetwork);
		editor.putInt(KEY_MIN_DISTANCE_GPS, minDistanceForGPS);
		editor.putInt(KEY_MIN_SPEED_FOR_CHANGE, minSpeedForChange);	
		editor.putBoolean(KEY_SERVICE_START_ON_BOOT, bDoServiceStartOnBoot);
		editor.putBoolean(KEY_CREATE_NOTIFICATION_WITH_LOCATION, bCreateNotificationWithLocation);
		editor.putString(KEY_CREATE_NOTIFICATION_WITH_LOCATION_MULTI, notificationWithLocationType);
		editor.commit();		
	}
	
	public void clearPreferences(){
		SharedPreferences.Editor editor = preferences.edit();
		editor.clear();
		editor.commit();		
	}	
	
	public int getMinTimeNetwork(){
		return preferences.getInt(KEY_MIN_TIME_NETWORK, DEFAULT_MIN_TIME_NETWORK);
	}
	public int getMinTimeGPS(){
		return preferences.getInt(KEY_MIN_TIME_GPS, DEFAULT_MIN_TIME_GPS);
	}
	public int getMinDistanceNetwork(){
		return preferences.getInt(KEY_MIN_DISTANCE_NETWORK, DEFAULT_MIN_DISTANCE_NETWORK);
	}
	public int getMinDistanceGPS(){
		return preferences.getInt(KEY_MIN_DISTANCE_GPS, DEFAULT_MIN_DISTANCE_GPS);
	}	
	public int getMinSpeedForChange(){
		return preferences.getInt(KEY_MIN_SPEED_FOR_CHANGE, DEFAULT_MIN_SPEED_FOR_CHANGE);
	}
	public boolean getDoServiceStartOnBoot(){
		return preferences.getBoolean(KEY_SERVICE_START_ON_BOOT, DEAFULT_SERVICE_START_ON_BOOT);
	}		

	public boolean getDoNotificationWithLocation() {
		return preferences.getBoolean(KEY_CREATE_NOTIFICATION_WITH_LOCATION,DEFAULT_CREATE_NOTIFICATION_WITH_LOCATION);
	}
	
	public String getNotificationWithLocationType() {
		return preferences.getString(KEY_CREATE_NOTIFICATION_WITH_LOCATION_MULTI,DEFAULT_NOTIFICATION_WITH_LOCATION_MULTI);
	}	
	
	public boolean getDoNotificationOnToggle() {
		return preferences.getBoolean(KEY_CREATE_NOTIFICATION_ON_TOGGLE, DEFAULT_CREATE_NOTIFICATION_ON_TOGGLE);
	}
	
}
