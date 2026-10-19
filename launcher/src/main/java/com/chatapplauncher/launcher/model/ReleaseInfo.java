package com.chatapplauncher.launcher.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReleaseInfo {
    private String version;
    private String downloadLink;
    private long downloadSize;
    private String checksum;
}