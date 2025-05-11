package org.example;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.util.Random;

public class WebClientHandler implements MessageHandlerStrategy, ConnectionHolder {
  private WebSocket conn;
  private Integer port;

  private Server server;
  private Logger logger;
  private Random rand;

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  @Inject
  public WebClientHandler(Server server) {
    this.conn = null;
    this.port = null;
    this.server = server;
    this.rand = new Random();
    this.logger = LoggerFactory.getLogger(WebServerHandler.class);
  }

  @Override
  public void handleByte(ByteBuffer buffer) {
    // TODO Auto-generated method stub

  }

  @Override
  public void handleString(String message) {
    if (message.startsWith("monitor/")) {
      String labNumber = message.substring(8);

      logger.info("received labNUmber is: " + labNumber);

      try {
        Collection<WebSocket> connections = this.server.getConnections();
        List<String> openedIpAddressList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String ipPrefix = "10.100.7" + labNumber;

        connections.stream()
            .map(conn -> new AbstractMap.SimpleEntry<>(conn,
                conn.getRemoteSocketAddress().getAddress().getHostAddress()))
            .filter(entry -> !entry.getValue().equals("192.168.0.106")) // exclude
            .filter(entry -> entry.getValue().startsWith(ipPrefix)) // filter by prefix
            .forEach(entry -> {
              WebSocket conn = entry.getKey();
              String ip = entry.getValue();
              if (conn.isOpen()) {
                openedIpAddressList.add(ip);
              } else if (conn.isClosed() && openedIpAddressList.contains(ip)) {
                openedIpAddressList.remove(ip);
              }
            });

        // simulation
        // for (int i = 1; i <= 40; i++) {
        // boolean isActive = this.rand.nextBoolean();
        // if (isActive) {
        // if (i >= 10) {
        // openedIpAddressList.add("10.100.71.2" + i);
        // } else {
        // openedIpAddressList.add("10.100.71.20" + i);
        // }
        // }
        // }

        String json = mapper.writeValueAsString(openedIpAddressList);
        this.conn.send(json);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public WebSocket getConnection() {
    if (this.conn != null) {
      return this.conn;
    }
    return null;
  }

  @Override
  public void setConnection(WebSocket conn, Integer port) {
    this.conn = conn;
    this.port = port;
  }

  @Override
  public Integer getPortNumber() {
    if (this.port != null) {
      return this.port;
    }
    return null;
  }
}
