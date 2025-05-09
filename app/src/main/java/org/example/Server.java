package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.example.model.BaseLab;
import org.example.model.BaseLab.Client;
import org.example.model.Context;
import org.example.model.FileAccessInfo;
import org.example.model.FileChunkMetadata;
import org.example.model.FileMetadata;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class Server extends WebSocketServer {
  Logger logger = LoggerFactory.getLogger(Server.class);
  Scanner sc = new Scanner(System.in);

  BaseLab l1 = new BaseLab(1, 40);
  BaseLab l2 = new BaseLab(2, 45);
  BaseLab l3 = new BaseLab(3, 35);
  BaseLab l4 = new BaseLab(4, 35);
  WebSocket connLocal = null;
  WebSocket connLocalClient = null;

  ConcurrentHashMap<String, String> otherClients = new ConcurrentHashMap<>();

  FileHandler fileHandler;

  List<Map<Integer, byte[]>> listOfChunkMaps = new ArrayList<>();
  List<FileMetadata> listFileMetadata;
  List<Path> listPath = new ArrayList<>();
  List<FileOutputStream> listFos = new ArrayList<>();
  int fileCounter;
  int chunkCounter;
  Integer entryId;
  String title = "";
  boolean isFinished;

  boolean readyToReceive;

  MultipleFileHandler mFileHandler;

  Context context;

  List<FileChunkMetadata> listFcm;

  Hasher hasher;

  FileVerifier fileVerifier;

  public Server(InetSocketAddress address) {
    super(address);
    this.hasher = Hashing.sha256().newHasher();
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    logger.info("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
    int connPort = conn.getRemoteSocketAddress().getPort();
    if (connIp.startsWith("10.100.71")) {
      l1.setClientConnection(connIp, conn);
    } else if (connIp.startsWith("10.100.72")) {
      l2.setClientConnection(connIp, conn);
    } else if (connIp.startsWith("10.100.73")) {
      l3.setClientConnection(connIp, conn);
    } else if (connIp.startsWith("10.100.74")) {
      l4.setClientConnection(connIp, conn);
    }
    // else if (connIp.equals("10.100.70.211")) {
    // // this.otherClients.put(connIp,
    // conn.getRemoteSocketAddress().getHostName());
    // connLocal = conn;
    // logger.info("Incoming IP is: " + connIp);
    // }
    else if (connIp.equals("192.168.0.106")) {
      if (connLocal != null && connLocalClient == null) {
        connLocalClient = conn;
        logger.info("Conn local client assigned too.");
      } else if (connLocal == null) {
        connLocal = conn;
        logger.info("Conn local server assigned.");
      }
    }
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    if (message.equals("PONG")) {
      String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
      if (connIp.startsWith("10.100.71")) {
        l1.setActiveByIP(connIp);
        logger.info(l1.getHostnameByIP(connIp) + " is active");
      } else if (connIp.startsWith("10.100.72")) {
        l2.setActiveByIP(connIp);
        logger.info(l2.getHostnameByIP(connIp) + " is active");
      } else if (connIp.startsWith("10.100.73")) {
        l3.setActiveByIP(connIp);
        logger.info(l3.getHostnameByIP(connIp) + " is active");
      } else if (connIp.startsWith("10.100.74")) {
        l4.setActiveByIP(connIp);
        logger.info(l4.getHostnameByIP(connIp) + " is active");
      }
      connLocal.send(connIp);
    } else if (message.startsWith("CHUNK-ID~")) {
      int chunkId = Integer.parseInt(message.substring(9));
      ByteBuffer byteBuffer = this.fileHandler.getChunkById(chunkId);
      conn.send(byteBuffer);
    } else if (message.startsWith("webserver/")) {
      String httpMessage = message.substring(10);
      if (httpMessage.startsWith("metadata/")) {
        String json = httpMessage.substring(9);
        ObjectMapper mapper = new ObjectMapper();

        try {
          Context context = mapper.readValue(json, Context.class);
          List<FileAccessInfo> listFai = context.getListFai();
          this.listFcm = context.getListFcm();

          this.fileCounter = 0;
          this.chunkCounter = 0;

          this.readyToReceive = true;

          // logger.info("fcm chunk maps size is: " +
          // this.listFcm.get(0).getMapsOfChunk().size());

          Map<Integer, byte[]> mapOfChunks = new HashMap<>();

          this.listFcm.get(this.fileCounter).setMapsOfChunk(mapOfChunks);

          this.connLocal.send("file~" + this.listFcm.get(this.fileCounter).getUuid() +
              "CHUNK-ID~" + this.chunkCounter);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    if (message.startsWith("file~")) {
      String[] parts = message.split("CHUNK-ID~");

      String leftPart = parts[0]; // "file~5"
      String rightPart = parts[1]; // "10"

      int fileCounter = Integer.parseInt(leftPart.split("~")[1]);
      int chunkId = Integer.parseInt(rightPart);
      System.out.println("fileCounter at client is: " + fileCounter);
      System.out.println("chunkId at client is: " + chunkId);
      ByteBuffer byteBuffer = ByteBuffer.wrap(this.listOfChunkMaps.get(fileCounter).get(chunkId));
      connLocalClient.send(byteBuffer);
    }
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer buffer) {
    if (this.readyToReceive) {
      byte[] data = buffer.array();

      FileChunkMetadata tempFcm = this.listFcm.get(this.fileCounter);
      Map<Integer, byte[]> tempMapOfChunks = tempFcm.getMapsOfChunk();

      tempMapOfChunks.put(this.chunkCounter, data);
      this.hasher.putBytes(data);

      this.chunkCounter++;

      if (tempMapOfChunks.size() == tempFcm.getChunkCount()) {
        if (this.fileCounter == this.listFcm.size()) {
          logger.info("All bytes received");
        } else {

          if (this.hasher.hash().toString().equals(tempFcm.getSignature())) {
            logger.info("File is safe. DONTW ORRY");
          } else {
            logger.info("file is CORRUPTED");
            return;
          }

          this.chunkCounter = 0;
          this.fileCounter++;
        }

      } else {
        this.connLocal.send("file~" + tempFcm.getUuid() + "CHUNK-ID~" + this.chunkCounter);
      }
    }
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

    this.fileHandler = new FileHandler(this);

    new Thread(() -> {
      logger.info("Command receiver thread started");
      while (true) {
        if (sc.hasNext()) {
          String command = sc.nextLine();
          // logger.info("The command is : " + command);
          switch (command) {
            case "ping":
              broadcast("PING");
              break;

            case "ping 1":
              pingLab(l1);
              break;

            case "ping 2":
              pingLab(l2);
              break;

            case "ping 3":
              pingLab(l3);
              break;

            case "ping 4":
              pingLab(l4);
              break;

            case "ping 0":

            case "ping 5":
              pingLocal();
              break;

            case "send":
              File file = new File("files/T06xxyyy.zip");
              this.fileHandler.setFile(file);
              this.fileHandler.filePreProcess();
              this.fileHandler.sendFileMetadata();
              break;

            case "q":
              try {
                this.stop();
              } catch (Exception e) {
                System.out.println(e.getMessage());
              }
              // case "delete":
              // String toDeletePath = "";
              // this.fileHandler
          }
        }
      }
    }).start();
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    String connIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();

    if (connIp.startsWith("10.100.71")) {
      l1.closeClientConnection(connIp);
    } else if (connIp.startsWith("10.100.72")) {
      l2.closeClientConnection(connIp);
    } else if (connIp.startsWith("10.100.73")) {
      l3.closeClientConnection(connIp);
    } else if (connIp.startsWith("10.100.74")) {
      l4.closeClientConnection(connIp);
    } else {
      connLocal = null;
      connLocalClient = null;
      logger.info(connIp + " has disconnected");
    }
  }

  public void pingLab(BaseLab lab) {
    ConcurrentHashMap<String, Client> clients = lab.getClients();
    Iterator<Map.Entry<String, Client>> iterator = clients.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Client> entry = iterator.next();
      WebSocket conn = entry.getValue().getConn();
      if (conn != null) {
        conn.send("PING");
      }
      // else: tampilkan yang belum terhubung
    }
  }

  public void pingLocal() {
    this.connLocal.send("PING");
  }

  public void clearAllListAndMap() {
    for (Map<Integer, byte[]> map : this.listOfChunkMaps) {
      map.clear();
    }
    this.listOfChunkMaps.clear();
    this.listFileMetadata.clear();
    this.listPath.clear();
    this.listFos.clear();
    this.fileCounter = 0;
    this.chunkCounter = 0;
    this.entryId = null;
    this.title = "";
  }

}
