package piotr.rsrpechhulp.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import piotr.rsrpechhulp.R;

public class Utils {

    public static final String ACTION_LOCATION_SOURCE_SETTINGS = "android.settings.LOCATION_SOURCE_SETTINGS";

    /**
     * Checks if GPS location provider is enabled
     * @param context the context to get system service from
     * @return <code>true</code> if GPS location provider is enabled;
     *         <code>false</code> otherwise
     */
    public static boolean checkGPSEnable(Context context) {
        final LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);

        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Builds {@link android.app.AlertDialog AlertDialog} with information about disabled GPS location provider.
     * It starts location source settings on positive button or runs {@link Activity#finish()} on {@link Activity} given
     * as param.
     * @param activity the activity to start settings from or finish
     * @return created AlertDialog
     */
    public static AlertDialog buildAlertMessageNoGps(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.error_gps_disabled_message)
                .setTitle(R.string.error_gps_disabled_title)
                .setCancelable(false)
                .setPositiveButton(R.string.error_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        activity.startActivity(new Intent(ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(R.string.error_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        activity.finish();
                    }
                });
        return builder.create();
    }

    /**
     * Checks if currently active network is connected or connecting to Internet
     * @param context the context to get system service from
     * @return <code>true</code> if currently active network is connected or connecting;
     *         <code>false</code> otherwise
     */
    public static boolean checkInternetConnectivity(Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }


    /**
     * Builds {@link android.app.AlertDialog AlertDialog} with information about not available internet connectivity.
     * It starts location source settings on positive button or runs {@link Activity#finish()} on {@link Activity} given
     * as param.
     * @param activity the activity to start settings from or finish
     * @return created AlertDialog
     */
    public static AlertDialog buildAlertMessageNoInternet(final Activity activity, final DialogInterface.OnClickListener onRetryListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.error_network_message)
                .setTitle(R.string.error_network_title)
                .setCancelable(false)
                .setPositiveButton(R.string.error_retry, onRetryListener)
                .setNegativeButton(R.string.error_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        activity.finish();
                    }
                });
        return builder.create();
    }
}