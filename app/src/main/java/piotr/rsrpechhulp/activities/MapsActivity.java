package piotr.rsrpechhulp.activities;

import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import piotr.rsrpechhulp.R;
import piotr.rsrpechhulp.utils.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, AddressObtainTask.Callback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int GPS_PERMISSIONS_REQUEST_ON_LOCATION_SERVICE_START_CODE = 200;

    private Button buttonCallNow;
    private RelativeLayout callPanelWrapper;

    private GoogleMap map;
    private Marker marker;
    private static final float MAP_ZOOM = 16.0f;

    private LocationService locationService;
    private boolean locationServiceBound;
    private IntentFilter locationServiceIntentFilter;

    private IntentFilter GPSSwitchStateIntentFilter;

    private static final float MINIMUM_LOCATION_ACCURACY = 1000.0f;
    private static final int SEARCH_LOCATION_TIMEOUT = 20000;
    private static final LatLng AMSTERDAM_LAT_LNG = new LatLng(52.370216, 4.895168);
    private Location lastLocation;
    private Timer locationTimeoutTimer;

    private ReentrantLock addressObtainedLock;

    private AlertDialog lastAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        callPanelWrapper = findViewById(R.id.call_panel_wrapper);
        buttonCallNow = findViewById(R.id.button_call_now);

        addressObtainedLock = new ReentrantLock();

        locationServiceIntentFilter = new IntentFilter();
        locationServiceIntentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);

        GPSSwitchStateIntentFilter = new IntentFilter();
        GPSSwitchStateIntentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
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
        registerReceiver(GPSSwitchStateReceiver, GPSSwitchStateIntentFilter);
        startLocationService();
    }

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
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if(map != null) {
            setNewLocation(latLng);
        }
    }

    private void setNewLocation(LatLng latLng) {
        if(map == null)
            return;

        hideLocationObtainingImage();

        if(marker == null) {
            marker = map.addMarker(createInitMarkerOptions());
            marker.setTitle(getString(R.string.address_obtaining));
            marker.showInfoWindow();
        }
        marker.setPosition(latLng);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM));

        new AddressObtainTask(this, this).execute(latLng);
    }

    @Override
    public void onAddressObtained(@NonNull final String address) {
        addressObtainedLock.lock();
        if(marker != null) {
            marker.setTitle(address);
            marker.showInfoWindow();
        }
        addressObtainedLock.unlock();
    }

    /** This callback is triggered when the map is ready to be used. */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));
        if(lastLocation == null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(AMSTERDAM_LAT_LNG, MAP_ZOOM));
        }
    }

    private MarkerOptions createInitMarkerOptions() {
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker));
        markerOptions.anchor(0.5f, 1.0f);
        markerOptions.position(AMSTERDAM_LAT_LNG);
        markerOptions.infoWindowAnchor(0.5f, -0.2f);
        return markerOptions;
    }

    private void hideLocationObtainingImage() {
        findViewById(R.id.location_obtaining).setVisibility(View.GONE);
    }

    private BroadcastReceiver GPSSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action == null) return;

            if(action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                checkGPSAndInternetAvailability();
            }
        }
    };

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
        unregisterReceiver(GPSSwitchStateReceiver);
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

    public void buttonCallNowClick(View view) {
        callPanelWrapper.setVisibility(View.VISIBLE);
        buttonCallNow.setVisibility(View.GONE);
    }

    public void buttonPanelCloseClick(View view) {
        callPanelWrapper.setVisibility(View.GONE);
        buttonCallNow.setVisibility(View.VISIBLE);
    }

    public void buttonDialClick(View view) {
        Utils.dialIfAvailable(this, getString(R.string.phone));
    }

    public void buttonBackClick(View view) {
        finish();
    }
}
