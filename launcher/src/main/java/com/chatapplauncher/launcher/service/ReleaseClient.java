package com.chatapplauncher.launcher.service;

import com.chatapplauncher.launcher.config.LauncherConfig;
import com.chatapplauncher.launcher.model.OperatingSystem;
import com.chatapplauncher.launcher.model.ReleaseInfo;
import com.chatapplauncher.launcher.util.PlatformUtil;
import com.chatapplauncher.launcher.util.VersionComparator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReleaseClient {
    private static final String ACCEPT_GITHUB_JSON = "application/vnd.github+json";
    private static final String ACCEPT_ANY = "*/*";
    private static final String APP_NAME = "ChatApp";
    private static final Pattern SHA256_FORMAT = Pattern.compile("[0-9a-f]{64}");

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<ReleaseInfo> fetchLatestRelease(){
        Optional<OperatingSystem> operatingSystem = PlatformUtil.getCurrentOS();

        if(operatingSystem.isEmpty()){
            return Optional.empty();
        }

        Optional<String> json = fetchText(LauncherConfig.LATEST_RELEASE_URL, ACCEPT_GITHUB_JSON);

        if(json.isEmpty()){
            return Optional.empty();
        }

        try {
            JsonNode node = objectMapper.readTree(json.get());


            String version = node.path("tag_name").asText("");

            if(!VersionComparator.isValid(version)){
                return Optional.empty();
            }

            JsonNode assets = node.path("assets");

            Optional<JsonNode> zipAsset = findAsset(assets, operatingSystem.get().getAssetName());

            if(zipAsset.isEmpty()){
                return Optional.empty();
            }

            Optional<JsonNode> checksumAsset = findAsset(assets, operatingSystem.get().getChecksumFileName());

            if(checksumAsset.isEmpty()){
                return Optional.empty();
            }

            String downloadLink = zipAsset.get().path("browser_download_url").asText("");
            String checksumLink = checksumAsset.get().path("browser_download_url").asText("");
            long downloadSize = zipAsset.get().path("size").asLong(0);

            if(downloadLink.isEmpty() || checksumLink.isEmpty() || downloadSize <= 0){
                return Optional.empty();
            }

            Optional<String> checksumText = fetchText(checksumLink, ACCEPT_ANY);

            if(checksumText.isEmpty()){
                return Optional.empty();
            }

            String[] hashArray = checksumText.get().trim().split("\\s+");

            String checksum = hashArray[0].toLowerCase(Locale.ROOT);

            Matcher isValidFormat = SHA256_FORMAT.matcher(checksum);

            if(!isValidFormat.matches()){
                return Optional.empty();
            }

            return Optional.of(new ReleaseInfo(version, downloadLink, downloadSize, checksum));
        } catch (JsonProcessingException _) {
            return Optional.empty();
        }
    }

    private Optional<String> fetchText(String address, String acceptHeader){
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(address))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", APP_NAME)
                .header("Accept", acceptHeader)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200){
                return Optional.empty();
            }

            return Optional.of(response.body());
        } catch (IOException _) {
            return Optional.empty();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();

            return Optional.empty();
        }
    }

    private Optional<JsonNode> findAsset(JsonNode assetsArray, String assetName){
        for(JsonNode asset : assetsArray){
            if(asset.path("name").asText("").equals(assetName)){
                return Optional.of(asset);
            }
        }

        return Optional.empty();
    }
}