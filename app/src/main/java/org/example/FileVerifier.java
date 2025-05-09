package org.example;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class FileVerifier {
  private Hasher hasher;

  public FileVerifier() {
    this.hasher = Hashing.sha256().newHasher();
  }

  public boolean verifyHashedBytes(String hashedA, String hashedB) {
    return hashedA.equals(hashedB) ? true : false;
  }
}
