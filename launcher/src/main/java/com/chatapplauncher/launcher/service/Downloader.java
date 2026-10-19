package com.chatapplauncher.launcher.service;

import com.chatapplauncher.launcher.config.LauncherConfig;
import com.chatapplauncher.launcher.model.ReleaseInfo;
import com.chatapplauncher.launcher.util.ChecksumUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.function.Consumer;

public class Downloader {
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();

    public void download(ReleaseInfo releaseInfo, Consumer<Double> onProgress) throws IOException {
        Files.createDirectories(LauncherConfig.TEMP_DOWNLOAD.getParent());

        long alreadyDownloaded = 0;
        long fullSize = releaseInfo.getDownloadSize();

        if(Files.exists(LauncherConfig.TEMP_DOWNLOAD)){
            long partialSize = Files.size(LauncherConfig.TEMP_DOWNLOAD);

            if(partialSize > fullSize){
                Files.deleteIfExists(LauncherConfig.TEMP_DOWNLOAD);
            }else{
                alreadyDownloaded = partialSize;
            }
        }

        if(alreadyDownloaded == fullSize){
            if(isChecksumValid(releaseInfo)){
                return;
            }

            Files.deleteIfExists(LauncherConfig.TEMP_DOWNLOAD);

            alreadyDownloaded = 0;
        }

        fetchInto(releaseInfo, alreadyDownloaded, onProgress);

        if(isChecksumValid(releaseInfo)){
            return;
        }

        if(alreadyDownloaded > 0){
            Files.deleteIfExists(LauncherConfig.TEMP_DOWNLOAD);

            fetchInto(releaseInfo, 0, onProgress);

            if(isChecksumValid(releaseInfo)){
                return;
            }
        }

        Files.deleteIfExists(LauncherConfig.TEMP_DOWNLOAD);

        throw new IOException("The downloaded update was damaged");
    }

    private void fetchInto(ReleaseInfo releaseInfo, long alreadyDownloaded, Consumer<Double> onProgress) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(releaseInfo.getDownloadLink()))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "ChatApp")
                .header("Range", "bytes=" + alreadyDownloaded + "-")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            StandardOpenOption[] openOptions = new StandardOpenOption[2];

            long progressCount = 0;

            if(response.statusCode() == 200){
                openOptions[0] = StandardOpenOption.CREATE;
                openOptions[1] = StandardOpenOption.TRUNCATE_EXISTING;
            }else if(response.statusCode() == 206){
                openOptions[0] = StandardOpenOption.CREATE;
                openOptions[1] = StandardOpenOption.APPEND;

                progressCount = alreadyDownloaded;
            }else{
                response.body().close();

                throw new IOException("Download failed with status: " + response.statusCode());
            }

            try (InputStream inputStream = response.body(); OutputStream outputStream = Files.newOutputStream(LauncherConfig.TEMP_DOWNLOAD, openOptions)) {
                byte[] buffer = new byte[8192];
                long fullSize = releaseInfo.getDownloadSize();

                while(true){
                    int numberOfBytes = inputStream.read(buffer);

                    if(numberOfBytes == -1){
                        break;
                    }

                    outputStream.write(buffer, 0, numberOfBytes);

                    progressCount += numberOfBytes;

                    onProgress.accept(Math.min(1.0, (double) progressCount / fullSize));
                }
            }

            if(Files.size(LauncherConfig.TEMP_DOWNLOAD) < releaseInfo.getDownloadSize()){
                throw new IOException("Download was interrupted");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IOException("Failed to retrieve bytes", e);
        }
    }

    private boolean isChecksumValid(ReleaseInfo releaseInfo) throws IOException {
        return releaseInfo.getChecksum().equals(ChecksumUtil.sha256(LauncherConfig.TEMP_DOWNLOAD));
    }
}