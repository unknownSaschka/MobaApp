package com.example.sascha.mobaapp;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Sascha on 25.05.2018.
 */

public class HttpResponseThread extends Thread{

    Socket socket;

    public HttpResponseThread(Socket socket){
        this.socket = socket;
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
        String htmlBodyInhalt = "<h1>Test</h1>";

        writeHTML(out, htmlBodyInhalt);

        try {
            //wait(1000);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeHTML(PrintWriter out, String bodyInhalt){

        String html = "<html><head></head><body> " + bodyInhalt + " </body></html>";

        out.print("HTTP/1.1 200 OK" + "\r\n");
        out.print("Content-Type: text/html" + "\r\n");
        out.print("Content-Length: " + html.length() + "\r\n\r\n");
        out.print(html + "\r\n");
        out.flush();

    }

}
