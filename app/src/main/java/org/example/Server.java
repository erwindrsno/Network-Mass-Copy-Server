package org.example;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.example.util.IpAddrExtractor.IP_EXTRACTOR;

import org.example.model.BaseLab;
import org.example.model.FileAccessInfo;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

public class Server extends WebSocketServer {
  Logger logger = LoggerFactory.getLogger(Server.class);
  Scanner sc = new Scanner(System.in);

  BaseLab l1 = new BaseLab(1, 40);
  BaseLab l2 = new BaseLab(2, 45);
  BaseLab l3 = new BaseLab(3, 35);
  BaseLab l4 = new BaseLab(4, 35);
  WebSocket connLocalClient = null;

  ConcurrentHashMap<String, String> otherClients = new ConcurrentHashMap<>();

  private WebServerHandler webServerHandler;
  private WebClientHandler webClientHandler;

  public Server(InetSocketAddress address) {
    super(address);
  }

  @Inject
  public void injectDependencies(WebServerHandler webServerHandler, WebClientHandler webClientHandler) {
    this.webServerHandler = webServerHandler;
    this.webClientHandler = webClientHandler;
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    logger.info("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
    int port = conn.getRemoteSocketAddress().getPort();
    logger.info("And their port is: " + conn.getRemoteSocketAddress().getPort());
    if (connIp.startsWith("10.100.71")) {
      l1.setClientConnection(connIp, conn);
    } else if (connIp.startsWith("10.100.72")) {
      l2.setClientConnection(connIp, conn);
    } else if (connIp.startsWith("10.100.73")) {
      l3.setClientConnection(connIp, conn);
    } else if (connIp.startsWith("10.100.74")) {
      l4.setClientConnection(connIp, conn);
    } else if (connIp.equals("10.100.70.211")) {
      if (this.webServerHandler.getConnection() == null) {
        this.webServerHandler.setConnection(conn, port);
        logger.info("WebServer conn is set.");
      } else if (this.webServerHandler.getConnection() != null && this.webClientHandler.getConnection() == null) {
        this.webClientHandler.setConnection(conn, port);
        logger.info("WebClient conn is set also.");
      }
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
      }
      // connLocal.send(connIp);
    } else if (message.startsWith("CHUNK-ID~")) {
      int chunkId = Integer.parseInt(message.substring(9));
      // ByteBuffer byteBuffer = this.fileHandler.getChunkById(chunkId);
      // conn.send(byteBuffer);
    } else if (message.startsWith("webserver/")) {
      this.webServerHandler.handleString(message.substring(10));
    } else if (message.startsWith("webclient/")) {
      this.webClientHandler.handleString(message.substring(10));
    }

    if (message.startsWith("file~")) {
      String[] parts = message.split("CHUNK-ID~");

      String leftPart = parts[0]; // "file~5"
      String rightPart = parts[1]; // "10"

      int fileCounter = Integer.parseInt(leftPart.split("~")[1]);
      int chunkId = Integer.parseInt(rightPart);
      System.out.println("fileCounter at client is: " + fileCounter);
      System.out.println("chunkId at client is: " + chunkId);
      // ByteBuffer byteBuffer =
      // ByteBuffer.wrap(this.listOfChunkMaps.get(fileCounter).get(chunkId));
      // connLocalClient.send(byteBuffer);
    }
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer buffer) {
    this.webServerHandler.handleByte(buffer);
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
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
    String hostname = conn.getRemoteSocketAddress().getHostName();
    int port = conn.getRemoteSocketAddress().getPort();

    if (connIp.startsWith("10.100.71")) {
      l1.closeClientConnection(connIp);
    } else if (connIp.startsWith("10.100.72")) {
      l2.closeClientConnection(connIp);
    } else if (connIp.startsWith("10.100.73")) {
      l3.closeClientConnection(connIp);
    } else if (connIp.startsWith("10.100.74")) {
      l4.closeClientConnection(connIp);
    } else if (connIp.equals("10.100.70.211")) {
      if (port == this.webServerHandler.getPortNumber()) {
        this.webServerHandler.setConnection(null, null);
        logger.info("Web server conn is closed.");
      } else if (port == this.webClientHandler.getPortNumber()) {
        this.webClientHandler.setConnection(null, null);
        logger.info("Web client conn is also closed.");
      }
    } else {
      connLocalClient = null;
      logger.info(connIp + " has disconnected");
    }
  }

  public void pingLocal() {
    // this.connLocal.send("PING");
  }

  public void startSendFilesToClients(List<FileAccessInfo> listFai) {
  }
}
