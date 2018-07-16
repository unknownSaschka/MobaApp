package com.example.sascha.mobaapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;

public class ServerService extends Service {
    private LocalBroadcastManager _localBroadcaster;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleLocalBroadcast(intent);
        }
    };

    private String _ipAddress;
    private InetSocketAddress _socketAddress;
    private boolean _httpServerActive = false;
    private ServerSocket _httpSocket = null;
    private ServerThread _serverThread;
    private ImageSendService _webSocketServer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        _ipAddress = getIpAddr();
        initLocalBroadcaster();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        if (Debug.InDebugging) {
            Log.i("ServerService", "Application closed. Stop all Services");
        }
        stopServer();

        stopSelf();
    }

    public String indexToHTML(String ipAddress) {
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
                if (strFileContents.equals("%")) {
                    if ((bytesRead = br.read(contents)) != -1) {
                        strFileContents = new String(contents, 0, bytesRead);
                        if (strFileContents.equals("%")) {
                            strFileContents = ipAddress + ":8887";
                        } else {
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
        return html;
    }

    public String getIpAddr() {
        _ipAddress = String.valueOf(R.string.noIpFound);
        Enumeration<NetworkInterface> interfacesEnum = null;
        Enumeration<InetAddress> inetAddressesEnum = null;
        NetworkInterface networkIface;
        InetAddress inetAddress;

        try {
            interfacesEnum = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if (interfacesEnum == null) {
            _ipAddress = String.valueOf(R.string.noInterface);
            _socketAddress = null;
            return _ipAddress;
        }

        while (interfacesEnum.hasMoreElements()) {
            networkIface = interfacesEnum.nextElement();
            inetAddressesEnum = networkIface.getInetAddresses();

            while (inetAddressesEnum.hasMoreElements()) {
                inetAddress = inetAddressesEnum.nextElement();

                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    _ipAddress = inetAddress.getHostAddress();
                    _socketAddress = new InetSocketAddress(inetAddress, 8887);
                } else {
                    continue;
                }
            }
        }
        return _ipAddress;
    }

    public synchronized void startServer() {
        if(_httpServerActive){
            return;
        }

        _ipAddress = getIpAddr();
        _httpSocket = null;
        try {
            _httpSocket = new ServerSocket(Constants.HTTP_SERVER_PORT);
        } catch (Exception e) {
            if (Debug.InDebugging) {
                Log.d("HttpServer", "Could not Start.");
            }
        }
        if (_httpSocket == null) return;

        String html = indexToHTML(_ipAddress);

        _serverThread = new ServerThread(_httpSocket, html);
        _serverThread.setName("Http_Server");
        _serverThread.start();
        _httpServerActive = true;

        //generateQR(URI);

        if (_socketAddress != null) {
            _webSocketServer = new ImageSendService(_socketAddress, getApplicationContext());
            _webSocketServer.start();
        } else {
            if (Debug.InDebugging) {
                Log.e("Get IPadr", "Error by activeInetAddress");
            }
        }
    }

    public synchronized void stopServer() {
        if(!_httpServerActive){
            return;
        }

        //_serverThread.interrupt();
        try {
            _httpSocket.close();
            //_httpSocket = new ServerSocket(HTTP_SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        _httpServerActive = false;

        try {
            if(Debug.InDebugging){
                Log.i("ServerService", "Server stoppen");
            }
            _webSocketServer.stop();
            _webSocketServer = null;
        } catch (IOException e) {
            if(Debug.InDebugging){
                Log.e("ServerService", "IOException bei WebSocket", e);
            }
        } catch (InterruptedException e) {
            if(Debug.InDebugging){
                Log.e("ServerService", "InterrupException bei WebSocket", e);
            }
        }
        WebSocketConnectionManager.clear();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanLocalBroadcaster();
        stopServer();
        if (_httpSocket != null) {
            try {
                _httpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
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

    private IntentFilter getIntentFilter() {
        IntentFilter toReturn = new IntentFilter(Constants.SERVER_HTTP_EVENT_NAME_COMMAND);
        toReturn.addAction(Constants.IP_REQUEST);
        return toReturn;
    }

    private void handleLocalBroadcast(Intent rawIntent) {
        String action = rawIntent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case Constants.IP_REQUEST:
                handleIpRequest();
                break;
            case Constants.SERVER_HTTP_EVENT_NAME_COMMAND:
                handleCommand(rawIntent);
                break;
            default:
                return;
        }
    }

    private void handleIpRequest() {
        Intent toSend = new Intent(Constants.IP_ANSWER);
        toSend.putExtra(Constants.IP_ANSWER_ADDRESS, _ipAddress);
        if (_httpServerActive) {
            toSend.putExtra(Constants.IP_ANSWER_FLAG_RUN, Constants.SERVER_HTTP_ISRUNNING_TRUE);
        } else {
            toSend.putExtra(Constants.IP_ANSWER_FLAG_RUN, Constants.SERVER_HTTP_ISRUNNING_FALSE);
        }
        _localBroadcaster.sendBroadcast(toSend);
    }

    private void handleCommand(Intent rawIntent) {
        String command = rawIntent.getStringExtra(Constants.SERVER_HTTP_COMMAND);

        switch (command) {
            case Constants.SERVER_HTTP_START:
                startServer();
                break;
            case Constants.SERVER_HTTP_STOP:
                stopServer();
                break;
            default:
                return;
        }
    }
}
