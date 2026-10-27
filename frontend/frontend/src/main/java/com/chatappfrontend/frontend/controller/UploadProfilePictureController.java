package com.chatappfrontend.frontend.controller;

import com.chatappfrontend.frontend.model.UserResponseDTO;
import com.chatappfrontend.frontend.service.UserService;
import com.chatappfrontend.frontend.util.AlertUtils;
import com.chatappfrontend.frontend.util.AppExecutor;
import com.chatappfrontend.frontend.util.AvatarUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public class UploadProfilePictureController {
    @FXML
    private Label profilePictureView;
    @FXML
    private Button changePictureButton;
    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();
    private File selectedProfilePicture;

    public void loadProfilePicture(){
        try {
            UserResponseDTO user = userService.getUserInformation();

            String initials = user.getName().substring(0, 1) + user.getSurname().substring(0, 1);

            AvatarUtils.applyAvatar(profilePictureView, user.getProfilePicture(), initials, false, 300);
        } catch (Exception _) {
            AlertUtils.showError(errorLabel, "Couldn't load profile picture");
        }
    }

    @FXML
    public void handleChangePicture(){
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Select profile picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Window owner = changePictureButton.getScene().getWindow();

        File selectedFile = fileChooser.showOpenDialog(owner);

        if(selectedFile == null){
            return;
        }

        selectedProfilePicture = selectedFile;

        Image image = new Image(selectedFile.toURI().toString(), 300, 300, false, true);

        ImageView imageView = new ImageView(image);

        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(false);

        Circle clip = new Circle(150, 150, 150);

        imageView.setClip(clip);

        profilePictureView.setText(null);
        profilePictureView.setGraphic(imageView);
    }

    @FXML
    public void handleUpload(){
        if(selectedProfilePicture == null){
            AlertUtils.showError(errorLabel, "Choose a picture first");

            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.updateProfilePicture(selectedProfilePicture);

                return null;
            }
        };

        task.setOnFailed(_ -> AlertUtils.showError(errorLabel, "Couldn't upload profile picture"));

        AppExecutor.run(task);
    }
}