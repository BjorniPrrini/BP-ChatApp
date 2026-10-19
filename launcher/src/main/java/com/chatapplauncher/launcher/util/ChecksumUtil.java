package com.chatapplauncher.launcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ChecksumUtil {
    private ChecksumUtil() {}

    public static String sha256(Path path) throws IOException {
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException _) {
            throw new IllegalStateException("SHA-256 is not available");
        }

        try (InputStream stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];

            while(true){
                int numberOfBytes = stream.read(buffer);

                if(numberOfBytes == -1){
                    break;
                }

                digest.update(buffer, 0, numberOfBytes);
            }

            byte[] bytes = digest.digest();

            return HexFormat.of().formatHex(bytes);
        }
    }
}