package piotr.rsrpechhulp.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import piotr.rsrpechhulp.R;

public class SplashActivity extends AppCompatActivity {

    private final int secondsDelayed = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //run main menu after secondsDelayed time
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SplashActivity.this.startMainMenu();
            }
        }, secondsDelayed * 1000);
    }

    private void startMainMenu() {
        startActivity(new Intent(SplashActivity.this, MainMenuActivity.class));
        finish();
    }
}
