package com.chatapplauncher.launcher.model;

public sealed interface LauncherState permits LauncherState.Checking, LauncherState.Downloading, LauncherState.Installing, LauncherState.Failed{
    record Checking() implements LauncherState {}
    record Downloading(double progress) implements LauncherState {}
    record Installing() implements LauncherState {}
    record Failed(String errorMessage) implements LauncherState {}
}