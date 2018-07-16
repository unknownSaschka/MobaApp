package com.Simple_Stream.UI;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Parcelable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import com.Simple_Stream.Capturing.CaptureService;
import com.Simple_Stream.Constants;
import com.Simple_Stream.R;
import com.Simple_Stream.HTTP_Server.ServerService;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout _DrawerLayout;

    private boolean _IsHttpServerRunning_shadow = false;
    private String _HttpServerPort_shadow = "" + Constants.DEFAULT_HTTP_SERVER_PORT;
    private String _IpAddress_shadow = "";

    private LocalBroadcastManager _localBroadcaster;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleLocalBroadcast(intent);
        }
    };


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndAskForInternetPermission();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);

        initDrawer();

        initLocalBroadcaster();

        startService(new Intent(getApplicationContext(), ServerService.class));
        startService(new Intent(getApplicationContext(), CaptureService.class));


        requestIpUpdate();
        requestPortUpdate();

        initStartButton();
    }

    public void generateQR() {
        ImageView qr = findViewById(R.id.QRImage);

        QRGeneratorThread qrGenerator = new QRGeneratorThread(_IpAddress_shadow, _HttpServerPort_shadow, qr, getApplicationContext());
        qrGenerator.start();
    }

    private void startHttpServer() {
        if (_localBroadcaster != null) {
            Intent toSend = new Intent(Constants.SERVER_HTTP_EVENT_NAME_COMMAND);
            toSend.putExtra(Constants.SERVER_HTTP_COMMAND, Constants.SERVER_HTTP_START);
            _localBroadcaster.sendBroadcast(toSend);
        }
        requestIpUpdate();
    }

    private void stopHttpServer() {
        if (_localBroadcaster != null) {
            Intent toSend = new Intent(Constants.SERVER_HTTP_EVENT_NAME_COMMAND);
            toSend.putExtra(Constants.SERVER_HTTP_COMMAND, Constants.SERVER_HTTP_STOP);
            _localBroadcaster.sendBroadcast(toSend);
        }
    }

    private void initStartButton() {
        Button startButton = findViewById(R.id.buttonStart);
        startButton.setText(R.string.buttonStart);
        View.OnClickListener listenerToSet = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.this._IsHttpServerRunning_shadow) {
                    Toast.makeText(MainActivity.this, "Server wird gestoppt", Toast.LENGTH_SHORT).show();
                    MainActivity.this.stopHttpServer();
                    MainActivity.this.stopCapturing();
                    return;
                }
                Toast.makeText(MainActivity.this, "Server wird gestartet", Toast.LENGTH_SHORT).show();
                MainActivity.this.startHttpServer();
                MainActivity.this.TryToStartCaptureService();
            }
        };
        startButton.setOnClickListener(listenerToSet);
    }

    private void stopCapturing() {
        if (_localBroadcaster != null) {
            Intent toSend = new Intent(Constants.CAPTURE_EVENT_NAME_COMMAND);
            toSend.putExtra(Constants.CAPTURE_COMMAND, Constants.CAPTURE_STOP);
            _localBroadcaster.sendBroadcast(toSend);
        }
        requestIpUpdate();
    }

    public void resetImageAndQr() {
        ImageView temp = findViewById(R.id.imagePreview);
        temp.setImageBitmap(null);
        temp = findViewById(R.id.QRImage);
        temp.setImageBitmap(null);
    }

    private void updateDisplayedValuesOn() {
        TextView ipInfo = findViewById(R.id.yourIPText);
        ipInfo.setText(getString(R.string.ipInfoServerOn));

        TextView ipAddressText = findViewById(R.id.ipAddressTW);
        String URI = _IpAddress_shadow + ":" + _HttpServerPort_shadow;
        ipAddressText.setText(URI);

        Button button = findViewById(R.id.buttonStart);
        button.setText(R.string.buttonStop);
    }

    private void updateDisplayedValuesOff() {
        TextView ipInfo = findViewById(R.id.yourIPText);
        ipInfo.setText(R.string.ipInfoServerOff);

        TextView ipAddressText = findViewById(R.id.ipAddressTW);
        ipAddressText.setText(_IpAddress_shadow + "");

        Button button = findViewById(R.id.buttonStart);
        button.setText(R.string.buttonStart);

        ImageView qr = (ImageView) findViewById(R.id.QRImage);
        qr.setImageDrawable(null);
    }

    /**
     * Can only used if localBroadcastManager is active.
     */
    private void requestIpUpdate() {
        if (_localBroadcaster != null) {
            Intent temp = new Intent(Constants.IP_REQUEST);
            _localBroadcaster.sendBroadcast(temp);
        } else {
            if (Constants.InDebugging) {
                Log.i("LocalBroadcast Main", "Illegal State access");
            }
        }
    }

    private void handleIpUpdate(Intent rawIntent) {
        String addr = rawIntent.getStringExtra(Constants.IP_ANSWER_ADDRESS);
        String isRunning = rawIntent.getStringExtra(Constants.IP_ANSWER_FLAG_RUN);

        if (addr != null && isRunning != null) {
            _IpAddress_shadow = addr;
            if (isRunning == Constants.SERVER_HTTP_IS_RUNNING_TRUE) {
                _IsHttpServerRunning_shadow = true;
                updateDisplayedValuesOn();
                generateQR();
            } else if (isRunning == Constants.SERVER_HTTP_IS_RUNNING_FALSE) {
                _IsHttpServerRunning_shadow = false;
                updateDisplayedValuesOff();
                resetImageAndQr();
            }
        }
    }

    /**
     * Can only used if localBroadcastManager is active.
     */
    private void requestPortUpdate() {
        if (_localBroadcaster != null) {
            Intent temp = new Intent(Constants.IP_REQUEST);
            _localBroadcaster.sendBroadcast(temp);
        } else {
            if (Constants.InDebugging) {
                Log.i("LocalBroadcast Main", "Illegal State access");
            }
        }
    }

    private void handlePortChange(Intent rawIntent) {
        String port = rawIntent.getStringExtra(Constants.SETTING_SERVER_PORT);
        if (port != null) {
            _HttpServerPort_shadow = port;
            if (_IsHttpServerRunning_shadow) {
                updateDisplayedValuesOn();
            } else {
                updateDisplayedValuesOff();
            }
        }
    }

    private void handleImageChange(Intent rawIntent) {
        byte[] data = rawIntent.getByteArrayExtra(Constants.IMAGE_DATA_NAME);
        if (data != null) {
            if(_IsHttpServerRunning_shadow) {
                Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
                ImageView view = findViewById(R.id.imagePreview);
                view.setImageBitmap(image);
            }else{
                resetImageAndQr();
            }
        }
    }

    private void handleLocalBroadcast(Intent rawIntent) {
        String action = rawIntent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case Constants.IP_ANSWER:
                handleIpUpdate(rawIntent);
                break;
            case Constants.IMAGE_EVENT_NAME:
                handleImageChange(rawIntent);
                break;
            case Constants.QR_CODE_EVENT:
                setQRImage(rawIntent);
                break;
            case Constants.SETTING_INFO_EVENT:
                handlePortChange(rawIntent);
                break;
            default:
                return;
        }
    }

    private IntentFilter getIntentFilter() {
        IntentFilter toReturn = new IntentFilter(Constants.IMAGE_EVENT_NAME);
        toReturn.addAction(Constants.IP_ANSWER);
        toReturn.addAction(Constants.QR_CODE_EVENT);
        toReturn.addAction(Constants.SETTING_INFO_EVENT);
        return toReturn;
    }

    private synchronized void setQRImage(Intent data) {
        Parcelable p = data.getParcelableExtra(Constants.QR_CODE_DATA);
        Bitmap b = (Bitmap) p;
        ImageView imageView = findViewById(R.id.QRImage);
        imageView.setImageBitmap(b);
    }

    private void initDrawer() {
        //Listener setzen für den Drawer
        _DrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {

                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        //menuItem.setChecked(true);
                        // close drawer when item is tapped
                        _DrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        Log.i("MainActivity", "" + menuItem.getItemId());
                        switch (menuItem.getItemId()) {
                            case R.id.nav_home:
                                break;
                            case R.id.nav_settings:
                                Intent intent = new Intent(MainActivity.this, Settings.class);
                                startActivity(intent);
                                break;
                        }
                        return true;
                    }
                });
    }

    private void checkAndAskForInternetPermission() {
        //Prüfe auf Permissions für Internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            if (Constants.InDebugging) {
                Log.i("Main", "Keine Perms");
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, Constants.INTERNET_PERMISSION);
        } else {
            if (Constants.InDebugging) {
                Log.i("Main", "Hat Perms");
            }
        }
    }

    private void TryToStartCaptureService() {
        MediaProjectionManager temp = null;
        //Result checking in callback methode onActivityResult()
        try {
            if (Constants.InDebugging) {
                Log.d("BeforeServiceStart", "Now trying to get Mediamanager.");
            }
            temp = (MediaProjectionManager) getSystemService(getApplicationContext().MEDIA_PROJECTION_SERVICE);
            startActivityForResult(temp.createScreenCaptureIntent(), Constants.REQUEST_CODE_SCREEN_CAPTURE);
        } catch (Exception ex) {
            if (Constants.InDebugging) {
                Log.d("BeforeServiceStart", ex.getMessage());
            }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        initLocalBroadcaster();
    }

    @Override
    public void onStop() {
        super.onStop();
        cleanLocalBroadcaster();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                Intent toSend = new Intent(Constants.CAPTURE_EVENT_NAME_COMMAND);
                toSend.putExtra(Constants.CAPTURE_COMMAND, Constants.CAPTURE_INIT);
                toSend.putExtra(Constants.CAPTURE_MEDIA_GRANTING_TOKEN_INTENT, data);
                _localBroadcaster.sendBroadcast(toSend);
            } else {
                if (Constants.InDebugging) {
                    Log.d("BeforeServiceStarting", "Request failed.");
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                _DrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


