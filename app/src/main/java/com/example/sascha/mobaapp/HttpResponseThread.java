package com.example.sascha.mobaapp;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * Created by Sascha on 25.05.2018.
 */

public class HttpResponseThread extends Thread{

    Socket socket;
    String html;

    public HttpResponseThread(Socket socket, String html){

        this.socket = socket;
        this.html = html;
    }

    @Override
    public void run(){
        PrintWriter out = null;

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(out == null) return;


        writeHTML(out, html);

        try {
            //wait(1000);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeHTML(PrintWriter out, String html){

        out.print("HTTP/1.1 200 OK" + "\r\n");
        out.print("Content-Type: text/html" + "\r\n");
        out.print("Content-Length: " + html.length() + "\r\n\r\n");
        out.print(html + "\r\n");
        out.flush();

    }

}
