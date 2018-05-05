package piotr.rsrpechhulp.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import piotr.rsrpechhulp.R;
import piotr.rsrpechhulp.utils.OnRetryClickListener;
import piotr.rsrpechhulp.utils.Utils;

public class MainMenuActivity extends AppCompatActivity {

    private final OnRetryClickListener onRetryClick = new OnRetryClickListener() {
        @Override
        public void onRetryClick() {
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
            Utils.buildAlertMessageGpsDisabled(this).show();
        else if(!Utils.checkInternetConnectivity(this)){
            Utils.buildAlertMessageNoInternet(this, onRetryClick).show();
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
        startActivity(new Intent(this, MapsActivity.class));
    }
}
