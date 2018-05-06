package piotr.rsrpechhulp.activities;

import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

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
import piotr.rsrpechhulp.utils.CustomInfoWindowAdapter;
import piotr.rsrpechhulp.utils.LocationService;
import piotr.rsrpechhulp.utils.OnRetryClickListener;
import piotr.rsrpechhulp.utils.Utils;

import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int GPS_PERMISSIONS_REQUEST_ON_LOCATION_SERVICE_START_CODE = 200;

    private GoogleMap map;
    private Marker marker;
    private static final float MAP_ZOOM = 16.0f;

    private LocationService locationService;
    private boolean locationServiceBound;
    private IntentFilter locationServiceIntentFilter;

    private static final float MINIMUM_LOCATION_ACCURACY = 1000.0f;
    private static final int SEARCH_LOCATION_TIMEOUT = 20000;
    private static final LatLng AMSTERDAM_LAT_LNG = new LatLng(52.370216, 4.895168);
    private Location lastLocation;
    private Timer locationTimeoutTimer;

    private AlertDialog lastAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationServiceIntentFilter = new IntentFilter();
        locationServiceIntentFilter.addAction(LocationService.ACTION_LOCATION_CHANGED);
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
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastReceiver, locationServiceIntentFilter);
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
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

    /** This callback is triggered when the map is ready to be used. */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));
        if(lastLocation == null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(AMSTERDAM_LAT_LNG));
        }
    }

    public MarkerOptions createInitMarkerOptions() {
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
    }

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
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
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

    public void buttonBack(View view) {
        finish();
    }
}
