package com.example.sascha.mobaapp;

import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;

public class Settings extends AppCompatActivity {
    DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);

        //Listener setzen für den Drawer
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {

                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        //menuItem.setChecked(true);
                        // close drawer when item is tapped
                        Settings.this.mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        Log.i("MainActivity", "" + menuItem.getItemId());
                        switch(menuItem.getItemId()){
                            case R.id.nav_home:
                                Settings.this.finish();
                                break;
                            case R.id.nav_settings:
                                return true;
                        }


                        return true;
                    }
                });

        EditText temp = findViewById(R.id.serverPortSettingsValue);
        temp.setText("8080");
        temp = findViewById(R.id.streamQuality);
        temp.setText("30");
        temp = findViewById(R.id.scaleSettingValue);
        temp.setText("1234");
    }

    public void saveAll(){

    }

    public void readAll(){

    }
}
