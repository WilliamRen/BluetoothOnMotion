package com.elsewhat.android.onmotion;



import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The activity will provide the configuration of the application. This will
 * include: - Parameters for how often to check for location events - Parameters
 * for when to turn bluetooth on - Parameters for when to turn bluetooth off -
 * Configuration of how to start the service
 * 
 * It is the BluetoothOnMotionService which does all the fun stuff.
 * 
 * @author dagfinn.parnas http://twitter.com/dparnas
 */
public class BluetoothOnMotion extends Activity {
	private Button bStartService;
	private TextView lblToggleService;
	private EditText txtLocationFrequencyDistance;
	private EditText txtLocationFrequencyTime;
	private EditText txtSpeedRequired;
	private CheckBox cStartOnBoot;
	private CheckBox cCreateNotificationOnToggle;
	private Spinner sCreateNotificationWithLocationType;

	private IOnMotionService onMotionService;

	private boolean bConnected = false;
	private boolean bStarted=false;
	private BluetoothOnMotionPreferences preferences;

	//easier access to resources R.strings from code
	private Resources res;
	
	/**
	 * Called when the activity is first created.
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		res= this.getResources();
		
		// retrieve the form elements
		bStartService = (Button) findViewById(R.id.bToggleService);
		lblToggleService = (TextView) findViewById(R.id.lblToggleService);
		cStartOnBoot = (CheckBox) findViewById(R.id.cStartOnBoot);
		txtLocationFrequencyDistance = (EditText) findViewById(R.id.txtLocationFrequencyDistance);
		txtLocationFrequencyTime = (EditText) findViewById(R.id.txtLocationFrequencyTime);
		txtSpeedRequired = (EditText) findViewById(R.id.txtSpeedRequired);
		cCreateNotificationOnToggle= (CheckBox) findViewById(R.id.cCreateNotificationOnToggle);

		sCreateNotificationWithLocationType = (Spinner) findViewById(R.id.sCreateNotificationWithLocationType);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        adapter.add(BluetoothOnMotionPreferences.NOTIFICATION_WITH_LOCATION_DISABLED);
        adapter.add(BluetoothOnMotionPreferences.NOTIFICATION_WITH_LOCATION_MAPS);
        adapter.add(BluetoothOnMotionPreferences.NOTIFICATION_WITH_LOCATION_STREETVIEW);
        adapter.add(BluetoothOnMotionPreferences.NOTIFICATION_WITH_LOCATION_RADAR);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sCreateNotificationWithLocationType.setAdapter(adapter);
		
		
		populateFromSharedPreferences();

		// connect to the BluetoothOnMotionService
		//bindService();
		updateServiceStatus();

		// setup listener for when we click it
		bStartService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleService();
				updateServiceStatus();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_setup, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuSave:
			saveSharedPreferences();

			//TODO: Update service
			if (bConnected) {
				try {
					onMotionService.doUpdatePreferences();
				}catch (RemoteException e){
					Log.w(this.getClass().getName(),"Could not update preferences for running service",e);
				}
			}
			
			return true;
		case R.id.menuReset:
			preferences.clearPreferences();
			populateFromSharedPreferences();
			Toast.makeText(this,
					R.string.msgPreferencesCleared,
					Toast.LENGTH_LONG).show();

			return true;
		case R.id.menuHelp:
			Intent i = new Intent();
			i.setAction(Intent.ACTION_VIEW);
			i.addCategory(Intent.CATEGORY_BROWSABLE);
			i.setData(Uri.parse(BluetoothOnMotionPreferences.HELP_URL));
			startActivity(i);
			
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}	
	
	private void populateFromSharedPreferences() {
		if (preferences == null) {
			preferences = new BluetoothOnMotionPreferences(this);
		}
		cStartOnBoot.setChecked(preferences.getDoServiceStartOnBoot());
		txtLocationFrequencyDistance.setText(""
				+ preferences.getMinDistanceNetwork());
		txtLocationFrequencyTime.setText("" + preferences.getMinTimeNetwork());
		txtSpeedRequired.setText("" + preferences.getMinSpeedForChange());

		cCreateNotificationOnToggle.setChecked(preferences.getDoNotificationOnToggle());
		//set selected spinner
		String locationType = preferences.getNotificationWithLocationType();
		for (int i=0;i< sCreateNotificationWithLocationType.getCount();i++){
			if (locationType.equals((String)sCreateNotificationWithLocationType.getItemAtPosition(i))){
				sCreateNotificationWithLocationType.setSelection(i);
			}
		}
		
		
	}

	/**
	 * Retrieve values from the UI and save the preferences in
	 * BluetoothOnMotionPreferences
	 * 
	 * If a value is invalid, it will not save the preferences but display a
	 * Toast message to the user
	 * 
	 */
	private void saveSharedPreferences() {
		boolean bDoServiceStartOnBoot = cStartOnBoot.isChecked();

		boolean bCreateNotificationOnToggle = cCreateNotificationOnToggle.isChecked();
		
		int minSpeedForChange;
		try {
			minSpeedForChange = Integer.parseInt(txtSpeedRequired.getText()
					.toString());
		} catch (NumberFormatException e) {
			Toast.makeText(this,
					R.string.lblSpeedRequired + " is not a valid number",
					Toast.LENGTH_LONG).show();
			return;
		}

		int minTimeForNetwork;
		try {
			minTimeForNetwork = Integer.parseInt(txtLocationFrequencyTime
					.getText().toString());
		} catch (NumberFormatException e) {
			Toast.makeText(
					this,
					R.string.lblLocationFrequencyTime
							+ " is not a valid number", Toast.LENGTH_LONG)
					.show();
			return;
		}

		int minDistanceForNetwork;
		try {
			minDistanceForNetwork = Integer
					.parseInt(txtLocationFrequencyDistance.getText().toString());
		} catch (NumberFormatException e) {
			Toast.makeText(
					this,
					R.string.lblLocationFrequencyDistance
							+ " is not a valid number", Toast.LENGTH_LONG)
					.show();
			return;
		}
		
		boolean bCreateNotificationWithLocation=false;
		String createNotificationWithLocationType = (String)sCreateNotificationWithLocationType.getSelectedItem();
		if(!createNotificationWithLocationType.equals(BluetoothOnMotionPreferences.NOTIFICATION_WITH_LOCATION_DISABLED)){
			bCreateNotificationWithLocation=true;
		}

		//store the preferences
		//currently we use the same values for GPS as for network 
		preferences.storePreferences(bDoServiceStartOnBoot,
				bCreateNotificationWithLocation,createNotificationWithLocationType,bCreateNotificationOnToggle, minSpeedForChange,
				minTimeForNetwork, minDistanceForNetwork, minTimeForNetwork,
				minDistanceForNetwork);
		
		Toast.makeText(
				this,
				R.string.msgSettingSaved, Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * Update service status should be called when we have
	 * 
	 */
	private void updateServiceStatus() {
		//used to check isServiceRunning
		if (bStarted) {
			bStartService.setText(R.string.bDisableService);
			lblToggleService.setText(R.string.lblToggleDisable);
		} else {
			bStartService.setText(R.string.bEnableService);
			lblToggleService.setText(R.string.lblToggleEnable);
		}

	}

	/**
	 * Check if the service is running
	 * 
	 */
	private boolean isServiceRunning() {
		if (onMotionService == null) {
			return false;
		} else {
			try {
				return onMotionService.isServiceStarted();
			} catch (RemoteException e) {
				Log.w(this.getClass().getName(),
						"Got remote exception when checking service status", e);
				return false;
			}
		}
	}

	/**
	 * Start the service if it is stopped Stop the service (but not destroy) if
	 * it is started
	 * 
	 */
	private void toggleService() {
		if (bStarted==false) {
			//unbind will destroy the service if it exist and is started from activity
			//(I think)
			unBindService();
			// start the service, but do not tie it to this activity
			// Intent serviceIntent = new Intent(BluetoothOnMotion.this,
			// BluetoothOnMotionService.class);
			// Intent serviceIntent = new Intent();
			Intent serviceIntent = new Intent();
			serviceIntent.setClassName("com.elsewhat.android.onmotion",
					BluetoothOnMotionService.class.getName());

			startService(serviceIntent);
			bindService();
			bStarted=true;

		} else {
			// stop the service through the interface we have
			try {
				bStarted=false;
				if (onMotionService != null) {
					onMotionService.doStopService();
				}
				
			} catch (RemoteException e) {
				Log.w(this.getClass().getName(),
						"Got remote exception when stopping service", e);
			}
		}

	}

	private void bindService() {
		Intent serviceIntent = new Intent();
		serviceIntent.setClassName("com.elsewhat.android.onmotion",
				BluetoothOnMotionService.class.getName());

		bindService(serviceIntent, serviceConnection, 0);
	}

	private void unBindService() {
		if (bConnected) {
			unbindService(serviceConnection);
		}
	}


	/**
	 * Class for interacting with the BluetoothOnMotionService
	 */
	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className,
				IBinder binderService) {
			onMotionService = IOnMotionService.Stub.asInterface(binderService);
			bConnected = true;
			//TODO:Implement a handler so that this thread can update the UI?
			//updateServiceStatus();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			onMotionService = null;
			bConnected = false;
			//TODO:Implement a handler so that this thread can update the UI?
			//updateServiceStatus();
		}

	};
	

	

}