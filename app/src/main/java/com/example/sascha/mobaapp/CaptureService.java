package com.example.sascha.mobaapp;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

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
    private ImageReader.OnImageAvailableListener _NewImageListener = null;
    private LocalBroadcastManager _localBroadcaster = null;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleLocalBroadcast(intent);
        }
    };

    private int _JPEGQuality_shadow = Constants.DEFAULT_JPEG_QUALI;
    private float _ScalingFactor_shadow = Constants.DEFAULT_SCALING_FACTOR;

    //Image generation should run in an extra Thread.
    private HandlerThread _ImageThread = null;
    private Handler _ImageThreadHandler = null;

    private int _Orientation = Configuration.ORIENTATION_UNDEFINED;
    private int _LastOrientation = Configuration.ORIENTATION_UNDEFINED;
    private OrientationEventListener _rotationListener;

    private boolean isInitialized = false;
    private boolean isCapturing = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override //Is called once per StartService call!
    public int onStartCommand(Intent _Intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }


    @Override //Is called just once!
    public void onCreate() {
        initLocalBroadcaster();
    }

    @Override
    public void onDestroy() {
        cleanLocalBroadcaster();
        cleanService();
    }

    /**
     * @param captureTokenIntent needs to get the granting Token Intent.
     */
    private synchronized void initService(Intent captureTokenIntent) {
        isInitialized = true;
        Context tempContext = getApplicationContext();
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

        _ScreenCapturer = ((MediaProjectionManager) getSystemService(getApplicationContext().MEDIA_PROJECTION_SERVICE)).getMediaProjection(Activity.RESULT_OK, captureTokenIntent);
        _ScreenCapturer.registerCallback(ProjectionCallback, new Handler(Looper.getMainLooper()));
        _ScreenToCapture = ((WindowManager) getSystemService(getApplicationContext().WINDOW_SERVICE)).getDefaultDisplay();
        if (Debug.InDebugging) {
            Log.i("Service", "Booted Capturing.");
        }

        _ImageThread = new HandlerThread("ImageGenerator");
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
        synchronized (this) {
            if (Debug.InDebugging) {
                Log.i("CaptureService", "Restart");
            }
            stopCapturing();
            startCapturing();
        }
    }

    /**
     * Starts the actual processing of the captured screen.
     */
    private synchronized void startCapturing() {
        synchronized (this) {
            if(isCapturing){
                return;
            }
            isCapturing = true;
            Point screenSize = new Point();
            _ScreenToCapture.getSize(screenSize);
            _ImageProcessor = ImageReader.newInstance(screenSize.x, screenSize.y, PixelFormat.RGBA_8888, 2);
            _NewImageListener = new OnNewImageReadyListener(this);
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
    }

    /**
     * Here should only the capturing be stopped
     */
    private synchronized void stopCapturing() {
        synchronized (this) {
            if(!isCapturing){
                return;
            }
            isCapturing = false;
            _NewImageListener = null;
            _CapturedScreen.release();
            _ImageProcessor.close();
        }
    }

    /**
     * This should be used to cleanup the Service.
     */
    private synchronized void cleanService() {
        synchronized (this) {
            stopCapturing();
            _ImageThread.quit();
            _rotationListener.disable();
            isInitialized = false;
            stopSelf();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        if (Debug.InDebugging) {
            Log.i("CaptureService", "Application closed. Stopping myself");
        }
        cleanService();
    }

    /**
     * Workaround, because listener has no context to send intents.
     *
     * @param intentToSend -
     */
    public void sendImage(Intent intentToSend) {
        if (_localBroadcaster != null) {
            _localBroadcaster.sendBroadcast(intentToSend);
        }
    }

    public int get_JPEGQuality_shadow(){
        return  _JPEGQuality_shadow;
    }

    public float get_ScalingFactor_shadow(){
        return _ScalingFactor_shadow;
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
     * Looks if rotation changed since last function call.
     *
     * @return true if screenRotation changed since last method call.
     */
    private boolean hasRotationChanged() {
        Boolean toReturn = false;
        if (_Orientation != _LastOrientation) {
            toReturn = true;
        }
        return toReturn;
    }

    private void handleCommand(Intent rawIntent) {
        String command = rawIntent.getStringExtra(Constants.CAPTURE_COMMAND);
        if (command == Constants.CAPTURE_INIT) {
            if (!isInitialized) {
                Intent grantingToken = rawIntent.getParcelableExtra(Constants.CAPTURE_MEDIA_GRANTING_TOKEN_INTENT);
                if (grantingToken != null) {
                    initService(grantingToken);
                }
            } else {
                startCapturing();
            }
        }else if(command == Constants.CAPTURE_START){
            startCapturing();
        } else if (command == Constants.CAPTURE_STOP) {
            stopCapturing();
        }
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

    private IntentFilter getIntentFilter() {
        IntentFilter toReturn = new IntentFilter(Constants.CAPTURE_EVENT_NAME_COMMAND);
        return toReturn;
    }

    private void handleLocalBroadcast(Intent rawIntent) {
        String action = rawIntent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case Constants.CAPTURE_EVENT_NAME_COMMAND:
                handleCommand(rawIntent);
                break;
            default:
                return;
        }
    }
}
