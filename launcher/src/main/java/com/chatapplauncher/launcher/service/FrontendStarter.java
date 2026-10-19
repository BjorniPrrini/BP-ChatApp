package com.chatapplauncher.launcher.service;

import com.chatapplauncher.launcher.config.LauncherConfig;
import com.chatapplauncher.launcher.model.OperatingSystem;
import com.chatapplauncher.launcher.util.PlatformUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FrontendStarter {
    private FrontendStarter() {}

    public static void start() throws IOException {
        Optional<OperatingSystem> operatingSystem = PlatformUtil.getCurrentOS();

        if(operatingSystem.isEmpty()){
            throw new IOException("Unable to retrieve operating system");
        }

        Path path = LauncherConfig.LIVE_PATH.resolve(operatingSystem.get().getExecutablePath());

        if(!Files.exists(path)){
            throw new IOException("Install path does not exist: " + path);
        }

        path.toFile().setExecutable(true);

        new ProcessBuilder(path.toString())
                .directory(LauncherConfig.LIVE_PATH.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
    }
}