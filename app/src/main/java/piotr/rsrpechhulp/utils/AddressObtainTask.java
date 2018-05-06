package piotr.rsrpechhulp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * An asynchronous task class used to obtain address in String form using {@link android.location.Geocoder Geocoder}
 *
 * @author Piotr Przestrzelski
 */
public class AddressObtainTask extends AsyncTask<LatLng, Void, String> {

    private final WeakReference<Context> contextWeakReference;
    private final WeakReference<Callback> callbackWeakReference;

    public AddressObtainTask(Context context, Callback callback) {
        this.contextWeakReference = new WeakReference<>(context);
        this.callbackWeakReference = new WeakReference<>(callback);
    }

    @Override
    protected String doInBackground(LatLng... latLngs) {
        if(latLngs.length == 0)
            return null;

        LatLng latLng = latLngs[0];

        try {
            final Context context = contextWeakReference.get();
            if(context == null)
                return null;

            List<Address> addresses = new Geocoder(context, Locale.getDefault()).getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (!addresses.isEmpty() ) {
                Address address = addresses.get(0);
                StringBuilder addressStr = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressStr.append(address.getAddressLine(i));
                    if (i != address.getMaxAddressLineIndex()) {
                        addressStr.append(", ");
                    }
                }
                return addressStr.toString();
            }
        } catch (IOException e) {
            return null;
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        final Callback callback = callbackWeakReference.get();
        if(result != null && callback != null)
            callback.onAddressObtained(result);
    }

    public interface Callback {
        void onAddressObtained(@NonNull String address);
    }

}
