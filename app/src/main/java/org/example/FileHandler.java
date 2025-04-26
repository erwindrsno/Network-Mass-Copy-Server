package org.example;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.nio.file.attribute.AclEntryPermission;

import org.slf4j.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;

import org.example.model.Acl;
import org.example.model.FileMetadata;

// Multifile sending
// 1. Memastikan jumlah file yang ingin dikirim
// 2. variabel jumlah file tsb akan ditaruh di metadata agar client dapat menyiapkan fileinpustream sesuai
// dengan jumlah yang dibutuhkan
// 3. Mekanisme protokol harus diubah sedikit.

public class FileHandler {
  Server server;
  int CHUNK_SIZE = 10240;
  Logger logger = LoggerFactory.getLogger(FileHandler.class);
  File file;

  Map<Integer, byte[]> bytesMap = new HashMap<>();

  // Acl aclFetch = new Acl();

  public FileHandler(Server server) {
    this.server = server;
  }

  public void startSend() {
    logger.info("SENDING FILE");

    try (FileInputStream fileInputStream = new FileInputStream(this.file)) {
      byte[] buffer = new byte[CHUNK_SIZE];
      int bytesRead;

      while ((bytesRead = fileInputStream.read(buffer)) != -1) {
        byte[] copy = Arrays.copyOf(buffer, bytesRead);
        ByteBuffer byteBuffer = ByteBuffer.wrap(copy);
        this.server.broadcast(byteBuffer);
      }
      logger.info("File sent");
    } catch (Exception e) {
      logger.error("Error at sending file");
      System.err.println(e);
    }
  }

  public void sendFileMetadata() {
    if (!this.file.exists()) {
      logger.error("File not found");
      return;
    }
    logger.info("SENDING FILE META DATA");
    try {
      Path filePath = Paths.get("files/" + this.file.getName());

      // hashing
      String signature = Hashing.sha256().hashBytes(Files.readAllBytes(filePath)).toString();

      // generate ACL
      Set<AclEntryPermission> aclEntry = Acl.getRWXAcl();

      FileMetadata fileMetadata = new FileMetadata(this.file.length(), this.CHUNK_SIZE, this.bytesMap.size(),
          this.file.getName(),
          "i20002", signature, aclEntry);
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(fileMetadata);
      this.server.broadcast("FILE-METADATA~" + json);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void filePreProcess() {
    try (FileInputStream fileInputStream = new FileInputStream(this.file)) {
      byte[] buffer = new byte[CHUNK_SIZE];
      int bytesRead;

      int idx = 0;
      while ((bytesRead = fileInputStream.read(buffer)) != -1) {
        byte[] copy = Arrays.copyOf(buffer, bytesRead);
        this.bytesMap.put(idx, copy);
        idx++;
      }
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  public void setFile(File file) {
    this.file = file;
  }

  public ByteBuffer getChunkById(int chunkId) {
    try {
      ByteBuffer byteBuffer = ByteBuffer.wrap(this.bytesMap.get(chunkId));
      return byteBuffer;
    } catch (Exception e) {
      logger.error(e.getMessage());
      return null;
    }
  }
}
