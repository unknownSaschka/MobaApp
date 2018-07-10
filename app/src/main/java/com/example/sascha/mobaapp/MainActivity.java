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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;

//TODO Besser auf IP-Addressen vergleichen

public class MainActivity extends AppCompatActivity {

    static final int INET_PERMS = 1;
    static final int HttpServerPort = 8080;
    ServerSocket httpSocket = null;
    private boolean httpServerActive = false;
    ServerThread thread;
    String ipAddress;
    ImageSendService webSocketServer;
    InetSocketAddress socketAddress;

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

        //Sachen für Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView ipInfo = findViewById(R.id.yourIPText);
        ipInfo.setText(R.string.ipInfoServerOff);
        TextView ipText = (TextView) findViewById(R.id.ipAddressTW);
        ipText.setText(getIpAddr());

        Button startButton = (Button) findViewById(R.id.buttonStart);
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

        generateQR(URI);


        Bitmap test = BitmapFactory.decodeResource(getResources(), R.raw.animetest);

        if(socketAddress != null){
            webSocketServer = new ImageSendService(socketAddress, test);
            webSocketServer.start();
        }
        else {
            System.out.println("Fehler bei activeInetAddress");
        }


    }

    public void startStream(){
        //ServerStream stream = new ServerStream();
        //stream.start();
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
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
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
                Context blarg = getApplicationContext();
                startService(new Intent(blarg, CaptureService.class).putExtra(Intent.EXTRA_INTENT, data));
            }
            else{
                if(Debug.InDebugging) {
                    Log.d("BeforeServiceStarting", "Request failed.");
                }
            }
        }
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


