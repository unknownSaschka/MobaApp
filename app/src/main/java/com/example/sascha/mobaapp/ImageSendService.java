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

        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] data = intent.getByteArrayExtra(Constants._imageDataName);
            if (data != null) {
                sendImage(data);
            }
        }

    };

    public ImageSendService(InetSocketAddress address, Bitmap image, Context appContext) {
        super(address);
        if (Debug.InDebugging) {
            Log.i("Server", "Starte WebSocket Server");
        }
        WebSocketConnectionManager.clear();
        _localBroadcaster = LocalBroadcastManager.getInstance(appContext);
        IntentFilter tempFilter = new IntentFilter(Constants._imageEventName);
        _localBroadcaster.registerReceiver(_localListener, tempFilter);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
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

    }

    @Override
    public void onStart() {
        if (Debug.InDebugging) {
            Log.i("Server", "SocketServer gestartet");
        }
    }

    public synchronized void sendImage(byte[] image) {
        WebSocket[] sockets = WebSocketConnectionManager.returnSessionList();

        for (int i = 0; i < sockets.length; i++) {
            try {
                sockets[i].send("true");
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
