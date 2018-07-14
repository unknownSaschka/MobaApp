package com.example.sascha.mobaapp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Enumeration;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    static final int INET_PERMS = 1;
    static final int HttpServerPort = 8080;
    private ServerSocket httpSocket = null;
    private boolean httpServerActive = false;
    private ServerThread thread;
    private String ipAddress;
    private ImageSendService webSocketServer;
    private InetSocketAddress socketAddress;
    private DrawerLayout mDrawerLayout;


    public CaptureService _CaptureService = null;
    public static final int REQUEST_CODE_SCREEN_CAPTURE = 69;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Prüfe auf Permissions für Internet
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            if(Debug.InDebugging) {
                Log.i("Main", "Keine Perms");
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, INET_PERMS);
        } else {
            if(Debug.InDebugging) {
                Log.i("Main", "Hat Perms");
            }
        }

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
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        Log.i("MainActivity", "" + menuItem.getItemId());
                        switch(menuItem.getItemId()){
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

        TextView ipInfo = findViewById(R.id.yourIPText);
        ipInfo.setText(R.string.ipInfoServerOff);
        TextView ipText = findViewById(R.id.ipAddressTW);
        ipText.setText(getIpAddr());

        Button startButton = findViewById(R.id.buttonStart);
        startButton.setText(R.string.buttonStart);
        startButton.setOnClickListener(new StartStopButtonListener(this));
    }

    public void startServer(){
        httpSocket = null;
        try {
            httpSocket = new ServerSocket(HttpServerPort);
        } catch (Exception e){
            if(Debug.InDebugging){
                Log.d("HttpServer", "Could not Start.");
            }
        }
        if(httpSocket == null) return;



        //Abändern der angezeigten Daten
        TextView ipInfo = findViewById(R.id.yourIPText);
        //ipInfo.setText("Die IP-Adresse deines Gerätes:");
        ipInfo.setText(getString(R.string.ipInfoServerOn));
        TextView ipAddressText = findViewById(R.id.ipAddressTW);
        String URI = ipAddress + ":" + HttpServerPort;
        ipAddressText.setText(ipAddress + ":" + HttpServerPort);
        Button button = findViewById(R.id.buttonStart);
        button.setText(R.string.buttonStop);

        String html = indexToHTML(ipAddress);

        thread = new ServerThread(httpSocket, html);
        thread.start();
        httpServerActive = true;

        //generateQR(URI);


        Bitmap test = BitmapFactory.decodeResource(getResources(), R.raw.animetest);

        if(socketAddress != null){
            webSocketServer = new ImageSendService(socketAddress, test, getApplicationContext());
            webSocketServer.start();
        }
        else {
            System.out.println("Fehler bei activeInetAddress");
        }


    }

    public void stopServer(){
        //thread.interrupt();
        try {
            httpSocket.close();
            //httpSocket = new ServerSocket(HttpServerPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        httpServerActive = false;
        TextView ipInfo = findViewById(R.id.yourIPText);
        //ipInfo.setText("Deine IP-Adresse:");
        ipInfo.setText(R.string.ipInfoServerOff);
        TextView ipAddressText = findViewById(R.id.ipAddressTW);
        ipAddressText.setText(ipAddress + "");
        Button button = findViewById(R.id.buttonStart);
        button.setText(R.string.buttonStart);

        ImageView qr = (ImageView) findViewById(R.id.QRImage);
        qr.setImageDrawable(null);

        try {
            System.out.println("Server stoppen");
            webSocketServer.stop();
            webSocketServer = null;
        } catch (IOException e) {

        } catch (InterruptedException e) {

        }
        WebSocketConnectionManager.clear();

    }

    public String getIpAddr() {
        ipAddress = String.valueOf(R.string.noIpFound);
        Enumeration<NetworkInterface> interfacesEnum = null;
        Enumeration<InetAddress> inetAddressesEnum = null;
        NetworkInterface networkIface;
        InetAddress inetAddress;

        try {
            interfacesEnum = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if(interfacesEnum == null){
            ipAddress = String.valueOf(R.string.noInterface);
            socketAddress = null;
            return ipAddress;
        }

        while(interfacesEnum.hasMoreElements()){
            networkIface = interfacesEnum.nextElement();
            inetAddressesEnum = networkIface.getInetAddresses();

            while(inetAddressesEnum.hasMoreElements()){
                inetAddress = inetAddressesEnum.nextElement();

                if(!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address){
                    ipAddress = inetAddress.getHostAddress();
                    socketAddress = new InetSocketAddress(inetAddress, 8887);
                } else {
                    continue;
                }
            }
        }
        return ipAddress;
    }

    public String indexToHTML(String ipAddress){
        //Umwandeln von HTML zu String
        InputStream databaseInputStream = getResources().openRawResource(R.raw.index);
        BufferedInputStream br = new BufferedInputStream(databaseInputStream);
        byte[] contents = new byte[1];
        int bytesRead = 0;
        String strFileContents;
        String html = "";
        try {
            while ((bytesRead = br.read(contents)) != -1) {
                strFileContents = new String(contents, 0, bytesRead);
                //System.out.println("Auf html parsen: " + strFileContents);
                if(strFileContents.equals("%")){
                    //System.out.println("Erstes %");
                    if((bytesRead = br.read(contents)) != -1){
                        strFileContents = new String(contents, 0, bytesRead);
                        if(strFileContents.equals("%")){
                            System.out.println("Ersetzen durch ip adresse");
                            strFileContents = ipAddress + ":8887";
                        } else{
                            strFileContents = "%" + strFileContents;
                        }
                    } else {
                        strFileContents = "%";
                    }
                }
                html += strFileContents;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(html);
        return html;
    }

    public void generateQR(String ip){
        ImageView qr = findViewById(R.id.QRImage);
        int width = qr.getWidth();
        int height = qr.getHeight();

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = qrCodeWriter.encode("http://" + ip, BarcodeFormat.QR_CODE, width, height);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        if(bitMatrix == null) return;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK: Color.WHITE);
            }
        }
        qr.setImageBitmap(bitmap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        return true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(httpSocket != null){
            try {
                httpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_CODE_SCREEN_CAPTURE){
            if(resultCode == Activity.RESULT_OK){
                startService(new Intent(getApplicationContext(), CaptureService.class).putExtra(Intent.EXTRA_INTENT, data));
            }
            else{
                if(Debug.InDebugging) {
                    Log.d("BeforeServiceStarting", "Request failed.");
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public boolean getHTTPServerActive(){
        return httpServerActive;
    }

    public void TryToStartCaptureService(){
        MediaProjectionManager temp = null;
        //Result checking in callback methode onActivityResult()
        try {
            if(Debug.InDebugging) {
                Log.d("BeforeServiceStart", "Now trying to get Mediamanager.");
            }
            temp = (MediaProjectionManager) getSystemService(getApplicationContext().MEDIA_PROJECTION_SERVICE);
            startActivityForResult(temp.createScreenCaptureIntent(), MainActivity.REQUEST_CODE_SCREEN_CAPTURE);
        } catch (Exception ex) {
            if(Debug.InDebugging) {
                Log.d("BeforeServiceStart", ex.getMessage());
            }
        }
    }
}


