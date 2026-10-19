package com.chatapplauncher.launcher.service;

import com.chatapplauncher.launcher.config.LauncherConfig;
import com.chatapplauncher.launcher.util.VersionComparator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class InstallStore {
    public void recovery() throws IOException {
        if(Files.exists(LauncherConfig.NEW_PATH)){
            if(readMarker(LauncherConfig.NEW_PATH).isPresent()){
                applyUpdate();

                return;
            }

            deleteRecursively(LauncherConfig.NEW_PATH);
        }

        boolean liveFolder = Files.exists(LauncherConfig.LIVE_PATH);
        boolean oldFolder = Files.exists(LauncherConfig.OLD_PATH);

        if(!liveFolder && oldFolder){
            move(LauncherConfig.OLD_PATH, LauncherConfig.LIVE_PATH);
        }else if(oldFolder){
            deleteRecursively(LauncherConfig.OLD_PATH);
        }
    }

    public void applyUpdate() throws IOException {
        if(readMarker(LauncherConfig.NEW_PATH).isEmpty()){
            throw new IOException("No complete update found");
        }

        if(Files.exists(LauncherConfig.LIVE_PATH)){
            deleteRecursively(LauncherConfig.OLD_PATH);

            move(LauncherConfig.LIVE_PATH, LauncherConfig.OLD_PATH);
        }

        move(LauncherConfig.NEW_PATH, LauncherConfig.LIVE_PATH);

        try {
            deleteRecursively(LauncherConfig.OLD_PATH);
        } catch (IOException _) {

        }
    }

    public Path prepareNewFolder() throws IOException {
        deleteRecursively(LauncherConfig.NEW_PATH);

        Files.createDirectories(LauncherConfig.NEW_PATH);

        return LauncherConfig.NEW_PATH;
    }

    public void markNewFolderComplete(String version) throws IOException {
        Files.writeString(LauncherConfig.NEW_PATH.resolve(LauncherConfig.MARKER_FILE_NAME), version);
    }

    public Optional<String> getInstalledVersion(){
        return readMarker(LauncherConfig.LIVE_PATH);
    }

    private void deleteRecursively(Path folder) throws IOException {
        if(!Files.exists(folder)){
            return;
        }

        try (Stream<Path> paths = Files.walk(folder)) {
            for(Path path : paths.sorted(Comparator.reverseOrder()).toList()){
                Files.delete(path);
            }
        }
    }

    private Optional<String> readMarker(Path folder){
        try {
            String version = Files.readString(folder.resolve(LauncherConfig.MARKER_FILE_NAME)).trim();

            return VersionComparator.isValid(version) ? Optional.of(version) : Optional.empty();
        } catch (IOException _) {
            return Optional.empty();
        }
    }

    private void move(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
    }
}