package com.example.sascha.mobaapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class HttpResponseThread extends Thread {

    Socket socket;
    String html;

    public HttpResponseThread(Socket socket, String html) {

        this.socket = socket;
        this.html = html;
    }

    @Override
    public void run() {
        PrintWriter out = null;

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (out == null) return;


        writeHTML(out, html);

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeHTML(PrintWriter out, String html) {

        out.print("HTTP/1.1 200 OK" + "\r\n");
        out.print("Content-Type: text/html" + "\r\n");
        out.print("Content-Length: " + html.length() + "\r\n\r\n");
        out.print(html + "\r\n");
        out.flush();

    }

}
