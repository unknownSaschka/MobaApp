package com.example.sascha.mobaapp;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class CaptureService extends Service {

    private MediaProjection.Callback ProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            super.onStop();
            stopCapturing();
        }
    };
    private MediaProjection _ScreenCapturer = null;
    private Display _ScreenToCapture = null;
    private VirtualDisplay _CapturedScreen = null;
    private ImageReader _ImageProcessor = null;
    private ImageReader.OnImageAvailableListener _NewImageListener = new OnNewImageReadyListener(this);
    private LocalBroadcastManager _localBroadcaster;

    //Image generation should run in an extra Thread.
    private HandlerThread _ImageThread = null;
    private Handler _ImageThreadHandler = null;

    private int _Orientation = Configuration.ORIENTATION_UNDEFINED;
    private int _LastOrientation = Configuration.ORIENTATION_UNDEFINED;
    private OrientationEventListener _rotationListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     *
     * @param _Intent You need to pass the capturetoken Intent as extra.
     * @param flags   -
     * @param startId -
     * @return -
     */
    @Override //Is called once per StartService call! //TODO: fix memory leak.
    public int onStartCommand(Intent _Intent, int flags, int startId) {
        Parcelable realIntent = _Intent.getParcelableExtra(Intent.EXTRA_INTENT);

        if (realIntent != null) {
            if (Debug.InDebugging) {
                Log.i("Service", "Token received.");
            }
            bootingService((Intent) realIntent);
        } else {
            if (Debug.InDebugging) {
                Log.d("Service", "Did not recieve token for Capturing.");
            }
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        return Service.START_NOT_STICKY;
    }


    @Override //Is called just once!
    public void onCreate(){
        Context tempContext = getApplicationContext();
        _localBroadcaster = LocalBroadcastManager.getInstance(tempContext);
        _rotationListener = new OrientationEventListener(tempContext) {
            @Override
            public synchronized void onOrientationChanged(int i) {
                updateRotation();
                if (hasRotationChanged()) {
                    CaptureService.this.restartCapturing();
                }
            }
        };
        _rotationListener.enable();
        if (Debug.InDebugging) {
            Log.i("Service", "Capture Service starting");
        }

    }

    private void bootingService(Intent captureTokenIntent) {
        _ScreenCapturer = ((MediaProjectionManager) getSystemService(getApplicationContext().MEDIA_PROJECTION_SERVICE)).getMediaProjection(Activity.RESULT_OK, captureTokenIntent);
        _ScreenCapturer.registerCallback(ProjectionCallback, new Handler(Looper.getMainLooper()));
        _ScreenToCapture = ((WindowManager) getSystemService(getApplicationContext().WINDOW_SERVICE)).getDefaultDisplay();
        if (Debug.InDebugging) {
            Log.i("Service", "Booted Capturing.");
        }

        _ImageThread = new HandlerThread("ImageGenerator", THREAD_PRIORITY_BACKGROUND);
        _ImageThread.start();
        _ImageThreadHandler = new Handler(_ImageThread.getLooper());
        if (Debug.InDebugging) {
            Log.i("Service", "Thread started.");
        }
        updateRotation();
        _LastOrientation = _Orientation;
        startCapturing();
    }

    /**
     * Needs to be called, if the rotation of the screen changes.
     */
    private synchronized void restartCapturing() {
        if (Debug.InDebugging) {
            Log.i("CaptureService", "Restart");
        }
        stopCapturing();
        startCapturing();
    }

    /**
     * Starts the actual processing of the captured screen.
     */
    private void startCapturing() {
        Point screenSize = new Point();
        _ScreenToCapture.getSize(screenSize);
        _ImageProcessor = ImageReader.newInstance(screenSize.x, screenSize.y, PixelFormat.RGBA_8888, 2);
        _ImageProcessor.setOnImageAvailableListener(_NewImageListener, _ImageThreadHandler);

        try {
            DisplayMetrics metric = new DisplayMetrics();
            _ScreenToCapture.getMetrics(metric);
            _CapturedScreen = _ScreenCapturer.createVirtualDisplay(
                    "SimpleStreamVirtualDisplay",
                    screenSize.x, screenSize.y,
                    metric.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                    _ImageProcessor.getSurface(), null, _ImageThreadHandler);
            if (Debug.InDebugging) {
                Log.i("Service", "Created Virtual Display.");
            }
        } catch (Exception e) {
            if (Debug.InDebugging) {
                Log.d("Service", "Could not create virtual display");
            }
        }
    }

    /**
     * Here should only the capturing be stopped
     */
    private void stopCapturing() {
        _CapturedScreen.release();
        _ImageProcessor.close();
    }

    /**
     * This should be used to stop the whole Service.
     */
    public void stopService() {
        stopCapturing();
        _ImageThread.quit();
        _rotationListener.disable();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopService();
    }

    /**
     * Workaround, because listener has no context to send intents.
     *
     * @param intentToSend -
     */
    public void sendImage(Intent intentToSend) {
        _localBroadcaster.sendBroadcast(intentToSend);
    }

    /**
     * Workaroundd, because listener should not know device screen.
     *
     * @return true if device is rotated in landscape mode.
     */
    public boolean isLandscape() {

        if (_Orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * updates Rotation.
     */
    private void updateRotation() {
        int rotation = _ScreenToCapture.getRotation();
        _LastOrientation = _Orientation;
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            _Orientation = Configuration.ORIENTATION_PORTRAIT;
        } else if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            _Orientation = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            _Orientation = Configuration.ORIENTATION_UNDEFINED;
        }
    }

    /**
     * Looks if rotation changed since last functioncall.
     *
     * @return true if screenRotation changed since last methode call.
     */
    private boolean hasRotationChanged() {
        Boolean toReturn = false;
        if (_Orientation != _LastOrientation) {
            toReturn = true;
        }
        return toReturn;
    }
}
