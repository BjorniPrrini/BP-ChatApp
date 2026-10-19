package com.chatapplauncher.launcher.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Installer {
    private Installer() {}

    public static void unzip(Path zipFile, Path unzipFolder) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            Path root = unzipFolder.normalize();

            while(zipEntry != null){
                Path target = root.resolve(zipEntry.getName()).normalize();

                if(!target.startsWith(root)){
                    throw new IOException("Unsafe path in update");
                }

                if(zipEntry.isDirectory()){
                    Files.createDirectories(target);
                }else{
                    Files.createDirectories(target.getParent());

                    Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                }

                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }
}