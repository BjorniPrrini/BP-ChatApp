package com.chatapplauncher.launcher.service;

import com.chatapplauncher.launcher.config.LauncherConfig;
import com.chatapplauncher.launcher.model.OperatingSystem;
import com.chatapplauncher.launcher.util.PlatformUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class FrontendStarter {
    private static final long STARTUP_CHECK_SECONDS = 2;
    private static final Path LOG_FILE = LauncherConfig.BASE_PATH.resolveSibling("frontend.log");

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

        Files.createDirectories(LOG_FILE.getParent());

        ProcessBuilder builder = new ProcessBuilder(path.toString())
                .directory(LauncherConfig.LIVE_PATH.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.to(LOG_FILE.toFile()));

        removeParentJvmSettings(builder.environment());

        Process process = builder.start();

        try {
            if(process.waitFor(STARTUP_CHECK_SECONDS, TimeUnit.SECONDS)){
                throw new IOException("BP-Chat closed right after starting (exit code " + process.exitValue() + "). See " + LOG_FILE);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private static void removeParentJvmSettings(Map<String, String> environment){
        environment.keySet().removeIf(name -> {
            String upper = name.toUpperCase(Locale.ROOT);

            return upper.equals("JAVA_TOOL_OPTIONS")
                    || upper.equals("JDK_JAVA_OPTIONS")
                    || upper.equals("_JAVA_OPTIONS")
                    || upper.equals("CLASSPATH")
                    || upper.startsWith("JAVA_MAIN_CLASS_")
                    || upper.contains("JPACKAGE");
        });
    }
}