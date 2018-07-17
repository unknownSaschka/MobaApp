package com.Simple_Stream.UI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.Simple_Stream.Constants;
import com.Simple_Stream.R;

public class Settings extends AppCompatActivity {
    DrawerLayout mDrawerLayout;

    private int oldHttpPort;
    private int oldVideoQuality;
    private float oldVideoScale;
    private LocalBroadcastManager _localBroadcaster;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleLocalBroadcast(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Constants.InDebugging){
            Log.i("SettingsActivity", "onCreate");
        }

        setContentView(R.layout.activity_settings);
        initLocalBroadcaster();
        _localBroadcaster.sendBroadcast(new Intent(Constants.SETTING_REQUEST));

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
        //temp.setText("8080");
        temp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b){
                    if(checkHTTPInput()){
                        saveAll();
                    }
                }
            }
        });
        temp = findViewById(R.id.streamQuality);
        //temp.setText("70");
        temp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b){
                    if(checkVideoQualityInput()){
                        saveAll();
                    }
                }
            }
        });
        temp = findViewById(R.id.scaleSettingValue);
        //temp.setText("50");
        temp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b){
                    if(checkVideoScaleInput()){
                        saveAll();
                    }
                }
            }
        });
    }

    public void saveAll(){
        Intent toSend = new Intent(Constants.SETTING_CHANGED_EVENT);
        toSend.putExtra(Constants.SETTING_SERVER_PORT, oldHttpPort);
        toSend.putExtra(Constants.SETTING_JPEG_QUALI, oldVideoQuality);
        toSend.putExtra(Constants.SETTING_SCALE, oldVideoScale);
        _localBroadcaster.sendBroadcast(toSend);
    }

    public void readAll(Intent intent){
        EditText temp = findViewById(R.id.serverPortSettingsValue);
        //oldHttpPort = Integer.parseInt(intent.getStringExtra(Constants.SETTING_SERVER_PORT));
        oldHttpPort = intent.getIntExtra(Constants.SETTING_SERVER_PORT, -1);
        //temp.setText(intent.getStringExtra(Constants.SETTING_SERVER_PORT));
        temp.setText(String.valueOf(intent.getIntExtra(Constants.SETTING_SERVER_PORT, -1)));
        temp = findViewById(R.id.streamQuality);
        //oldVideoQuality = Integer.parseInt(intent.getStringExtra(Constants.SETTING_JPEG_QUALI));
        oldVideoQuality = intent.getIntExtra(Constants.SETTING_JPEG_QUALI, -1);
        //temp.setText(Constants.SETTING_JPEG_QUALI);
        temp.setText(String.valueOf(intent.getIntExtra(Constants.SETTING_JPEG_QUALI, -1)));
        temp = findViewById(R.id.scaleSettingValue);
        //oldVideoScale = Integer.parseInt(intent.getStringExtra(Constants.SETTING_SCALE));
        oldVideoScale = intent.getFloatExtra(Constants.SETTING_SCALE, -1);
        //temp.setText(Constants.SETTING_SCALE);
        temp.setText(String.valueOf(intent.getFloatExtra(Constants.SETTING_SCALE, -1)));
    }

    private void initLocalBroadcaster() {
        if (_localBroadcaster == null) {
            _localBroadcaster = LocalBroadcastManager.getInstance(getApplicationContext());
            _localBroadcaster.registerReceiver(_localListener, getIntentFilter());
        }
    }

    private void cleanLocalBroadcaster() {
        if (_localBroadcaster != null) {
            _localBroadcaster.unregisterReceiver(_localListener);
            _localBroadcaster = null;
        }
    }

    private void handleLocalBroadcast(Intent rawIntent) {
        String action = rawIntent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case Constants.SETTING_INFO_EVENT:
                readAll(rawIntent);
                break;
            default:
                return;
        }
    }

    private IntentFilter getIntentFilter() {
        IntentFilter toReturn = new IntentFilter(Constants.SETTING_INFO_EVENT);
        return toReturn;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(checkHTTPInput() || checkVideoQualityInput() || checkVideoScaleInput()){
            saveAll();
        }
        cleanLocalBroadcaster();
    }

    private boolean checkHTTPInput(){
        if(Constants.InDebugging){
            Log.i("Settings", "ServerPortFocusChange");
        }
        EditText ETHttpPort = findViewById(R.id.serverPortSettingsValue);
        String input = ETHttpPort.getText().toString();
        Integer httpPort = null;
        try {
            httpPort = Integer.parseInt(input);
        } catch (NumberFormatException e){
            httpPort = 8080;
            ETHttpPort.setText("8080");
        }
        if(httpPort < 1024 || httpPort > 65535){
            httpPort = 8080;
            ETHttpPort.setText("8080");
        }

        if(oldHttpPort == httpPort) return false;
        else oldHttpPort = httpPort;
        return true;
    }

    private boolean checkVideoQualityInput(){
        if(Constants.InDebugging){
            Log.i("Settings", "QualityFocusChange");
        }
        EditText ETVidQuality = findViewById(R.id.streamQuality);
        String input = ETVidQuality.getText().toString();
        Integer videoQuality = null;
        try {
            videoQuality = Integer.parseInt(input);
        } catch (NumberFormatException e){
            videoQuality = 50;
        }
        if(videoQuality < 0){
            videoQuality = 0;
            ETVidQuality.setText("0");
        }

        if(videoQuality > 100){
            videoQuality = 100;
            ETVidQuality.setText("100");
        }
        if(videoQuality == oldVideoQuality) return false;
        else oldVideoQuality = videoQuality;
        return true;
    }

    private boolean checkVideoScaleInput(){
        if(Constants.InDebugging){
            Log.i("Settings", "ScaleFocusChange");
        }
        EditText ETVidScale = findViewById(R.id.scaleSettingValue);
        String input = ETVidScale.getText().toString();
        Float videoScale = null;
        try {
            videoScale = Float.parseFloat(input);
        } catch (NumberFormatException e){
            videoScale = 1f;
        }
        if(videoScale < 0.5f){
            videoScale = 0.5f;
            ETVidScale.setText("0.5");
        }

        if(videoScale > 1.5f){
            videoScale = 1.5f;
            ETVidScale.setText("1.5");
        }
        if(videoScale == oldVideoScale) return false;
        else oldVideoScale = videoScale;
        return true;
    }
}
