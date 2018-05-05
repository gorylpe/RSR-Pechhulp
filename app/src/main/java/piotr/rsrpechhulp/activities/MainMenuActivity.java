package piotr.rsrpechhulp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import piotr.rsrpechhulp.R;
import piotr.rsrpechhulp.utils.Utils;

public class MainMenuActivity extends AppCompatActivity {

    private final DialogInterface.OnClickListener onRetryCheckClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            MainMenuActivity.this.checkGPSAndInternet();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    private void checkGPSAndInternet() {
        if(!Utils.checkGPSEnable(this))
            Utils.buildAlertMessageNoGps(this).show();
        else if(!Utils.checkInternetConnectivity(this)){
            Utils.buildAlertMessageNoInternet(this, onRetryCheckClick).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        checkGPSAndInternet();
    }

    public void buttonInfoClick(View view) {
        startActivity(new Intent(this, InfoActivity.class));
    }

    public void buttonMapClick(View view) {
        startActivity(new Intent(this, MapActivity.class));
    }
}
