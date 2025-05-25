package org.websocket_server.handler;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.websocket_server.Server;
import org.websocket_server.model.Context;
import org.websocket_server.model.FileChunkMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;

public class WebSocketClientHandler implements MessageHandlerStrategy {
  private Logger logger;
  private Server server;
  private Context context;
  private ObjectMapper mapper;

  @Inject
  public WebSocketClientHandler(Server server) {
    this.logger = LoggerFactory.getLogger(WebServerHandler.class);
    this.server = server;
    this.context = null;
    this.mapper = new ObjectMapper();
    this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  @Override
  public void handleByte(ByteBuffer buffer) {
    // TODO Auto-generated method stub

  }

  @Override
  public void handleString(String message) {
  }

  @Override
  public void handleWebSocketClientString(WebSocket conn, String message) {
    if (message.startsWith("file~")) {
      String[] parts = message.split("CHUNK-ID~");

      String leftPart = parts[0]; // "file~5"
      String rightPart = parts[1]; // "10"

      String requestedFileUuid = leftPart.split("~")[1];
      Long chunkId = Long.parseLong(rightPart);

      FileChunkMetadata requestedFcm = this.context.getListFcm().stream()
          .filter(fcm -> fcm.getUuid().equals(requestedFileUuid))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("No file found."));

      byte[] arrOfBytes = requestedFcm.getMapOfChunks().get(chunkId);
      ByteBuffer byteBuffer = ByteBuffer.wrap(arrOfBytes);
      conn.send(byteBuffer);

    } else if (message.startsWith("ok/")) {
      try {
        if (message.substring(3).startsWith("copy/")) {
          String strFileId = message.substring(8);
          String receivedIpAddr = conn.getRemoteSocketAddress().getAddress().getHostAddress();

          Map<String, String> jsonMap = new HashMap<>();
          jsonMap.put("file_id", strFileId);
          jsonMap.put("ip_addr", receivedIpAddr);
          String json = this.mapper.writeValueAsString(jsonMap);
          this.server.getWebServerHandler().getConnection().send("ok/" + json);
        }
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    } else if (message.startsWith("fin/")) {
      try {
        if (message.substring(4).startsWith("copy/")) {
          Integer directoryId = Integer.parseInt(message.substring(9));
          this.server.getWebServerHandler().getConnection().send("fin/copy/" + directoryId);

        } else if (message.substring(4).startsWith("takeown/")) {
          Integer directoryId = Integer.parseInt(message.substring(12));
          this.server.getWebServerHandler().getConnection().send("fin/takeown/" + directoryId);
        } else if (message.substring(4).startsWith("delete/")) {
          Integer directoryId = Integer.parseInt(message.substring(11));
          this.server.getWebServerHandler().getConnection().send("fin/delete/" + directoryId);
        } else if (message.substring(4).startsWith("single-delete/")) {
          Integer fileId = Integer.parseInt(message.substring(18));
          logger.info("file id to be update deletedAT is : " + fileId);
          this.server.getWebServerHandler().getConnection().send("fin/single-delete/" + fileId);
        }
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  public void setContext(Context context) {
    this.context = context;
  }
}
