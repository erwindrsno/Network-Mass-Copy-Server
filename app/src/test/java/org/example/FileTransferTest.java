package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

public class FileTransferTest {
    File file = new File("files/T06xxyyy.zip");
    long fileSize = file.length();
    long chunkSize = 10240;

    byte[] fileBuffer = new byte[(int) file.length()];
    int currIdx = 0;

    @Test
    public void transfer() {
        if (!file.exists()) {
            fail("File T06xxyyy.zip does not exist sadly");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) chunkSize];

            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);

                broadcast(byteBuffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void broadcast(ByteBuffer buffer) {
        byte[] data = new byte[buffer.remaining()];

        buffer.get(data);

        System.arraycopy(data, 0, fileBuffer, currIdx, data.length);
        currIdx += data.length;

        if (currIdx != fileBuffer.length) {
            // System.out.println("corrupt");
        } else {
            System.out.println("OK");
        }
    }
}
