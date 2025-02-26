package org.example;

import java.net.InetSocketAddress;

import org.java_websocket.server.WebSocketServer;

public class App {
    public static void main(String[] args) {
        // String host = "10.100.70.60";
        String host = "10.100.70.211";
        int port = 8887;

        WebSocketServer server = new Server(new InetSocketAddress(host, port));
        server.run();
    }
}
