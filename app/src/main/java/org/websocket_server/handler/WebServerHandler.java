package org.websocket_server.handler;

import static org.websocket_server.util.IpAddrExtractor.IP_EXTRACTOR;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.websocket_server.Server;
import org.websocket_server.model.Context;
import org.websocket_server.model.DirectoryAccessInfo;
import org.websocket_server.model.FileAccessInfo;
import org.websocket_server.model.FileChunkMetadata;
import org.websocket_server.util.ConnectionHolder;
import org.websocket_server.util.FileVerifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import io.github.cdimascio.dotenv.Dotenv;

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
  ObjectMapper mapper;
  Dotenv dotenv;

  @Inject
  public WebServerHandler(FileVerifier fileVerifier, Server server,
      Dotenv dotenv) {
    this.conn = null;
    this.server = server;
    this.port = null;
    this.context = null;
    this.fileVerifier = fileVerifier;
    this.listFcm = null;
    this.mapper = new ObjectMapper();
    this.dotenv = dotenv;
    this.logger = LoggerFactory.getLogger(WebServerHandler.class);

    // listFcm sebenarnya tidak perlu, namun untuk separation of concerns antara
    // FileAccess info dan FileChunkMetadata
    // dibuatkan variabel list untuk menampung chunks yang dari context.
    // case nya ada di method handleString
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
          this.server.getWsClientHandler().setContext(this.context);
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
    if (message.startsWith("metadata/copy/")) {
      String json = message.substring(14);

      try {
        this.context = this.mapper.readValue(json, Context.class);
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
    } else if (message.startsWith("metadata/takeown/")) {
      String json = message.substring(17);
      try {
        this.context = this.mapper.readValue(json, Context.class);

        Collection<WebSocket> connections = this.server.getConnections();
        Map<String, List<FileAccessInfo>> groupedByIp = this.context.getListFai().stream()
            .collect(Collectors.groupingBy(FileAccessInfo::getIp_address));

        connections.stream()
            .filter(conn -> conn.isOpen())
            .filter(conn -> {
              String ip = IP_EXTRACTOR.extract(conn);
              return !ip.equals(dotenv.get("LOCAL_WEBSOCKET_IP")) && groupedByIp.containsKey(ip);
            })
            .forEach(conn -> {
              String ip = IP_EXTRACTOR.extract(conn);
              List<FileAccessInfo> listFaiPerClient = groupedByIp.getOrDefault(ip, Collections.emptyList());

              List<DirectoryAccessInfo> listDaiPerClient = this.context.getListDai().stream()
                  .filter(dai -> dai.getId().equals(listFaiPerClient.get(0).getDirectoryId()))
                  .collect(Collectors.toList());

              if (!listFaiPerClient.isEmpty()) {
                Context clientContext = Context.builder()
                    .listFai(listFaiPerClient)
                    .listDai(listDaiPerClient)
                    .build();

                try {
                  String toBeSendJson = this.mapper.writeValueAsString(clientContext);
                  conn.send("server/metadata/takeown/" + toBeSendJson);
                } catch (Exception e) {
                  logger.error(e.getMessage(), e);
                }
              }
            });
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    } else if (message.startsWith("metadata/delete/")) {
      String json = message.substring(16);
      logger.info("Delete command received.");
      try {
        this.context = this.mapper.readValue(json, Context.class);

        Collection<WebSocket> connections = this.server.getConnections();
        Map<String, List<FileAccessInfo>> groupedByIp = this.context.getListFai().stream()
            .collect(Collectors.groupingBy(FileAccessInfo::getIp_address));

        connections.stream()
            .filter(conn -> conn.isOpen())
            .filter(conn -> {
              String ip = IP_EXTRACTOR.extract(conn);
              return !ip.equals(dotenv.get("LOCAL_WEBSOCKET_IP")) && groupedByIp.containsKey(ip);
            })
            .forEach(conn -> {
              String ip = IP_EXTRACTOR.extract(conn);
              List<FileAccessInfo> listFaiPerClient = groupedByIp.getOrDefault(ip, Collections.emptyList());

              List<DirectoryAccessInfo> listDaiPerClient = this.context.getListDai().stream()
                  .filter(dai -> dai.getId().equals(listFaiPerClient.get(0).getDirectoryId()))
                  .collect(Collectors.toList());

              if (!listFaiPerClient.isEmpty()) {
                Context clientContext = Context.builder()
                    .listFai(listFaiPerClient)
                    .listDai(listDaiPerClient)
                    .build();

                try {
                  String toBeSendJson = this.mapper.writeValueAsString(clientContext);
                  conn.send("server/metadata/delete/" + toBeSendJson);
                } catch (Exception e) {
                  logger.error(e.getMessage(), e);
                }
              }
            });
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    } else if (message.startsWith("metadata/single-delete/")) {
      String json = message.substring(23);
      logger.info("Delete command received.");
      try {
        this.context = this.mapper.readValue(json, Context.class);

        Collection<WebSocket> connections = this.server.getConnections();
        Map<String, List<FileAccessInfo>> groupedByIp = this.context.getListFai().stream()
            .collect(Collectors.groupingBy(FileAccessInfo::getIp_address));

        connections.stream()
            .filter(conn -> conn.isOpen())
            .filter(conn -> {
              String ip = IP_EXTRACTOR.extract(conn);
              return !ip.equals(dotenv.get("LOCAL_WEBSOCKET_IP")) && groupedByIp.containsKey(ip);
            })
            .forEach(conn -> {
              String ip = IP_EXTRACTOR.extract(conn);
              List<FileAccessInfo> listFaiPerClient = groupedByIp.getOrDefault(ip, Collections.emptyList());

              List<DirectoryAccessInfo> listDaiPerClient = this.context.getListDai().stream()
                  .filter(dai -> dai.getId().equals(listFaiPerClient.get(0).getDirectoryId()))
                  .collect(Collectors.toList());

              if (!listFaiPerClient.isEmpty()) {
                Context clientContext = Context.builder()
                    .listFai(listFaiPerClient)
                    .listDai(listDaiPerClient)
                    .build();

                try {
                  String toBeSendJson = this.mapper.writeValueAsString(clientContext);
                  conn.send("server/metadata/single-delete/" + toBeSendJson);
                } catch (Exception e) {
                  logger.error(e.getMessage(), e);
                }
              }
            });
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    } else if (message.startsWith("to-webclient/refetch")) {
      this.server.getWebClientHandler().getConnection().send("refetch");
    }
  }

  @Override
  public void handleWebSocketClientString(WebSocket conn, String message) {
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
      Map<String, List<FileAccessInfo>> groupedByIp = this.context.getListFai().stream()
          .collect(Collectors.groupingBy(FileAccessInfo::getIp_address));

      connections.stream()
          .filter(conn -> conn.isOpen())
          .filter(conn -> {
            String ip = IP_EXTRACTOR.extract(conn);
            return !ip.equals(dotenv.get("LOCAL_WEBSOCKET_IP")) && groupedByIp.containsKey(ip);
          })
          .forEach(conn -> {
            String ip = IP_EXTRACTOR.extract(conn);
            List<FileAccessInfo> listFaiPerClient = groupedByIp.getOrDefault(ip, Collections.emptyList());
            List<DirectoryAccessInfo> listDaiPerClient = this.context.getListDai().stream()
                .filter(dai -> dai.getId().equals(listFaiPerClient.get(0).getDirectoryId()))
                .collect(Collectors.toList());

            if (!listFaiPerClient.isEmpty()) {
              Context clientContext = Context.builder()
                  .listFai(listFaiPerClient)
                  .listDai(listDaiPerClient)
                  .listFcm(this.context.getListFcm())
                  .build();

              try {
                String json = this.mapper.writeValueAsString(clientContext);
                logger.info("Sending metadata to client...");
                conn.send("server/metadata/copy/" + json);
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
