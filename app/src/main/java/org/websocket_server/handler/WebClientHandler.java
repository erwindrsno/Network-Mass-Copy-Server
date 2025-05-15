package org.websocket_server.handler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.websocket_server.Server;
import org.websocket_server.util.ConnectionHolder;

import static org.websocket_server.util.IpAddrExtractor.IP_EXTRACTOR;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

public class WebClientHandler implements MessageHandlerStrategy, ConnectionHolder {
  private WebSocket conn;
  private Integer port;

  private Server server;
  private Logger logger;

  @Inject
  public WebClientHandler(Server server) {
    this.conn = null;
    this.port = null;
    this.server = server;
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
            .filter(conn -> !IP_EXTRACTOR.extract(conn).equals("10.100.70.211")) // exclude
            .filter(
                conn -> IP_EXTRACTOR.extract(conn).startsWith(ipPrefix)) // filter
            .forEach(conn -> {
              String ip_addr = IP_EXTRACTOR.extract(conn);
              if (conn.isOpen()) {

                openedIpAddressList.add(ip_addr);
              } else if (conn.isClosed() && openedIpAddressList.contains(ip_addr)) {
                openedIpAddressList.remove(ip_addr);
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
        this.conn.send("monitor/" + json);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public void handleWebSocketClientString(WebSocket conn, String message) {
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
