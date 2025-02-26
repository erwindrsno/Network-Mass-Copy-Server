package org.example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import org.junit.jupiter.api.Test;

import com.google.common.hash.*;

public class FileIntegrityTest {
    @Test
    public void hashingFileT06() {
        Path filePath = Paths.get("T06xxyyy.zip");

        try {
            byte[] fileBytes = Files.readAllBytes(filePath);

            String hashedBytes = Hashing.sha256().hashBytes(fileBytes).toString();
            String hashed2 = Hashing.sha256().hashBytes(fileBytes).toString();
            System.out.println("Hashed is : " + hashedBytes);

            System.out.println("The length is: " + hashedBytes.length());

            // System.out.println("First byte is : " + hashedBytes[0]);

            // assertArrayEquals(hashedBytes, hashed2);
            assertEquals(hashedBytes, hashed2 + "aaa");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void hashingFileClient() {
        Path filePath = Paths.get("Client.zip");

        try {
            byte[] fileBytes = Files.readAllBytes(filePath);

            byte[] hashedBytes = Hashing.sha256().hashBytes(fileBytes).asBytes();
            System.out.println("Hashed CLIENT is : " + hashedBytes);

            System.out.println("The length is: " + hashedBytes.length);

            System.out.println("First byte is : " + hashedBytes[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void hashingFileArchive() {
        Path filePath = Paths.get("archive.zip");

        try {
            byte[] fileBytes = Files.readAllBytes(filePath);

            byte[] hashedBytes = Hashing.sha256().hashBytes(fileBytes).asBytes();
            System.out.println("Hashed archive is : " + hashedBytes);

            System.out.println("The length is: " + hashedBytes.length);

            System.out.println("First byte is : " + hashedBytes[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
