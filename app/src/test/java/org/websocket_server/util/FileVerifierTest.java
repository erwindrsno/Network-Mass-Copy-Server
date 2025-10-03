package org.websocket_server.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FileVerifierTest {

  private FileVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new FileVerifier();
  }

  @Test
  void testVerifyCorrectHash() {
    // prepare hasher
    verifier.prepare();
    byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
    verifier.putBytes(data);

    // calculate expected hash manually
    String expectedHash = com.google.common.hash.Hashing.sha256()
        .hashBytes(data)
        .toString();

    assertTrue(verifier.verifyHashedBytes(expectedHash));
  }

  @Test
  void testVerifyWrongHash() {
    verifier.prepare();
    verifier.putBytes("hello".getBytes(StandardCharsets.UTF_8));

    String wrongHash = "00000000000000000000000000000000";
    assertFalse(verifier.verifyHashedBytes(wrongHash));
  }

  @Test
  void testClearResetsHasher() {
    verifier.prepare();
    verifier.putBytes("hello".getBytes(StandardCharsets.UTF_8));

    verifier.clear();

    assertThrows(NullPointerException.class, () -> verifier.verifyHashedBytes("something"));
  }

  @Test
  void testVerifyWithoutPrepareThrows() {
    assertThrows(NullPointerException.class, () -> verifier.verifyHashedBytes("dummy"));
  }
}
