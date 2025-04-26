package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

public class FileTransferTwoTest {
  File file = new File("files/T06xxyyy.zip");
  long fileSize = file.length();
  final int CHUNK_SIZE = 10240;
  Map<Integer, byte[]> bytesMap = new HashMap<>();
  Logger logger = LoggerFactory.getLogger(FileTransferTwoTest.class);

  @Test
  public void chunkFileBeforeTransfer() {
    try (FileInputStream fileInputStream = new FileInputStream(this.file)) {
      byte[] buffer = new byte[CHUNK_SIZE];
      int bytesRead;

      int idx = 0;
      while ((bytesRead = fileInputStream.read(buffer)) != -1) {
        byte[] copy = Arrays.copyOf(buffer, bytesRead);
        this.bytesMap.put(idx, copy);
        idx++;
      }
      System.out.println(bytesMap.size() + " yeyeyyeyeyey");
      // logger.info(bytesMap.size() + "");
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
