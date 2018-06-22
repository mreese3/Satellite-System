package edu.esu.syssoft.androidcontroller;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;



// GPSHandler class.
// We are only interested in the NMEA output, so most of the Location
// overhead is irrelevant.

// The use of the Service class allows this to exist without an activity.
// As you can, subclassing this into AndroidController would be a maintenance nightmare.
public class GPSHandler extends Service implements GpsStatus.NmeaListener, LocationListener{

	// getSystemService is part of the Activity class. Therefore, we need a context.
	// Passing contexts like hot potatoes is fun for the whole family!
	// (Java doesn't support subclassing in alternate files, so this is what we have.)
	private Context context = null;

	// The LocationManager update code requires a time delay in ms, and a distance
	// in meters. These are wildly adjustable. Don't be scared!
	private static final int MINIMUM_TIME_TO_UPDATE = 500;
	private static final int MINIMUM_DISTANCE_TO_UPDATE = 1;
	// The LocationManager actually handles the GPS events. It's pretty important.
	private LocationManager locationManager = null;

	// This is our pride an joy. Regularly checking of this will allow easy compilation
	// for the JSON creation.
	private String NMEAString = "";
	// This one is a fun one. Android requires permissions checking to be done before
	// you can modify the LocationManager object. In API 23 and above, this can be triggered
	// at runtime. That is not ideal, especially since we don't want user interaction.
	// As such, these two variables are set to false, if an only if, the LocationManager aspects
	// fail miserably.
	private boolean locationUpdatesAdded = true;
	private boolean nmeaListerAdded = true;

	// Overall global to dictate the GPS level of Sanity. This really boils down
	// to the Android Permissions scheme, and it's implications in automated applications.
	private boolean GPSIsUsable = false;



	public GPSHandler(Context c) {
		// Note that localContext should NEVER be null! If I send a null context to
		// the constructor, I am worse programmer than I think I am. (That's bad...)
		context = c;
		setupLocationManager();
	}

	// Public method for parent error control. Better to know the GPS is
	// broken before we wait around for an update.
	public boolean getGPSStatus() {
		return GPSIsUsable;
	}

	private boolean GPSAvailable() {
		// This is the safest method to check for the GPS to be in a sane state.
		// Accuracy and usefulness are not guaranteed, but accessibility is!
		boolean ret = false;

		if (locationManager != null) {
			ret = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}
		return ret;
	}

	private void setupLocationManager() {
		// The cast is a side-effect of context switching. We shouldn't need error checking here, because
		// Context.getSystemService should be always return what I requested *fingers crossed*
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		GPSIsUsable = GPSAvailable();
		if (GPSIsUsable) {
			try {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
								      MINIMUM_TIME_TO_UPDATE,
								      MINIMUM_DISTANCE_TO_UPDATE,
								      this);
			}
			catch (SecurityException e) {
				utils.msgoutput(context, "No permissions for GPS Updates. Service does nothing.");
				GPSIsUsable = false;
				locationUpdatesAdded = false;
			}
			try {
				locationManager.addNmeaListener(this);
			}
			catch (SecurityException e){
				utils.msgoutput(context, "No permissions for GPS NMEA listener. Service does nothing.");
				GPSIsUsable = false;
				nmeaListerAdded = false;
			}

		}
		else {
			utils.msgoutput(context, "GPS Available returned false!");
		}
	}

	public void onDestroy() {
		// If the GPS is garbage, we still need to try to deallocate whatever we added
		// correctly. Bear in mind, this code will probably never fire...
		if (locationUpdatesAdded) {
			try {
				locationManager.removeUpdates(this);
			}
			catch (SecurityException e) {
				utils.msgoutput(context, "Failed removing updates, this an odd state.");
			}
		}
		if (nmeaListerAdded) {
			try {
				locationManager.removeNmeaListener(this);
			}
			catch (SecurityException e) {
				utils.msgoutput(context, "Failed nmealistener, this an odd state.");
			}
		}
	}


	public String getNMEAString() {
		return NMEAString;
	}



	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		// This is the magic. When this service gets to run, we automagically
		// get a new NMEA string. It only took 150 LOC.
		NMEAString = nmea;
	}



	// These are all garbage methods to implement required interfaces.
	// They are important, but we don't have to do anything for them.

	// The onBind method is a Service oddity, that we really don't care about...
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// Location Listener
	@Override
	public void onLocationChanged(Location location) {

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onProviderDisabled(String provider) {

	}
}
