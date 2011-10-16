package com.elsewhat.android.onmotion;

import java.util.Iterator;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * This service provides the core of the application
 * 
 * It has no fixed loop, instead it reacts to the events it receives.
 * 
 * It listens to the location service in order to get events that
 * the location has changed. The location service requires the following
 * uses-permissions in the AndroidManifest.xml :
 * android.permission.ACCESS_COARSE_LOCATION
 * android.permission.ACCESS_FINE_LOCATION
 * 
 * It listens to the bluetooth service in order to get events of 
 * change in bluetooth state 
 * 
 * The bluetooth handling requires the following uses-permissions
 * in the AndroidManifest.xml : 
 * android.permission.BLUETOOTH
 * android.permission.BLUETOOTH_ADMIN
 * (BLUETOOTH_ADMIN required in order to enable/disable bluetooth without user interaction)
 *  
 * @author dagfinn.parnas http://twitter.com/dparnas
 */
public class BluetoothOnMotionService extends Service{
	private final static boolean DEBUG=true;
	//parameters read from the preferences
	private int minTimeNetwork;
	private int minTimeGPS;
	private int minDistanceNetwork;
	private int minDistanceGPS;
	private boolean bNotificationOnToggle;
	private boolean bNotificationWithLocation;
	private String bNotificationWithLocaitonType;
	private float minSpeedForChangeMS;

	//is this service started
	private boolean bIsServiceStarted=false;
	
	//bluetooth interface
	private BluetoothAdapter bluetoothAdapter;
	private BroadcastReceiver bluetoothReceiver;
	private boolean bDeviceSupportsBluetooth=false;
	
	//location interface
	private LocationManager locationManager ; 
	private LocationListener locationListener;

	//class which stores previous locations
	private LocationHistory locationHistory;

	//easier access to resources R.strings from code
	private Resources res;
	
	//identifiers for notifications
	private int NOTIFICATION_TOGGLE_ID=1;
	private int NOTIFICATION_LOCATION_ID=2;

	/**
	 * Create the service, but do not setup the listeners.
	 * (this is done in onStart)
	 * 	
	 */
	public void onCreate() {
	    super.onCreate();
	    
	    res= this.getResources();
	    
	    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (bluetoothAdapter == null) {
	    	bDeviceSupportsBluetooth=false;
	    	Log.e(this.getClass().getName(), "Device does not support bluetooth. Service will not start");
	    	//TODO: create notification that bluetooth is not support on current device
	    }else {
	    	bDeviceSupportsBluetooth=true;
	    }

	}
	
	/**
	 * onStart is called automatically if started 
	 * on boot, but not if bound to from activity
	 * 
	 * Sets up the required listeners
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if(bDeviceSupportsBluetooth){
			doUpdatePreferences();
			//5 locations are stored. Can be used for more advanced speed
			//calculations in the future
			locationHistory = new LocationHistory(5);
	    	setupBluetoothListener();
	    	setupLocationListener();
    	}
		bIsServiceStarted=true;
	}

	
	
	/**
	 * Read the preferences that have been set 
	 * by the activity.
	 * 
	 */
	public void doUpdatePreferences(){
		// if no user is setup, redirect to setup
		BluetoothOnMotionPreferences preferences =  new BluetoothOnMotionPreferences(this);
		//preference stored in seconds, we need milliseconds
		minTimeNetwork=preferences.getMinTimeNetwork()*1000;
		minTimeGPS=preferences.getMinTimeGPS()*1000;
		
		minDistanceNetwork=preferences.getMinDistanceNetwork();
		minDistanceGPS=preferences.getMinDistanceGPS();
		//speed is stored in km/h,but we need meters/second
		minSpeedForChangeMS=preferences.getMinSpeedForChange()/3.6f;
		
		bNotificationOnToggle=preferences.getDoNotificationOnToggle();
		bNotificationWithLocation = preferences.getDoNotificationWithLocation();
		bNotificationWithLocaitonType = preferences.getNotificationWithLocationType();
		
		//If the service is already started, we will reset the location listener
		//with the new settings
		if(bIsServiceStarted){
			locationManager.removeUpdates(locationListener);
	    	setupLocationListener();
		}
	}
	
	/**
	 * Setup the location listener and action handler
	 * 
	 * The action handler contains the main logic of the application
	 * 
	 */
	private void setupLocationListener() {
		locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE); 
		locationListener = new LocationListener(){
			/**
			 * Action method will be called when the location changes. 
			 * This is where we trigger if the bluetooth should
			 * be enabled or disabled
			 */
			@Override
			public void onLocationChanged(Location location) {
				if(DEBUG)
					Log.d(this.getClass().getName(), "Location changed  :"+ location.toString());
				
				locationHistory.addLocation(location);
				float speed;
				if (location.hasSpeed()){
					speed=location.getSpeed();
				}else {
					speed = locationHistory.getEstimatedSpeed();
				}
				
				if(DEBUG)
					Log.d(this.getClass().getName(), "Speed estimated to " + speed + " meters pr second");
				
				//enable check
				if(!bluetoothAdapter.isEnabled() && speed>minSpeedForChangeMS){
					Log.i(this.getClass().getName(), "Enabling bluetooth since speed " + speed + " is larger than " + minSpeedForChangeMS);					
					enableBluetooth();
					if(bNotificationOnToggle){
						createNotificationOnToggle(true);
					}
				}
				//TODO: Check for disabling
			}


			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				Log.w(this.getClass().getName(), "Provider " + provider + " changed status to " + status);
			}


			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub	
			}
			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub	
			}
		};
		
		try {
			//listen for both the network and GPS. However, we must assume GPS is disabled
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeNetwork, minDistanceNetwork,locationListener);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeGPS, minDistanceGPS,locationListener);
		}catch (Throwable t){
			Log.e(this.getClass().getName(), "Could not set location updates", t);
		}
	}
	
	/**
	 * Setup the bluetooth listener
	 * in order to recieve any changes to the bluetooth adapter
	 * 
	 * This listener is not strictly needed and may be removed in the future
	 */
	private void setupBluetoothListener(){
		//receiver of bluetooth events.
		bluetoothReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
		        String action = intent.getAction();
		        
	        	Bundle b = intent.getExtras();
	        	
	        	
	        	if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
	        		//if bluetooth is connected with a device we just log it
	        		Log.i(this.getClass().getName(),"ACTION_ACL_CONNECTED A bluetooth device has been connected");
	        	}else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
	        		//if bluetooth is disconnected with a device
	        		//-turn off bluetooth
	        		//-create notification(s)
	        		Log.i(this.getClass().getName(),"ACTION_ACL_DISCONNECTED A bluetooth device has been disconnected. Therefore, we are turning off bluetooth");
	        		disableBluetooth();
	        		if(bNotificationOnToggle){
	        			createNotificationOnToggle(false);
	        		}
	        		if (bNotificationWithLocation){
	        			
	        			try {
	        				Criteria criteria = new Criteria();
		        			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		        			String bestProvider= locationManager.getBestProvider(criteria, true);
	        				Location loc = locationManager.getLastKnownLocation(bestProvider);
		        			if(loc!=null){
		        				createNotificationOnLocation(loc);
		        				Log.i(this.getClass().getName(),"Create notification with location " + loc.toString());
		        			}
	        			}catch (IllegalArgumentException e) {
							Log.w(this.getClass().getName(), "Location not found for best provider ", e);
						}catch (RuntimeException e){
							Log.w(this.getClass().getName(), "Runtime exception when trying to create notification with location ", e);
						}

	        		}
	        		
	        	}else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
	        		//if the adapter has changed status (on/off/starting...etc)
	        		//we just log it
	        		Log.i(this.getClass().getName(),"ACTION_STATE_CHANGED The status of the bluetooth adapter has changed");
	        	}

	        	/* Some debug code
	        	String strLog ="";
	        	Set keys= b.keySet();
	        	for (Iterator iterator = keys.iterator(); iterator
						.hasNext();) {
					String strKey = (String) iterator.next();
					Object value= b.get(strKey);
					strLog = strLog+ strKey + "="+value + " ";						
				}
	        	Log.w(this.getClass().getName(), "Bluetooth state changed action:"+ action + " extras:" + strLog);
		        */
		   
		    }
		};
		
		//this actions are triggered when a bluetooth device is connected
		registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));	
		registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED));	
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_CLASS_CHANGED));	
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));	
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE));
		//registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
		//this action is triggered when bluetooth is turned off or on
		registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		
		
	}
	
	/**
	 * Called when we want to enable bluetooth
	 * Requires both
	 *   <uses-permission android:name="android.permission.BLUETOOTH" />
  	 *   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
	 */
	private void enableBluetooth(){
		if (!bluetoothAdapter.isEnabled()) {
		    //the standard way of turning on bluetooth equires user input and is therefore not suitable
			//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    //enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    //startActivity(enableBtIntent);
			bluetoothAdapter.enable();
		}
	}
	
	/**
	 * Called when we want to disable bluetooth
	 * Requires both
	 *   <uses-permission android:name="android.permission.BLUETOOTH" />
  	 *   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
	 */	
	private boolean disableBluetooth(){
		if (bluetoothAdapter.isEnabled()) {
			return bluetoothAdapter.disable();
		}else {
			//TODO: should we really return boolean here? might trigger a notification from calling party
			return true;
		}
		
	}

	
	/**
	 * Create a notification into the system tray
	 * Should be called every time we enable or disable bluetooth
	 * 
	 * It will reuse the same notification message for enable and disable
	 * 
	 * 
	 */
	private void createNotificationOnToggle(boolean isEnable){
		//we have reuse the sys warning icon
		int icon = android.R.drawable.stat_sys_warning;        // icon from resources
		long when = System.currentTimeMillis();         // notification time
		
		//Create the intent
		Intent notificationIntent = new Intent(this, BluetoothOnMotion.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,0 );
		
		//trigger the notification
		Notification notification;
		if(isEnable){
			notification = new Notification(icon,res.getString(R.string.notificationEnableTicker) , when);
			notification.setLatestEventInfo(this, res.getText(R.string.notificationEnableTitle), res.getText(R.string.notificationEnableMessage), contentIntent);
		}else {
			notification= new Notification(icon,res.getString(R.string.notificationDisableTicker) , when);
			notification.setLatestEventInfo(this, res.getText(R.string.notificationDisableTitle), res.getText(R.string.notificationDisableMessage), contentIntent);			
		}

		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_TOGGLE_ID, notification);
	}
	
	/**
	 * Create a notification into the system tray
	 * Should be called if the user wants to store a certain location
	 * such as where the car is parked
	 * 
	 */
	private void createNotificationOnLocation(Location loc){
		if(loc==null){
			return;
		}
		//we have reuse the sys warning icon
		int icon = android.R.drawable.star_off;        // icon from resources
		long when = System.currentTimeMillis();         // notification time
		
		//Create the intent
		Intent notificationIntent;
		
		if(bNotificationWithLocaitonType.equals(BluetoothOnMotionPreferences.NOTIFICATION_WITH_LOCATION_RADAR)){
			notificationIntent = new Intent ("com.google.android.radar.SHOW_RADAR"); 
			notificationIntent.putExtra("latitude",loc.getLatitude()); 
			notificationIntent.putExtra("longitude",loc.getLongitude() ); 
		}else if (bNotificationWithLocaitonType.equals(BluetoothOnMotionPreferences.NOTIFICATION_WITH_LOCATION_STREETVIEW)){
			//google streetview integration
			notificationIntent = new Intent(Intent.ACTION_VIEW);
			notificationIntent.setData(Uri.parse("google.streetview:cbll=" + loc.getLatitude()+ ","+ loc.getLongitude()));
		}else {
			//Google maps integrations is default
			notificationIntent = new Intent(Intent.ACTION_VIEW);
			notificationIntent.setData(Uri.parse("geo:" + loc.getLatitude()+ ","+ loc.getLongitude())); 
		}
		//
		
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,0 );
		
		//trigger the notification
		Notification notification = new Notification(icon,res.getString(R.string.notificationLocationTicker) , when);
		notification.setLatestEventInfo(this, res.getText(R.string.notificationLocationTitle), res.getText(R.string.notificationLocationMessage), contentIntent);
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_LOCATION_ID, notification);
	}
	
	/**
	 * Called when the service should be destroyed
	 */
	@Override
	public void onDestroy() {
		Log.i(this.getClass().getName(),"In Service OnDesctroy and will remove all listeners");

		//make sure all receivers are destroyed
		unregisterReceiver(bluetoothReceiver);
		bluetoothReceiver=null;
		bluetoothAdapter=null;
		locationManager.removeUpdates(locationListener);
		locationListener=null;
		locationManager=null;
		bIsServiceStarted=false;
		super.onDestroy();
	}
	/**
	 * Return the communication channel to the service. 
	 */
	@Override
	public IBinder onBind(Intent intent) {
	    return serviceBinder;
	}

    /**
     * An interface to the service which can be called from other activities
     * These are the methods available to the setup activity
     */
    private final IOnMotionService.Stub serviceBinder = new IOnMotionService.Stub() {
        public boolean isServiceStarted() {
            return BluetoothOnMotionService.this.isServiceStarted();
        }
        /**
         * Triggers an update of the preferences for the service
         * The service will read the new preferences from
         * SharedPreferences and resetup listeners
         * 
         */
        public void doUpdatePreferences(){
        	BluetoothOnMotionService.this.doUpdatePreferences();
        }
        
        public void doStopService(){
        	BluetoothOnMotionService.this.stopSelf();
        }
    };
	
	public boolean isServiceStarted(){
		return bIsServiceStarted;
	}
	
}