package org.websocket_server.handler;

import static org.websocket_server.util.IpAddrExtractor.IP_EXTRACTOR;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.websocket_server.Server;
import org.websocket_server.util.ConnectionHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import io.github.cdimascio.dotenv.Dotenv;

public class WebClientHandler implements MessageHandlerStrategy, ConnectionHolder {
  private WebSocket conn;
  private Integer port;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private Server server;
  private Logger logger;
  private Dotenv dotenv;

  @Inject
  public WebClientHandler(Server server, Dotenv dotenv) {
    this.conn = null;
    this.port = null;
    this.server = server;
    this.logger = LoggerFactory.getLogger(WebServerHandler.class);
    this.dotenv = dotenv;
  }

  @Override
  public void handleByte(ByteBuffer buffer) {
    // TODO Auto-generated method stub

  }

  @Override
  public void handleString(String message) {
    if (message.startsWith("monitor/")) {
      String labNumber = message.substring(8);
      ObjectMapper mapper = new ObjectMapper();

      logger.info("received labNUmber is: " + labNumber);

      try {
        scheduler.scheduleAtFixedRate(() -> {
          try {
            Collection<WebSocket> connections = this.server.getConnections();
            String ipPrefix = "10.100.7" + labNumber;

            List<WebSocket> filteredConnections = connections.stream()
                .filter(conn -> !IP_EXTRACTOR.extract(conn).equals(this.dotenv.get("LOCAL_WEBSOCKET_IP")))
                .filter(conn -> IP_EXTRACTOR.extract(conn).startsWith(ipPrefix))
                .filter(WebSocket::isOpen)
                .collect(Collectors.toList());

            String json = mapper.writeValueAsString(filteredConnections);
            this.conn.send("monitor/" + json);

          } catch (Exception e) {
            e.printStackTrace();
          }
        }, 0, 5, TimeUnit.SECONDS);

        // Collection<WebSocket> connections = this.server.getConnections();
        // String ipPrefix = "10.100.7" + labNumber;
        //
        // connections.stream()
        // .filter(conn ->
        // !IP_EXTRACTOR.extract(conn).equals(this.dotenv.get("LOCAL_WEBSOCKET_IP"))) //
        // exclude
        // .filter(
        // conn -> IP_EXTRACTOR.extract(conn).startsWith(ipPrefix)) // filter
        // .filter(conn -> conn.isOpen())
        // .collect(Collectors.toList());
        //
        // String json = mapper.writeValueAsString(connections);
        // this.conn.send("monitor/" + json);
        // .forEach(conn -> {
        // String ip_addr = IP_EXTRACTOR.extract(conn);
        // if (conn.isOpen()) {
        //
        // openedIpAddressList.add(ip_addr);
        // } else if (conn.isClosed() && openedIpAddressList.contains(ip_addr)) {
        // openedIpAddressList.remove(ip_addr);
        // }
        // });

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
