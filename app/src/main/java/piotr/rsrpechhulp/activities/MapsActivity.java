package piotr.rsrpechhulp.activities;

import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import piotr.rsrpechhulp.R;
import piotr.rsrpechhulp.utils.*;

import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity {

    private static final int GPS_PERMISSIONS_REQUEST_ON_LOCATION_SERVICE_START_CODE = 200;
    private static final float MINIMUM_LOCATION_ACCURACY = 1000.0f;
    private static final int SEARCH_LOCATION_TIMEOUT = 20000; //miliseconds

    private MapManager mapManager;

    private AlertDialog lastAlertDialog;
    private Button buttonCallNow;
    private RelativeLayout callPanelWrapper;

    private LocationService locationService;
    private boolean locationServiceBound;
    private IntentFilter locationServiceIntentFilter;

    private IntentFilter GPSOrInternetChangedIntentFilter;

    private Location lastLocation;
    private Timer locationTimeoutTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapManager = new MapManager(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_maps);
        mapFragment.getMapAsync(mapManager);

        callPanelWrapper = findViewById(R.id.rl_maps_call_popup_wrapper);
        buttonCallNow = findViewById(R.id.btn_maps_call_now);

        locationServiceIntentFilter = new IntentFilter();
        locationServiceIntentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);

        GPSOrInternetChangedIntentFilter = new IntentFilter();
        GPSOrInternetChangedIntentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        GPSOrInternetChangedIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindToLocationService();
    }

    private void bindToLocationService() {
        Intent intent = new Intent(this, LocationService.class);
        locationServiceBound = bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            locationService = ((LocationService.LocalBinder) iBinder).getService();
            startLocationService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    };

    @Override
    public void onResume() {
        super.onResume();
        checkGPSAndInternetAvailability();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(locationBroadcastReceiver, locationServiceIntentFilter);
        registerReceiver(GPSOrInternetChangedReceiver, GPSOrInternetChangedIntentFilter);
        startLocationService();
    }

    /**
     * Check for gps permission, requests if not granted, otherwise starts LocationService location listening
     */
    @SuppressWarnings({"MissingPermission"})
    private void startLocationService() {
        if(locationService != null) {
            if (!Utils.checkGPSPermissions(this)) {
                Utils.requestGPSPermissions(this, GPS_PERMISSIONS_REQUEST_ON_LOCATION_SERVICE_START_CODE);
            } else {
                locationService.startListening();
            }
        }
    }

    /**
     * onReceive called when LocationService notifies about new available location
     */
    private BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(null == action)
                return;

            //Only checking actions from LocationService
            if (action.equals(LocationService.ACTION_LOCATION_CHANGED)) {
                final Location location = intent.getParcelableExtra(LocationService.LOCATION_INTENT_EXTRAS);
                onLocationReceived(location);
            }
        }
    };

    private void onLocationReceived(Location location) {
        hideLocationObtainingImage();
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mapManager.setLocation(latLng);
    }

    private void hideLocationObtainingImage() {
        findViewById(R.id.imv_maps_location_obtaining).setVisibility(View.GONE);
    }

    /**
     * onReceive called when GPS or Connectivity state changed and requires availability check.
     */
    private BroadcastReceiver GPSOrInternetChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action == null) return;

            if(action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)
            || action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                checkGPSAndInternetAvailability();
            }
        }
    };

    /**
     * Checks GPS enabled state and Internet connectivity.
     * If there any error occurs, creates proper AlertDialog,
     * if not starts timer to check if location searching doesn't last too long.
     */
    private void checkGPSAndInternetAvailability() {
        // Don't check if previous dialog is still opened
        if(isActiveAlertDialog())
            return;

        if(!Utils.checkGPSEnabled(this))
            (lastAlertDialog = Utils.buildAlertMessageGpsDisabled(this)).show();
        else if(!Utils.checkInternetConnectivity(this))
            (lastAlertDialog = Utils.buildAlertMessageNoInternet(this, onRetryCheckClick)).show();
        else {
            startLocationSearchingTimeoutTimer();
        }
    }

    private boolean isActiveAlertDialog() {
        return lastAlertDialog != null && lastAlertDialog.isShowing();
    }

    /**
     * Start location searching timeout timer.
     * After the time has passed runs checking last received location correctness
     */
    private void startLocationSearchingTimeoutTimer() {
        if(locationTimeoutTimer != null)
            locationTimeoutTimer.cancel();

        locationTimeoutTimer = new Timer();
        locationTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkLastLocationAccuracy();
                    }
                });
            }
        }, SEARCH_LOCATION_TIMEOUT);
    }

    /**
     * Checks if we received any location from service or if we do check if its accuracy is enough
     */
    private void checkLastLocationAccuracy() {
        if(lastLocation == null || lastLocation.getAccuracy() > MINIMUM_LOCATION_ACCURACY) {
            if(!isActiveAlertDialog()){
                (lastAlertDialog = Utils.buildAlertMessageBadLocation(this, onRetryCheckClick)).show();
            }
        }
    }

    private final OnRetryClickListener onRetryCheckClick = new OnRetryClickListener() {
        @Override
        public void onRetryClick() {
            if(lastAlertDialog != null) lastAlertDialog.dismiss();
            checkGPSAndInternetAvailability();
        }
    };

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(locationBroadcastReceiver);
        unregisterReceiver(GPSOrInternetChangedReceiver);
        if(locationService != null)
            locationService.stopListening();
        if(locationTimeoutTimer != null)
            locationTimeoutTimer.cancel();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(locationServiceBound){
            unbindService(locationServiceConnection);
            locationServiceBound = false;
        }
    }

    /**
     * Tries to start location service once again when there was needed requesting GPS permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case GPS_PERMISSIONS_REQUEST_ON_LOCATION_SERVICE_START_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService();
                } else {
                    Toast.makeText(this, R.string.error_gps_no_permissions, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    public void btnCallNowClick(View view) {
        callPanelWrapper.setVisibility(View.VISIBLE);
        buttonCallNow.setVisibility(View.GONE);
    }

    public void btnPopupCloseClick(View view) {
        callPanelWrapper.setVisibility(View.GONE);
        buttonCallNow.setVisibility(View.VISIBLE);
    }

    public void btnDialClick(View view) {
        Utils.dialIfAvailable(this, getString(R.string.popup_phone));
    }

    public void btnBackClick(View view) {
        finish();
    }
}
