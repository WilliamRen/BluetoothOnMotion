package com.banasiak.android.btom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Simple receiver that will handle the boot completed intent and send the intent to 
 * launch the BluetotthOnMotionService if the configuration indicates this.
 * 
 * Adapted from http://androidenea.blogspot.com/2009/09/starting-android-service-after-boot.html
 */
public class BootReceiver extends BroadcastReceiver {
 @Override
 public void onReceive(final Context context, final Intent bootintent) {
	BluetoothOnMotionPreferences preferences = new BluetoothOnMotionPreferences(context);
	if(preferences.getDoServiceStartOnBoot()){
		Log.i(this.getClass().getName(), "Starting BluetoothOnMotion on boot in accordance with app settings");
		Intent onMovementService = new Intent(context, BluetoothOnMotionService.class);
		context.startService(onMovementService);
	}
 }
}