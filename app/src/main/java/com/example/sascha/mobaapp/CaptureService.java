package com.example.sascha.mobaapp;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.content.Intent;
import android.util.Log;

public class CaptureService extends Service {

    private MediaProjection Projection = null;
    private MediaProjection.Callback ProjectionCallback = new MediaProjection.Callback(){
        @Override
        public void onStop() {
            super.onStop();
            stopStream();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //Here is the Beginning of this Serviceobject.
    public int onStartCommand(Intent _Intent, int flags, int startId){
        Log.i("Service", "Capture Service starting");
        Intent realIntent = _Intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if(realIntent != null){
            Projection = ((MediaProjectionManager) getSystemService(getApplicationContext().MEDIA_PROJECTION_SERVICE)).getMediaProjection(Activity.RESULT_OK, realIntent);
            Projection.registerCallback(ProjectionCallback, new Handler(Looper.getMainLooper()));
        }
        else{
            Log.d("Service", "Did not recieve token for Capturing.");
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        return Service.START_NOT_STICKY;
    }

    private void stopStream(){
        //hier sollte der Stream gestoppt werden.
        stopSelf();
    }
}
