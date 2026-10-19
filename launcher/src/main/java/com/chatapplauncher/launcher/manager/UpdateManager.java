package com.chatapplauncher.launcher.manager;

import com.chatapplauncher.launcher.config.LauncherConfig;
import com.chatapplauncher.launcher.model.LauncherState;
import com.chatapplauncher.launcher.model.ReleaseInfo;
import com.chatapplauncher.launcher.service.*;
import com.chatapplauncher.launcher.util.VersionComparator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class UpdateManager {
    private final InstallStore installStore = new InstallStore();
    private final ReleaseClient releaseClient = new ReleaseClient();
    private final Downloader downloader = new Downloader();

    public void run(Consumer<LauncherState> onStateChange, Runnable onLaunched){
        onStateChange.accept(new LauncherState.Checking());

        try {
            if(!recoverSafely()){
                if(installStore.getInstalledVersion().isPresent()){
                    launchFrontend(onLaunched);
                }else{
                    onStateChange.accept(new LauncherState.Failed("The installation could not be repaired"));
                }

                return;
            }

            Optional<String> installedVersion = installStore.getInstalledVersion();
            Optional<ReleaseInfo> latestRelease = releaseClient.fetchLatestRelease();

            if(latestRelease.isEmpty() && installedVersion.isPresent()){
                launchFrontend(onLaunched);

                return;
            }

            if(latestRelease.isEmpty()){
                onStateChange.accept(new LauncherState.Failed("Could not reach the update server"));

                return;
            }

            if(installedVersion.isPresent() && !VersionComparator.isNewer(latestRelease.get().getVersion(), installedVersion.get())){
                launchFrontend(onLaunched);

                return;
            }

            ReleaseInfo releaseInfo = latestRelease.get();

            onStateChange.accept(new LauncherState.Downloading(0.0));

            AtomicInteger lastPercentage = new AtomicInteger(-1);

            downloader.download(releaseInfo, progress -> {
                int percentage = (int) Math.round(progress  * 100);

                if(percentage == lastPercentage.get()){
                    return;
                }

                lastPercentage.set(percentage);

                onStateChange.accept(new LauncherState.Downloading(progress));
            });

            onStateChange.accept(new LauncherState.Installing());

            Path preparedFolder = installStore.prepareNewFolder();

            Installer.unzip(LauncherConfig.TEMP_DOWNLOAD, preparedFolder);

            installStore.markNewFolderComplete(releaseInfo.getVersion());

            installStore.applyUpdate();

            deleteDownloadedZip();

            launchFrontend(onLaunched);
        } catch (IOException e) {
            if(e.getMessage() == null){
                onStateChange.accept(new LauncherState.Failed("Update failed"));
            }else{
                onStateChange.accept(new LauncherState.Failed(e.getMessage()));
            }
        }
    }

    private void launchFrontend(Runnable onLaunched) throws IOException {
        FrontendStarter.start();

        onLaunched.run();
    }

    private boolean recoverSafely(){
        try {
            installStore.recovery();

            return true;
        } catch (IOException _) {
            return false;
        }
    }

    private void deleteDownloadedZip(){
        try {
            Files.deleteIfExists(LauncherConfig.TEMP_DOWNLOAD);
        } catch (IOException _) {

        }
    }
}