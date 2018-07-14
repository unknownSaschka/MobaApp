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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ImageSendService extends WebSocketServer{

    Bitmap testImage;
    byte[] byteArray;

    private LocalBroadcastManager _localBroadcaster;
    private BroadcastReceiver _localListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent ) {

            byte[] data = intent.getByteArrayExtra(Constants._imageDataName);
            if(data != null){
                sendImage(data);
            }
        }

    };

    public ImageSendService(InetSocketAddress address, Bitmap image, Context appContext){
        super(address);
        System.out.println("Starte WebSocket Server");
        testImage = image;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        testImage.compress(Bitmap.CompressFormat.JPEG, 30, stream);
        byteArray = stream.toByteArray();
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
        Log.i("ImageSendService", message);
        //sendTestPictures();
        //TestTimerSendThread thread = new TestTimerSendThread(this);
        //thread.run();
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {
        System.out.println("SocketServer gestartet");
    }

    public void sendTestPictures(){

        Log.i("ImageSendService", "SendeTestImage");
        WebSocket[] sockets = WebSocketConnectionManager.returnSessionList();
        Log.i("ImageSendService", String.valueOf(sockets.length));

        for ( int i=0; i < sockets.length; i++ )
        {
            //WebSocket s = sockets[i];
            //System.out.println(s);
            try {
                sockets[i].send("true");
                sockets[i].send(byteArray);
            } catch (Exception e) {

            }
        }
    }

    public synchronized void sendImage(byte[] image){
        WebSocket[] sockets = WebSocketConnectionManager.returnSessionList();

        for ( int i=0; i < sockets.length; i++ )
        {
            try {
                sockets[i].send("true");
                sockets[i].send(image);
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void stop() throws IOException, InterruptedException{
        super.stop();
        _localBroadcaster.unregisterReceiver(_localListener);
    }
}
