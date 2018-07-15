package com.example.sascha.mobaapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ImageSendService extends WebSocketServer {

    private LocalBroadcastManager _localBroadcaster;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {
        //Gets only events from Type Constants._imageEventName
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] data = intent.getByteArrayExtra(Constants._imageDataName);
            int width = intent.getIntExtra(Constants._imageWidth, 0);
            int height = intent.getIntExtra(Constants._imageHeight, 0);

            if (data != null && width != 0 && height != 0) {
                sendImage(data, width, height);
            }
        }

    };

    public ImageSendService(InetSocketAddress address, Context appContext) {
        super(address);
        if (Debug.InDebugging) {
            Log.i("Server", "Starte WebSocket Server");
        }
        WebSocketConnectionManager.clear();
        _localBroadcaster = LocalBroadcastManager.getInstance(appContext);
        IntentFilter tempFilter = new IntentFilter(Constants._imageEventName);
        _localBroadcaster.registerReceiver(_localListener, tempFilter);
        Log.i("ImageSendService", "WebSocketServer läuft");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i("ImageSendService", "Öffnen");
        WebSocketConnectionManager.addSession(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WebSocketConnectionManager.removeSession(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (Debug.InDebugging) {
            Log.i("ImageSendService", message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (Debug.InDebugging) {
            Log.e("ImageSendService", "SocketFehler", ex);
        }
    }

    @Override
    public void onStart() {
        if (Debug.InDebugging) {
            Log.i("Server", "SocketServer gestartet");
        }
    }

    public synchronized void sendImage(byte[] image, int width, int height) {
        WebSocket[] sockets = WebSocketConnectionManager.returnSessionList();

        for (int i = 0; i < sockets.length; i++) {
            try {
                sockets[i].send("{\"width\":\"" + width +"\" , \"height\":\""+ height +"\" }");
                sockets[i].send(image);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        super.stop();
        _localBroadcaster.unregisterReceiver(_localListener);
    }
}
