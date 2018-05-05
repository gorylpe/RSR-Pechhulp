package piotr.rsrpechhulp.utils;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class LocationService extends Service implements LocationListener {

    private static final String TAG = LocationService.class.getSimpleName();

    public static final String ACTION_LOCATION_CHANGED = LocationService.class.getCanonicalName() + ".LOCATION";
    public static final String ACTION_NO_PERMISSIONS = LocationService.class.getCanonicalName() + ".PERMISSIONS";
    public static final String ACTION_NO_GPS = LocationService.class.getCanonicalName() + ".GPS";

    public static final String LOCATION_INTENT_EXTRAS = "LOCATION";

    private static final int LOCATION_UPDATE_INTERVAL = 1000;
    private static final float LOCATION_UPDATE_MIN_DISTANCE = 10.0f;

    private LocationManager locationManager;

    @Override
    public void onCreate() {
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        Log.i(TAG, "Creating service");
    }

    public void startListening() {
        if(Utils.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_MIN_DISTANCE, this);
        } else {
            Log.i(TAG, "no perms");
            sendNoPermissions();
        }
    }

    private void sendNoPermissions() {
        Intent intent = new Intent(ACTION_NO_PERMISSIONS);
        sendLocalBroadcast(intent);
    }

    public void stopListening() {
        locationManager.removeUpdates(this);
    }

    private void sendLocalBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged " + location.toString());
        sendNewLocation(location);
    }

    private void sendNewLocation(Location location) {
        Intent intent = new Intent(ACTION_LOCATION_CHANGED);
        intent.putExtra(LOCATION_INTENT_EXTRAS, location);
        sendLocalBroadcast(intent);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) { }
    @Override
    public void onProviderEnabled(String s) { }
    @Override
    public void onProviderDisabled(String s) {
        Log.i(TAG, "onProviderDisabled " + s);
        Intent intent = new Intent(ACTION_NO_GPS);
        sendLocalBroadcast(intent);
    }


    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
