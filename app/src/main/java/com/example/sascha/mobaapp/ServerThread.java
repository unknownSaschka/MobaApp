package com.example.sascha.mobaapp;


import android.util.Log;

import java.net.Socket;
import java.net.ServerSocket;

public class ServerThread extends Thread {

    private ServerSocket httpSocket;
    private String html;

    public ServerThread(ServerSocket httpSocket, String html) {

        this.httpSocket = httpSocket;
        this.html = html;
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            while (true) {
                if (Debug.InDebugging) {
                    Log.i("ServerThread", "Wartet...");
                }
                socket = httpSocket.accept();
                if (Debug.InDebugging) {
                    Log.i("ServerThread", "Neuer Client");
                }
                if (socket == null || httpSocket == null) {
                    return;
                }
                HttpResponseThread responseThread = new HttpResponseThread(socket, html);
                responseThread.start();
            }

        } catch (Exception e) {
            if (Debug.InDebugging) {
                Log.i("ServerThread", "In der Exception");
            }
            closeServer(socket);
        }
    }

    private void closeServer(Socket socket) {
        try {
            if (socket.getOutputStream() != null) {
                socket.shutdownOutput();
            }
            if (socket.getInputStream() != null) {
                socket.shutdownInput();
            }
            if (Debug.InDebugging) {
                Log.i("ServerThread", "Server wurde geschlossen");
            }
        } catch (Exception e) {
            if (Debug.InDebugging) {
                Log.i("ServerThread", "Server wurde durch Exception geschlossen");
            }
        }
    }
}
