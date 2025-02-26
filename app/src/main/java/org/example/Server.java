package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocketAdapter;
import org.java_websocket.framing.Framedata;
import org.slf4j.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.example.model.BaseLab;
import org.example.model.BaseLab.Client;

public class Server extends WebSocketServer {
    Logger logger = LoggerFactory.getLogger(Server.class);
    Scanner sc = new Scanner(System.in);

    BaseLab l1 = new BaseLab(1, 40);
    BaseLab l2 = new BaseLab(2, 45);
    BaseLab l3 = new BaseLab(3, 35);
    BaseLab l4 = new BaseLab(4, 35);
    WebSocket connLocal = null;

    FileHandler fileHandler;

    public Server(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
        String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        if (connIp.startsWith("10.100.71")) {
            l1.setClientConnection(connIp, conn);
        } else if (connIp.startsWith("10.100.72")) {
            l2.setClientConnection(connIp, conn);
        } else if (connIp.startsWith("10.100.73")) {
            l3.setClientConnection(connIp, conn);
        } else if (connIp.startsWith("10.100.74")) {
            l4.setClientConnection(connIp, conn);
        } else {
            connLocal = conn;
            logger.info("Local IP is: " + connIp);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.equals("PONG")) {
            String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
            if (connIp.startsWith("10.100.71")) {
                l1.setActiveByIP(connIp);
                logger.info(l1.getHostnameByIP(connIp) + " is active");
            } else if (connIp.startsWith("10.100.72")) {
                l2.setActiveByIP(connIp);
                logger.info(l2.getHostnameByIP(connIp) + " is active");
            } else if (connIp.startsWith("10.100.73")) {
                l3.setActiveByIP(connIp);
                logger.info(l3.getHostnameByIP(connIp) + " is active");
            } else if (connIp.startsWith("10.100.74")) {
                l4.setActiveByIP(connIp);
                logger.info(l4.getHostnameByIP(connIp) + " is active");
            } else {
                logger.info("Pong received from " + connIp);
            }
        } else if (message.equals("READY-FILE~")) {
            this.fileHandler.startSend();
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        System.out.println("received ByteBuffer from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("an error occurred on connection " + conn.getRemoteSocketAddress() + ":" + ex);
    }

    @Override
    public void onStart() {
        String myIP = "";
        try {
            InetAddress me = InetAddress.getLocalHost();
            myIP += me.getHostAddress();
        } catch (Exception e) {
            logger.error("Error getting localhost IP address");
            e.printStackTrace();
        }
        logger.info("Server started at ws://" + myIP + ":8887");

        new Thread(() -> {
            logger.info("Command receiver thread started");
            while (true) {
                if (sc.hasNext()) {
                    String command = sc.next();
                    switch (command) {
                        case "ping":
                            broadcast("PING");
                            break;

                        case "ping 1":
                            pingLab(l1);
                            break;

                        case "ping 2":
                            pingLab(l2);
                            break;

                        case "ping 3":
                            pingLab(l3);
                            break;

                        case "ping 4":
                            pingLab(l4);
                            break;

                        case "ping 0":
                            pingLocal();
                            break;

                        case "send":
                            File file = new File("T06xxyyy.zip");
                            this.fileHandler = new FileHandler(this, file);
                            this.fileHandler.sendFileMetadata();
                            break;
                    }
                }
            }
        }).start();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();

        if (connIp.startsWith("10.100.71")) {
            l1.closeClientConnection(connIp);
        } else if (connIp.startsWith("10.100.72")) {
            l2.closeClientConnection(connIp);
        } else if (connIp.startsWith("10.100.73")) {
            l3.closeClientConnection(connIp);
        } else if (connIp.startsWith("10.100.74")) {
            l4.closeClientConnection(connIp);
        } else {
            connLocal = conn;
            logger.info("Local IP is: " + connIp);
        }
        System.out.println(
                "closed " + conn.getRemoteSocketAddress().getAddress() + " with exit code " + code
                        + " additional info: " + reason);
    }

    public void pingLab(BaseLab lab) {
        ConcurrentHashMap<String, Client> clients = lab.getClients();
        Iterator<Map.Entry<String, Client>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Client> entry = iterator.next();
            WebSocket conn = entry.getValue().getConn();
            if (conn != null) {
                conn.send("PING");
            }
            // else: tampilkan yang belum terhubung
        }
    }

    public void pingLocal() {
        this.connLocal.send("PING");
    }
}

// ping harus periodic
// trigger onClose conenction
