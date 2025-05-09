package org.example;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntryPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.model.Acl;
import org.example.model.FileMetadata;
import org.example.model.Metadata;
import org.java_websocket.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;

public class MultipleFileHandler {
  Server server;
  private String title;
  private Integer entryId;
  private List<FileMetadata> listFileMetadata;
  private List<Map<Integer, byte[]>> listOfChunkMaps;
  private WebSocket connLocalClient;

  public MultipleFileHandler(String title, Integer entryId, List<FileMetadata> listFileMetadata,
      List<Map<Integer, byte[]>> listOfChunkMaps, Server server, WebSocket connLocalClient) {
    this.title = title;
    this.entryId = entryId;
    this.listFileMetadata = listFileMetadata;
    this.listOfChunkMaps = listOfChunkMaps;
    this.server = server;
    this.connLocalClient = connLocalClient;
  }

  public void prepareMetadata() {
    Path basePath = Paths.get("files/" + entryId);
    System.out.println("entered prepareMetadata in MultipleFileHandler");

    try {
      Files.walk(basePath).forEach(path -> {
        if (!Files.isDirectory(path)) {
          try (FileInputStream fileInputStream = new FileInputStream(path.toFile())) {
            byte[] buffer = new byte[10240];
            int bytesRead;

            Map<Integer, byte[]> chunks = new HashMap<>();

            int idx = 0;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
              byte[] copy = Arrays.copyOf(buffer, bytesRead);
              chunks.put(idx, copy);
              idx++;
            }
            listOfChunkMaps.add(chunks);
          } catch (Exception e) {
            e.printStackTrace();
          }

          try {
            String signature = Hashing.sha256().hashBytes(Files.readAllBytes(path)).toString();
            String filename = path.getFileName().toString();
            int chunkSize = 10240;

            long fileSize = Files.size(path);
            int chunkCount = (int) ((fileSize + chunkSize - 1) / chunkSize);

            Set<AclEntryPermission> aclEntry = Acl.getRWXAcl();

            this.listFileMetadata.add(new FileMetadata(chunkSize, chunkCount, filename,
                signature, "test"));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
      Metadata metadata = new Metadata(listFileMetadata, title, entryId);
      // this.wsClient.setListOfChunkMaps(listOfChunkMaps);
      // this.wsClient.setListFileMetadata(listFileMetadata);
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(metadata);
      this.connLocalClient.send("server/metadata/" + json);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
