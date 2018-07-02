package com.example.sascha.mobaapp;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Sascha on 01.06.2018.
 */

public class StartStopButtonListener implements View.OnClickListener{
    private MainActivity _activity;

    public StartStopButtonListener(MainActivity activity){
        _activity = activity;
    }

    @Override
    public void onClick(View view) {
        Button serverButton = _activity.findViewById(R.id.buttonStart);
        if(_activity.getHTTPServerActive()) {
            Toast.makeText(_activity, "Server wird gestoppt", Toast.LENGTH_SHORT).show();
            _activity.stopServer();
            return;
        }
        Toast.makeText(_activity, "Server wird gestartet", Toast.LENGTH_SHORT).show();
        _activity.startServer();

        _activity.TryToStartCaptureService();
    }
}
