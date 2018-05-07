package piotr.rsrpechhulp.utils;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import piotr.rsrpechhulp.R;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private Activity activity;

    public CustomInfoWindowAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        View view = activity.getLayoutInflater().inflate(R.layout.view_info_layout, null);
        ((TextView) view.findViewById(R.id.address_text)).setText(marker.getTitle());
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}