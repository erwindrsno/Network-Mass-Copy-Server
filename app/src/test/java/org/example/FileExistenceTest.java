package org.example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import org.junit.jupiter.api.Test;

import com.google.common.hash.*;
import java.io.File;

public class FileExistenceTest {
    @Test
    public void hashingFileT06() {
        File file = new File("files/T06xxyyy.zip");

        assertEquals(true, file.exists());
    }
}
