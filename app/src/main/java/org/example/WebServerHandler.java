package org.example;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.model.Context;
import org.example.model.FileAccessInfo;
import org.example.model.FileChunkMetadata;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

public class WebServerHandler implements MessageHandlerStrategy, ConnectionHolder {
  private WebSocket conn;
  private Integer port;

  private List<FileChunkMetadata> listFcm;
  private int fileCounter;
  private long chunkCounter;
  private boolean readyToReceive;
  private FileVerifier fileVerifier;
  private Logger logger;

  @Inject
  public WebServerHandler(FileVerifier fileVerifier) {
    this.conn = null;
    this.port = null;
    this.fileVerifier = fileVerifier;
    this.logger = LoggerFactory.getLogger(WebServerHandler.class);
  }

  @Override
  public void handleByte(ByteBuffer buffer) {
    if (this.readyToReceive) {
      byte[] data = buffer.array();

      FileChunkMetadata tempFcm = this.listFcm.get(this.fileCounter);
      Map<Long, byte[]> tempMapOfChunks = tempFcm.getMapOfChunks();

      tempMapOfChunks.put(this.chunkCounter, data);
      this.fileVerifier.putBytes(data);

      this.chunkCounter++;

      if (tempMapOfChunks.size() == tempFcm.getChunkCount()) {
        boolean isVerified = this.fileVerifier.verifyHashedBytes(tempFcm.getSignature());
        if (isVerified) {
          logger.info("File is safe. dont worry.");
        } else {
          logger.error("a file is NOT SAFE..., go next!");
        }

        this.fileVerifier.clear();

        this.chunkCounter = 0;

        if (this.fileCounter == this.listFcm.size() - 1) {
          this.fileCounter = 0;
          this.readyToReceive = false;
          logger.info("All files received.");
          return;
        } else {
          this.fileCounter++;
          Map<Long, byte[]> mapOfChunks = new HashMap<>();
          this.listFcm.get(this.fileCounter).setMapOfChunks(mapOfChunks);
          this.fileVerifier.prepare();
          this.conn
              .send("file~" + this.listFcm.get(this.fileCounter).getUuid() + "CHUNK-ID~" + this.chunkCounter);
        }
      } else {
        this.conn.send("file~" + tempFcm.getUuid() + "CHUNK-ID~" + this.chunkCounter);
      }
    }
  }

  @Override
  public void handleString(String message) {
    if (message.startsWith("metadata/")) {
      String json = message.substring(9);
      ObjectMapper mapper = new ObjectMapper();

      try {
        Context context = mapper.readValue(json, Context.class);
        List<FileAccessInfo> listFai = context.getListFai();
        this.listFcm = context.getListFcm();

        this.fileCounter = 0;
        this.chunkCounter = 0;
        this.readyToReceive = true;

        Map<Long, byte[]> mapOfChunks = new HashMap<>();

        this.listFcm.get(this.fileCounter).setMapOfChunks(mapOfChunks);
        this.fileVerifier.prepare();

        this.conn.send("file~" + this.listFcm.get(this.fileCounter).getUuid() +
            "CHUNK-ID~" + this.chunkCounter);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (message.equals("active-computers")) {
      // TODO
    }
  }

  @Override
  public void setConnection(WebSocket conn, Integer port) {
    this.conn = conn;
    this.port = port;
  }

  @Override
  public WebSocket getConnection() {
    if (this.conn != null) {
      return this.conn;
    }
    return null;
  }

  @Override
  public Integer getPortNumber() {
    if (this.port != null) {
      return this.port;
    }
    return null;
  }
}
