package org.websocket_server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.websocket_server.handler.WebClientHandler;
import org.websocket_server.handler.WebServerHandler;
import org.websocket_server.handler.WebSocketClientHandler;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import io.github.cdimascio.dotenv.Dotenv;

public class Server extends WebSocketServer {
  Logger logger = LoggerFactory.getLogger(Server.class);
  Scanner sc = new Scanner(System.in);

  WebSocket connLocalClient = null;

  ConcurrentHashMap<String, String> otherClients = new ConcurrentHashMap<>();

  private WebServerHandler webServerHandler;
  private WebClientHandler webClientHandler;
  private WebSocketClientHandler webSocketClientHandler;
  private Dotenv dotenv;

  public Server(InetSocketAddress address) {
    super(address);
  }

  @Inject
  public void injectDependencies(WebServerHandler webServerHandler, WebClientHandler webClientHandler,
      WebSocketClientHandler webSocketClientHandler, Dotenv dotenv) {
    this.webServerHandler = webServerHandler;
    this.webClientHandler = webClientHandler;
    this.webSocketClientHandler = webSocketClientHandler;
    this.dotenv = dotenv;
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    logger.info("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
    int port = conn.getRemoteSocketAddress().getPort();
    if (connIp.equals(dotenv.get("LOCAL_WEBSOCKET_IP"))) {
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
    if (message.startsWith("webserver/")) {
      this.webServerHandler.handleString(message.substring(10));
    } else if (message.startsWith("webclient/")) {
      this.webClientHandler.handleString(message.substring(10));
    } else if (message.startsWith("client/")) {
      this.webSocketClientHandler.handleWebSocketClientString(conn, message.substring(7));
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

    if (connIp.equals("10.100.70.211")) {
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

  public WebSocketClientHandler getWsClientHandler() {
    return this.webSocketClientHandler;
  }

  public WebServerHandler getWebServerHandler() {
    return this.webServerHandler;
  }

  public WebClientHandler getWebClientHandler() {
    return this.webClientHandler;
  }
}
