package com.example.sascha.mobaapp;


import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * Created by Sascha on 25.05.2018.
 */

public class ServerThread extends Thread {


    ServerSocket httpSocket = null;

    public ServerThread(ServerSocket httpSocket){
        this.httpSocket = httpSocket;
    }

    @Override
    public void run(){
        Socket socket = null;
        //Log.i("ServerThreadClass", "VorTryCatch");

        try {
            while(true) {
                Log.i("ServerThread", "Wartet...");
                socket = httpSocket.accept();
                if (socket == null || httpSocket == null) return;
                HttpResponseThread responseThread = new HttpResponseThread(socket);
                responseThread.start();
            }

        } catch (Exception e){
            Log.i("ServerThread","In der Exception");
            closeServer(socket);
        }
    }

    private void closeServer(Socket socket){
        try {
            if(socket.getOutputStream() != null){
                socket.shutdownOutput();
            }
            if(socket.getInputStream() != null){
                socket.shutdownInput();
            }
            Log.i("ServerThread","Server wurde geschlossen");
        } catch (Exception e) {
            Log.i("ServerThread","Server wurde durch Exception geschlossen");
        }
    }
}
