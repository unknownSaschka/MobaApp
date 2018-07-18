package com.Simple_Stream.Servers;

import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.Iterator;

public class WebSocketConnectionManager {

    public static final ArrayList<WebSocket> socketliste = new ArrayList<WebSocket>();
    private static int connectionCount = 0;

    private static synchronized Object modifySocketList(int type, Object input) {
        switch (type) {
            case 1:
                WebSocket[] temp2 = new WebSocket[socketliste.size()];
                for (int i = 0; i < socketliste.size(); i++) {
                    temp2[i] = socketliste.get(i);
                }
                return temp2;
            case 2:
                return socketliste.get((Integer) input);
            case 3:
                return socketliste.size();
            case 4:
                for (Iterator i = socketliste.iterator(); i.hasNext(); ) {
                    WebSocket temp = (WebSocket) i.next();
                    if (temp.equals(input)) {
                        return false;
                    }

                }
                socketliste.add((WebSocket) input);
                return true;
            case 5:
                socketliste.remove((WebSocket) input);
                return null;
            case 6:
                return socketliste;
        }
        return null;
    }

    // Synchronisierte Zugriffe auf die Liste
    public static WebSocket[] returnSessionList() {
        //return socketliste.toString();
        return (WebSocket[]) modifySocketList(1, null);
    }

    // Verbindung an der Position i holen
    public static WebSocket getSession(int i) {
        //return socketliste.get(i);
        return (WebSocket) modifySocketList(2, i);
    }

    // Anzahl der Verbindungen besorgen
    public static int SessionCount() {
        //return socketliste.size();
        return (Integer) modifySocketList(3, null);
    }

    // Verbindung hinzufÃ¼gen
    public static boolean addSession(WebSocket session) {
        //socketliste.add(session);
        return (boolean) modifySocketList(4, session);
    }

    // Verbindung entfernen
    public static void removeSession(WebSocket session) {
        //socketliste.remove(session);
        modifySocketList(5, session);
    }

    public static void clear() {
        socketliste.clear();
    }
}
