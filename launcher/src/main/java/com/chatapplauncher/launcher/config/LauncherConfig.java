package com.chatapplauncher.launcher.config;

import java.nio.file.Path;

public final class LauncherConfig {
    public static final String REPO_OWNER = "BjorniPrrini";
    public static final String REPO_NAME = "ChatApp";

    public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";

    public static final Path BASE_PATH = Path.of(System.getProperty("user.home"), ".bpchat", "app");

    public static final Path LIVE_PATH = BASE_PATH.resolve("frontend");
    public static final Path NEW_PATH = BASE_PATH.resolve("frontend-new");
    public static final Path OLD_PATH = BASE_PATH.resolve("frontend-old");

    public static final Path TEMP_DOWNLOAD = BASE_PATH.resolve("download.part");
    public static final String MARKER_FILE_NAME = "update-complete.txt";

    public static final String ASSET_WINDOWS = "bpchat-windows.zip";
    public static final String ASSET_LINUX = "bpchat-linux.zip";
    public static final String ASSET_MAC = "bpchat-mac.zip";

    public static final String CHECKSUM_SUFFIX = ".sha256";

    public static final String APP_NAME = "BP-Chat";

    private LauncherConfig() {}
}