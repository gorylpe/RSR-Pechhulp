package piotr.rsrpechhulp.utils;

import android.app.Activity;
import android.support.annotation.NonNull;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import piotr.rsrpechhulp.R;

import java.util.concurrent.locks.ReentrantLock;

public class MapManager implements OnMapReadyCallback, AddressObtainTask.Callback {

    private static final float MAP_ZOOM = 16.0f;
    private static final LatLng AMSTERDAM_LAT_LNG = new LatLng(52.370216, 4.895168);

    private Activity activity;

    private GoogleMap map;
    private Marker marker;

    private ReentrantLock addressObtainedLock;

    public MapManager(Activity activity) {
        this.activity = activity;
        addressObtainedLock = new ReentrantLock();
    }

    /** This callback is triggered when the map is ready to be used. */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setInfoWindowAdapter(new CustomInfoWindowAdapter(activity));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(AMSTERDAM_LAT_LNG, MAP_ZOOM));
    }

    public void setLocation(LatLng latLng) {
        if(map == null)
            return;

        if(marker == null) {
            marker = map.addMarker(createInitMarkerOptions());
            marker.setTitle(activity.getString(R.string.address_obtaining));
            marker.showInfoWindow();
        }
        marker.setPosition(latLng);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM));

        new AddressObtainTask(activity, this).execute(latLng);
    }

    private MarkerOptions createInitMarkerOptions() {
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker));
        markerOptions.anchor(0.5f, 1.0f);
        markerOptions.position(AMSTERDAM_LAT_LNG);
        markerOptions.infoWindowAnchor(0.5f, -0.2f);
        return markerOptions;
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
}
