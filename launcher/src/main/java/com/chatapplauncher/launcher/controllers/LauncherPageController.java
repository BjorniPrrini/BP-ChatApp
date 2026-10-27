package com.chatapplauncher.launcher.controllers;

import com.chatapplauncher.launcher.manager.UpdateManager;
import com.chatapplauncher.launcher.model.LauncherState;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LauncherPageController {
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator spinner;
    @FXML
    private VBox progressBox;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label percentLabel;
    @FXML
    private HBox failedBox;

    private final UpdateManager updateManager = new UpdateManager();

    @FXML
    public void initialize(){
        render(new LauncherState.Checking());

        startUpdate();
    }

    @FXML
    private void onRetryClicked(){
        render(new LauncherState.Checking());

        startUpdate();
    }

    private void render(LauncherState state){
        hideAllStateControls();

        switch (state) {
            case LauncherState.Checking _ -> {
                showStateComponent(spinner);

                statusLabel.setText("Searching for update...");
            }
            case LauncherState.Downloading (double fraction) -> {
                showStateComponent(progressBox);

                statusLabel.setText("Downloading...");
                progressBar.setProgress(fraction);
                percentLabel.setText(Math.round(fraction * 100) + "%");
            }
            case LauncherState.Installing _ -> {
                showStateComponent(spinner);

                statusLabel.setText("Installing...");
            }
            case LauncherState.Failed (String errorMessage) -> {
                showStateComponent(failedBox);

                statusLabel.setText(errorMessage);
            }
        }
    }

    private void startUpdate(){
        Thread thread = new Thread(() -> {
            try {
                updateManager.run(state -> Platform.runLater(() -> render(state)), () -> Platform.runLater(Platform::exit));
            } catch (Exception e) {
                if(e.getMessage() != null){
                    Platform.runLater(() -> render(new LauncherState.Failed(e.getMessage())));
                }else{
                    Platform.runLater(() -> render(new LauncherState.Failed("Update failed")));
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void hideAllStateControls(){
        spinner.setManaged(false);
        spinner.setVisible(false);
        progressBox.setManaged(false);
        progressBox.setVisible(false);
        failedBox.setManaged(false);
        failedBox.setVisible(false);
    }

    private void showStateComponent(Node component){
        component.setManaged(true);
        component.setVisible(true);
    }
}