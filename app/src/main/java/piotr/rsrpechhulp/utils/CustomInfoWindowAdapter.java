package piotr.rsrpechhulp.utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import piotr.rsrpechhulp.R;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private LayoutInflater layoutInflater;

    public CustomInfoWindowAdapter(Activity activity) {
        this.layoutInflater = activity.getLayoutInflater();
    }

    @Override
    public View getInfoWindow(Marker marker) {
        View view = layoutInflater.inflate(R.layout.view_info_layout, null);
        ((TextView) view.findViewById(R.id.address_text)).setText(marker.getTitle());
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
