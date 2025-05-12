package org.example;

import static org.example.util.IpAddrExtractor.IP_EXTRACTOR;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.model.Context;
import org.example.model.FileAccessInfo;
import org.example.model.FileChunkMetadata;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;

public class WebServerHandler implements MessageHandlerStrategy, ConnectionHolder {
  private WebSocket conn;
  private Server server;
  private Integer port;

  private Context context;
  private List<FileChunkMetadata> listFcm;
  private int fileCounter;
  private long chunkCounter;
  private boolean readyToReceive;
  private FileVerifier fileVerifier;
  private Logger logger;

  @Inject
  public WebServerHandler(FileVerifier fileVerifier, Server server) {
    this.conn = null;
    this.server = server;
    this.port = null;
    this.context = null;
    this.fileVerifier = fileVerifier;
    this.listFcm = null;
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

          this.sendFilesToClients();
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
        this.context = mapper.readValue(json, Context.class);
        this.listFcm = this.context.getListFcm();

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

  public void sendFilesToClients() {
    Collection<WebSocket> connections = this.server.getConnections();

    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty print

      // String json = mapper.writeValueAsString(this.context);
      // logger.info(json);

      // List<String> listTargetCopyIpAddr = this.context.getListFai().stream()
      // .map(FileAccessInfo::getIp_address)
      // .collect(Collectors.toList());

      Map<String, List<FileAccessInfo>> groupedByIp = this.context.getListFai().stream()
          .collect(Collectors.groupingBy(FileAccessInfo::getIp_address));

      connections.stream()
          .filter(conn -> conn.isOpen())
          .filter(conn -> {
            String ip = IP_EXTRACTOR.extract(conn);
            return !ip.equals("10.100.70.211") && groupedByIp.containsKey(ip);
          })
          .forEach(conn -> {
            String ip = IP_EXTRACTOR.extract(conn);
            List<FileAccessInfo> listFaiPerClient = groupedByIp.getOrDefault(ip, Collections.emptyList());

            if (!listFaiPerClient.isEmpty()) {
              Context clientContext = Context.builder()
                  .listFai(listFaiPerClient)
                  .listFcm(this.context.getListFcm())
                  .build();

              try {
                String json = mapper.writeValueAsString(clientContext);
                conn.send("server/metadata/" + json);
              } catch (Exception e) {
                logger.error(e.getMessage(), e);
              }
            }
          });
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }

  }
}
