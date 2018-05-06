package piotr.rsrpechhulp.activities;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import piotr.rsrpechhulp.R;
import piotr.rsrpechhulp.utils.OnRetryClickListener;
import piotr.rsrpechhulp.utils.Utils;

public class MainMenuActivity extends AppCompatActivity {

    private AlertDialog lastAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkGPSAndInternetAvailability();
    }

    private void checkGPSAndInternetAvailability() {
        //Don't check if previous dialog is still opened
        if (lastAlertDialog != null && lastAlertDialog.isShowing())
            return;

        if(!Utils.checkGPSEnabled(this))
            (lastAlertDialog = Utils.buildAlertMessageGpsDisabled(this)).show();
        else if(!Utils.checkInternetConnectivity(this)){
            (lastAlertDialog = Utils.buildAlertMessageNoInternet(this, onRetryClick)).show();
        }
    }

    private final OnRetryClickListener onRetryClick = new OnRetryClickListener() {
        @Override
        public void onRetryClick() {
            if(lastAlertDialog != null) lastAlertDialog.dismiss();
            checkGPSAndInternetAvailability();
        }
    };

    public void buttonInfoClick(View view) {
        startActivity(new Intent(this, InfoActivity.class));
    }

    public void buttonMapClick(View view) {
        startActivity(new Intent(this, MapsActivity.class));
    }
}
