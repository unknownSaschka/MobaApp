package com.Simple_Stream.HTTP_Server;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.Simple_Stream.Constants;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ImageSendService extends WebSocketServer {

    private LocalBroadcastManager _localBroadcaster;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {
        //Gets only events from Type Constants.IMAGE_EVENT_NAME
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] data = intent.getByteArrayExtra(Constants.IMAGE_DATA_NAME);
            int width = intent.getIntExtra(Constants.IMAGE_WIDTH, 0);
            int height = intent.getIntExtra(Constants.IMAGE_HEIGHT, 0);

            if (data != null && width != 0 && height != 0) {
                sendImage(data, width, height);
            }
        }

    };

    public ImageSendService(InetSocketAddress address, Context appContext) {
        super(address);
        setReuseAddr(true);
        if (Constants.InDebugging) {
            Log.i("Server", "Starte WebSocket Server");
        }
        WebSocketConnectionManager.clear();
        _localBroadcaster = LocalBroadcastManager.getInstance(appContext);
        IntentFilter tempFilter = new IntentFilter(Constants.IMAGE_EVENT_NAME);
        _localBroadcaster.registerReceiver(_localListener, tempFilter);
        Log.i("ImageSendService", "WebSocketServer läuft");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i("ImageSendService", "Öffnen");
        WebSocketConnectionManager.addSession(conn);
        sendClientAmount();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WebSocketConnectionManager.removeSession(conn);
        sendClientAmount();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (Constants.InDebugging) {
            Log.i("ImageSendService", message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (Constants.InDebugging) {
            Log.e("ImageSendService", "SocketFehler", ex);
        }
        sendClientAmount();
    }

    @Override
    public void onStart() {
        if (Constants.InDebugging) {
            Log.i("ImageSendService", "SocketServer gestartet");
        }
        sendClientAmount();
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
        if (Constants.InDebugging) {
            Log.i("ImageSendService", "SocketServer stoppen");
        }
        sendClientAmount();
    }

    private void sendClientAmount(){

        int amount = WebSocketConnectionManager.socketliste.size();
        Log.i("WenSocketService", "SendeClientZahl " + amount);
        Intent toSend = new Intent(Constants.CLIENT_CONNECTED_EVENT);
        toSend.putExtra(Constants.CLIENT_AMOUNT, amount);
        _localBroadcaster.sendBroadcast(toSend);
    }
}
