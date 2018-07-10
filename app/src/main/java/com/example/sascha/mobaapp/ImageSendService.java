package com.example.sascha.mobaapp;


import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Created by Sascha on 08.07.2018.
 */

public class ImageSendService extends WebSocketServer{

    Bitmap testImage;
    byte[] byteArray;

    public ImageSendService(InetSocketAddress address, Bitmap image){
        super(address);
        System.out.println("Starte WebSocket Server");
        testImage = image;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        testImage.compress(Bitmap.CompressFormat.JPEG, 30, stream);
        byteArray = stream.toByteArray();
        WebSocketConnectionManager.clear();
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
        TestTimerSendThread thread = new TestTimerSendThread(this);
        thread.run();
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {
        System.out.println("SocketServer gestartet");
    }

    public void sendPicture(byte[] image, int rotation){

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
}
