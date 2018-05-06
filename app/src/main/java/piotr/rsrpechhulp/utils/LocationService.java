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
import android.support.annotation.RequiresPermission;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class LocationService extends Service implements LocationListener {

    private static final String TAG = LocationService.class.getSimpleName();

    public static final String ACTION_LOCATION_CHANGED = LocationService.class.getCanonicalName() + ".LOCATION";
    public static final String ACTION_NO_GPS = LocationService.class.getCanonicalName() + ".GPS";

    public static final String LOCATION_INTENT_EXTRAS = "LOCATION";

    private static final int LOCATION_UPDATE_INTERVAL = 1000;
    private static final float LOCATION_UPDATE_MIN_DISTANCE = 10.0f;

    private LocationManager locationManager;

    private Location currentBestLocation;

    @Override
    public void onCreate() {
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @SuppressWarnings({"MissingPermission"})
    public void startListening() {
        //if it's enabled, it'll provide better location
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_MIN_DISTANCE, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_MIN_DISTANCE, this);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_MIN_DISTANCE, this);

        final Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(lastKnownLocation != null)
            onLocationChanged(lastKnownLocation);
    }

    private void sendNoGPS() {
        Intent intent = new Intent(ACTION_NO_GPS);
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
        if(isBetterLocation(location)){
            currentBestLocation = location;
            sendNewLocation(location);
            Log.i(TAG, "sending new best location " + location.toString());
        }
    }

    private void sendNewLocation(Location location) {
        Intent intent = new Intent(ACTION_LOCATION_CHANGED);
        intent.putExtra(LOCATION_INTENT_EXTRAS, location);
        sendLocalBroadcast(intent);
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     */
    protected boolean isBetterLocation(Location location) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) { }
    @Override
    public void onProviderEnabled(String s) { }
    @Override
    public void onProviderDisabled(String s) {
        if(s.equals(LocationManager.GPS_PROVIDER)) {
            sendNoGPS();
        }
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
