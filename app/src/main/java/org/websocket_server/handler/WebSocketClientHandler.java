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
import com.google.inject.Inject;

public class WebSocketClientHandler implements MessageHandlerStrategy {
  private Logger logger;
  private Server server;
  private Context context;
  private ObjectMapper objectMapper;

  @Inject
  public WebSocketClientHandler(Server server) {
    this.logger = LoggerFactory.getLogger(WebServerHandler.class);
    this.server = server;
    this.context = null;
    this.objectMapper = new ObjectMapper();
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
        logger.info("Sending notif");
        String strFileId = message.substring(3);
        String receivedIpAddr = conn.getRemoteSocketAddress().getAddress().getHostAddress();

        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("entry_id", this.context.getEntryId() + "");
        jsonMap.put("file_id", strFileId);
        jsonMap.put("ip_addr", receivedIpAddr);
        String json = this.objectMapper.writeValueAsString(jsonMap);
        this.server.getWebServerHandler().getConnection().send("ok/" + json);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  public void setContext(Context context) {
    this.context = context;
  }
}
