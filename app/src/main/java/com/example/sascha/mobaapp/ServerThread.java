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
    String html;

    public ServerThread(ServerSocket httpSocket, String html){

        this.httpSocket = httpSocket;
        this.html = html;
    }

    @Override
    public void run(){
        Socket socket = null;
        //Log.i("ServerThreadClass", "VorTryCatch");

        try {
            while(true) {
                if(Debug.InDebugging) {
                    Log.i("ServerThread", "Wartet...");
                }
                socket = httpSocket.accept();
                if (socket == null || httpSocket == null) return;
                HttpResponseThread responseThread = new HttpResponseThread(socket, html);
                responseThread.start();
            }

        } catch (Exception e) {
            if(Debug.InDebugging){
                Log.i("ServerThread", "In der Exception");
            }
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
            if(Debug.InDebugging) {
                Log.i("ServerThread", "Server wurde geschlossen");
            }
        } catch (Exception e) {
            if(Debug.InDebugging) {
                Log.i("ServerThread", "Server wurde durch Exception geschlossen");
            }
        }
    }
}
