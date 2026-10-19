package com.chatapplauncher.launcher.util;

import com.chatapplauncher.launcher.model.OperatingSystem;

import java.util.Locale;
import java.util.Optional;

public final class PlatformUtil {
    private PlatformUtil() {}

    public static Optional<OperatingSystem> getCurrentOS(){
        String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if(operatingSystem.startsWith("windows")){
            return Optional.of(OperatingSystem.WINDOWS);
        }else if(operatingSystem.startsWith("mac")){
            return Optional.of(OperatingSystem.MAC);
        }else if(operatingSystem.startsWith("linux")){
            return Optional.of(OperatingSystem.LINUX);
        }

        return Optional.empty();
    }
}