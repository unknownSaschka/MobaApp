package com.Simple_Stream.Settings;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.Simple_Stream.Constants;

public class SettingsService extends Service{

    private int _httpPort;
    private int _videoQuality;
    private float _videoScaling;
    private XMLUtils _settingsXML;
    private LocalBroadcastManager _localBroadcaster;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleLocalBroadcast(intent);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override //Is called once per StartService call!
    public int onStartCommand(Intent _Intent, int flags, int startId) {
        _settingsXML = new XMLUtils(getApplicationContext());
        initSettingsValues();
        initLocalBroadcaster();
        sendSettings();
        return Service.START_NOT_STICKY;
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
            case Constants.SETTING_REQUEST:
                sendSettings();
                break;
            case Constants.SETTING_CHANGED_EVENT:
                saveNewSettings(rawIntent);
                break;
            default:
                return;
        }
    }

    private void sendSettings(){
        Intent toSend = new Intent(Constants.SETTING_INFO_EVENT);
        toSend.putExtra(Constants.SETTING_SERVER_PORT, _httpPort);
        toSend.putExtra(Constants.SETTING_JPEG_QUALI, _videoQuality);
        toSend.putExtra(Constants.SETTING_SCALE, _videoScaling);
        Log.i("SettingsService", "Sende Settings raus: " + _httpPort + " " + _videoQuality + " " + _videoScaling);
        _localBroadcaster.sendBroadcast(toSend);
    }

    private IntentFilter getIntentFilter() {
        IntentFilter toReturn = new IntentFilter(Constants.SETTING_REQUEST);
        toReturn.addAction(Constants.SETTING_CHANGED_EVENT);
        return toReturn;
    }

    private void initSettingsValues(){
        String[] settingValues = _settingsXML.getXMLSettings();
        _httpPort = Integer.parseInt(settingValues[0]);
        _videoQuality = Integer.parseInt(settingValues[1]);
        _videoScaling = Float.parseFloat(settingValues[2]);
    }

    private synchronized void saveNewSettings(Intent intent){
        _httpPort = intent.getIntExtra(Constants.SETTING_SERVER_PORT, -1);
        _videoQuality = intent.getIntExtra(Constants.SETTING_JPEG_QUALI, -1);
        _videoScaling = intent.getFloatExtra(Constants.SETTING_SCALE, -1);
        if(Constants.InDebugging){
            Log.i("SettingsService", "Speichere neue Settings: " + _httpPort + " " + _videoQuality + " " + _videoScaling);
        }
        _settingsXML.createXML(String.valueOf(_httpPort), String.valueOf(_videoQuality), String.valueOf(_videoScaling));
    }

    @Override
    public void onDestroy(){
        cleanLocalBroadcaster();
    }
}
