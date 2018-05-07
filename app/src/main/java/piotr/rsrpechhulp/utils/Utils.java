package piotr.rsrpechhulp.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import piotr.rsrpechhulp.R;

public class Utils {

    /**
     * Checks if GPS location provider is enabled
     * @param context the context to get system service from
     * @return <code>true</code> if GPS location provider is enabled;
     *         <code>false</code> otherwise
     */
    public static boolean checkGPSEnabled(Context context) {
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
    public static AlertDialog buildAlertMessageGpsDisabled(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.error_gps_disabled_message)
                .setTitle(R.string.error_gps_disabled_title)
                .setCancelable(false)
                .setPositiveButton(R.string.error_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
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
     * It run method onRetryClick on given listener on positive button
     * or runs {@link Activity#finish()} on {@link Activity} give as param.
     * @param activity the activity to build dialog on or finish
     * @return created AlertDialog
     */
    public static AlertDialog buildAlertMessageNoInternet(final Activity activity, final OnRetryClickListener onRetryClickListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.error_network_message)
                .setTitle(R.string.error_network_title)
                .setCancelable(false)
                .setPositiveButton(R.string.error_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onRetryClickListener.onRetryClick();
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
     * Builds {@link android.app.AlertDialog AlertDialog} with information about problems with location searching.
     * It run method onRetryClick on given listener on positive button
     * or runs {@link Activity#finish()} on {@link Activity} give as param.
     * @param activity the activity to build dialog on or finish
     * @return created AlertDialog
     */
    public static AlertDialog buildAlertMessageBadLocation(final Activity activity, final OnRetryClickListener onRetryClickListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.error_location_text)
                .setTitle(R.string.error_location_title)
                .setCancelable(false)
                .setPositiveButton(R.string.error_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onRetryClickListener.onRetryClick();
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

    public static boolean checkGPSPermissions(Context context) {
        return Utils.checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean checkPermission(final Context context, final String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestGPSPermissions(Activity activity, final int requestCode) {
        requestPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION, requestCode);
    }

    public static void requestPermission(final Activity activity, final String permission, final int requestCode) {
        requestPermissions(activity, new String[]{permission}, requestCode);
    }

    public static void requestPermissions(final Activity activity, final String[] permissions, final int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * Helper method used to starts dialer if available with given phone number
     * @param context the context to start dialer activity from
     * @param phoneNumber the number to dial
     */
    public static void dialIfAvailable(Context context, String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        //check if exists activity that can be started with dialIntent
        if (context.getPackageManager().queryIntentActivities(dialIntent, 0).size() > 0) {
            context.startActivity(dialIntent);
        }
    }
}
