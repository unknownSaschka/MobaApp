package com.Simple_Stream.Servers;

import android.util.Log;

import com.Simple_Stream.Constants;

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
        if (Constants.InDebugging) {
            Log.i("HTTPResponseThread", "Verarbeitet Client");
        }

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (out == null) return;

        if (Constants.InDebugging) {
            Log.i("HTTPResponseThread", "Schreibe HTML raus");
        }
        writeHTML(out, html);

        try {
            if (Constants.InDebugging) {
                Log.i("HTTPResponseThread", "Schließe Socket");
            }
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
