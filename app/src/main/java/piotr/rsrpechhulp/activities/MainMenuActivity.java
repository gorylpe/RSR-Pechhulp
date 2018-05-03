package piotr.rsrpechhulp.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import piotr.rsrpechhulp.R;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }


    public void buttonInfoClick(View view) {
        startActivity(new Intent(this, InfoActivity.class));
    }
}
