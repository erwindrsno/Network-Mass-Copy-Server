package org.websocket_server.handler;

import static org.websocket_server.util.IpAddrExtractor.IP_EXTRACTOR;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.websocket_server.Server;
import org.websocket_server.util.ConnectionHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    logger.info(message);
    if (message.startsWith("monitor/")) {
      String labNumber = message.substring(8);
      ObjectMapper mapper = new ObjectMapper();

      logger.info("received labNUmber is: " + labNumber);

      ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1]; // hack for mutable reference

      futureHolder[0] = scheduler.scheduleAtFixedRate(() -> {
        try {
          if (this.getConnection() == null) {
            futureHolder[0].cancel(false);
            return;
          }
          Collection<WebSocket> connections = this.server.getConnections();
          String ipPrefix = "10.100.7" + labNumber;

          List<String> filteredConnections = connections.stream()
              .map(IP_EXTRACTOR::extract) // get IP
              .filter(ip -> !ip.equals(this.dotenv.get("LOCAL_WEBSOCKET_IP")))
              .filter(ip -> ip.startsWith(ipPrefix))
              .collect(Collectors.toList());

          mapper.enable(SerializationFeature.INDENT_OUTPUT);
          String json = mapper.writeValueAsString(filteredConnections);
          logger.info(json);
          this.conn.send("monitor/" + json);

        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }, 2, 3, TimeUnit.SECONDS);
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
