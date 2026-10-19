package com.chatapplauncher.launcher.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionComparator {
    private static final Pattern FORMAT = Pattern.compile("^[vV]?(\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})$");

    private VersionComparator() {}

    public static boolean isNewer(String latest, String installed){
        Optional<int[]> latestParts = parse(latest);
        Optional<int[]> installedParts = parse(installed);

        if(latestParts.isEmpty() || installedParts.isEmpty()){
            return false;
        }

        int[] l = latestParts.get();
        int[] i = installedParts.get();

        for(int index = 0; index < 3; index++){
            if(l[index] != i[index]){
                return l[index] > i[index];
            }
        }

        return false;
    }

    public static boolean isValid(String tag){
        return parse(tag).isPresent();
    }

    private static Optional<int[]> parse(String tag){
        if(tag == null){
            return Optional.empty();
        }

        Matcher matcher = FORMAT.matcher(tag.trim());

        if(!matcher.matches()){
            return Optional.empty();
        }

        return Optional.of(new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))});
    }
}