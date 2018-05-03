package piotr.rsrpechhulp.activities;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import piotr.rsrpechhulp.R;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    private void setupActionBar() {
        // Inflate your custom layout
        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(
                R.layout.custom_actionbar,
                null);

        // Set up your ActionBar
        final ActionBar actionBar = getSupportActionBar();
        if(null == actionBar)
            return;
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(actionBarLayout);
    }
}
