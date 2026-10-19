package com.chatapplauncher.launcher.model;

import com.chatapplauncher.launcher.config.LauncherConfig;

import lombok.Getter;

public enum OperatingSystem {
    WINDOWS(LauncherConfig.ASSET_WINDOWS, LauncherConfig.APP_NAME + ".exe"),
    MAC(LauncherConfig.ASSET_MAC, LauncherConfig.APP_NAME + ".app/Contents/MacOS/" + LauncherConfig.APP_NAME),
    LINUX(LauncherConfig.ASSET_LINUX, "bin/" + LauncherConfig.APP_NAME);

    @Getter
    private final String assetName;
    @Getter
    private final String executablePath;

    OperatingSystem(String assetName, String executablePath){
        this.assetName = assetName;
        this.executablePath = executablePath;
    }

    public String getChecksumFileName(){
        return assetName + LauncherConfig.CHECKSUM_SUFFIX;
    }
}