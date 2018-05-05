package piotr.rsrpechhulp.activities;

import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import piotr.rsrpechhulp.R;
import piotr.rsrpechhulp.utils.LocationService;
import piotr.rsrpechhulp.utils.OnRetryClickListener;
import piotr.rsrpechhulp.utils.Utils;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int GPS_PERMISSIONS_REQUEST_CODE = 200;

    private GoogleMap map;

    private LocationService locationService;
    private IntentFilter locationServiceIntentFilter;


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
        locationServiceIntentFilter.addAction(LocationService.ACTION_NO_PERMISSIONS);
        locationServiceIntentFilter.addAction(LocationService.ACTION_NO_GPS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindToLocationService();
    }

    private void bindToLocationService() {
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(locationServiceConnection);
    }

    private ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MapsActivity.this.locationService = ((LocationService.LocalBinder) iBinder).getService();
            MapsActivity.this.locationService.startListening();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    };


    @Override
    public void onResume() {
        super.onResume();
        checkGPSAndInternet();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastReceiver, locationServiceIntentFilter);
        if(locationService != null){
            locationService.startListening();
        }
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
        if(locationService != null)
            locationService.stopListening();
        super.onPause();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng sydney = new LatLng(52.370216, 4.895168);
        map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void buttonBack(View view) {
        finish();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Only checking actions from LocationService
            if(null == action)
                return;

            if (action.equals(LocationService.ACTION_LOCATION_CHANGED)) {
                final Location location = intent.getParcelableExtra(LocationService.LOCATION_INTENT_EXTRAS);
                onLocationReceived(location);
            } else if (action.equals(LocationService.ACTION_NO_GPS)) {
                MapsActivity.this.checkGPSAndInternet();
            } else if (action.equals(LocationService.ACTION_NO_PERMISSIONS)) {
                MapsActivity.this.checkGPSPermissions();
            }
        }
    };

    private void onLocationReceived(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if(map != null)
            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void checkGPSPermissions() {
        if(!Utils.checkGPSPermissions(MapsActivity.this))
            Utils.requestGPSPermissions(MapsActivity.this, GPS_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case GPS_PERMISSIONS_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.error_gps_no_permissions, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private final OnRetryClickListener onRetryClick = new OnRetryClickListener() {
        @Override
        public void onRetryClick() {
            MapsActivity.this.checkGPSAndInternet();
        }
    };

    private void checkGPSAndInternet() {
        if(!Utils.checkGPSEnable(this))
            Utils.buildAlertMessageGpsDisabled(this).show();
        else if(!Utils.checkInternetConnectivity(this)){
            Utils.buildAlertMessageNoInternet(this, onRetryClick).show();
        }
    }
}
