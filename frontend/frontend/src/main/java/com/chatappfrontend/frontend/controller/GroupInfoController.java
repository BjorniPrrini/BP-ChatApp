package com.chatappfrontend.frontend.controller;

import com.chatappfrontend.frontend.cell.ParticipantCell;
import com.chatappfrontend.frontend.model.ConversationResponseDTO;
import com.chatappfrontend.frontend.model.ParticipantDTO;
import com.chatappfrontend.frontend.service.ConversationService;
import com.chatappfrontend.frontend.util.*;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import lombok.Setter;

import java.io.File;

public class GroupInfoController {
    @FXML
    private Label groupPictureView;
    @FXML
    private Button changePictureButton;
    @FXML
    private TextField groupNameField;
    @FXML
    private Button addParticipantButton;
    @FXML
    private ListView<ParticipantDTO> participantList;
    @FXML
    private Label errorLabel;
    @FXML
    private CheckBox allowInviteToggle;

    @Setter
    private Long currentConversationId;
    @Setter
    private Runnable onBack;
    @Setter
    private TriConsumer<Long, String, String> onGroupUpdated;
    @Setter
    private StackPane contentPane;

    private boolean isAdmin;
    private File selectedGroupPicture;
    private String currentGroupPicture;

    private final ConversationService conversationService = new ConversationService();

    public void loadGroupInformation(){
        try {
            ConversationResponseDTO conversationInfo = conversationService.getConversationById(currentConversationId);

            isAdmin = conversationService.isAdmin(currentConversationId);

            boolean canAdd = isAdmin || conversationInfo.isAllowParticipantsInvite();

            addParticipantButton.setVisible(canAdd);
            addParticipantButton.setManaged(canAdd);

            allowInviteToggle.setVisible(isAdmin);
            allowInviteToggle.setManaged(isAdmin);
            allowInviteToggle.setSelected(conversationInfo.isAllowParticipantsInvite());

            String groupPicture = conversationInfo.getGroupPicture();

            this.currentGroupPicture = groupPicture;

            if(groupPicture != null && !groupPicture.isBlank()){
                ImageCache.load(groupPicture, 300, image -> {
                    if(groupPicture.equals(this.currentGroupPicture)){
                        ImageView imageView = new ImageView(image);

                        imageView.setFitWidth(300);
                        imageView.setFitHeight(300);
                        imageView.setPreserveRatio(false);

                        Circle clip = new Circle(150, 150, 150);

                        imageView.setClip(clip);

                        groupPictureView.setGraphic(imageView);
                        groupPictureView.setText("");
                    }
                });
            }

            groupNameField.setText(conversationInfo.getGroupName());

            participantList.setCellFactory(_ -> new ParticipantCell(isAdmin,
                    participantId -> {
                        try {
                            conversationService.promoteToAdmin(currentConversationId, participantId);

                            loadGroupInformation();
                        } catch (Exception _) {
                            AlertUtils.showError(errorLabel, "Couldn't promote participant");
                        }
                    },
                    participantId -> {
                        try {
                            conversationService.demoteAdmin(currentConversationId, participantId);

                            loadGroupInformation();
                        } catch (Exception _) {
                            AlertUtils.showError(errorLabel, "Couldn't demote participant");
                        }
                    },
                    participantId -> {
                        try {
                            conversationService.kickParticipant(currentConversationId, participantId);

                            loadGroupInformation();
                        } catch (Exception _) {
                            AlertUtils.showError(errorLabel, "Couldn't kick participant");
                        }
                    }
            ));

            participantList.getItems().setAll(conversationInfo.getParticipants());
        } catch (Exception _) {
            AlertUtils.showError(errorLabel, "Couldn't load group information");
        }
    }

    @FXML
    public void handleBack(){
        onBack.run();
    }

    @FXML
    public void handleChangePicture(){
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Select group picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Window owner = changePictureButton.getScene().getWindow();

        File selectedFile = fileChooser.showOpenDialog(owner);

        if(selectedFile == null){
            return;
        }

        selectedGroupPicture = selectedFile;

        Image image = new Image(selectedFile.toURI().toString());

        ImageView imageView = new ImageView(image);

        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(false);

        Circle clip = new Circle(150, 150, 150);

        imageView.setClip(clip);

        groupPictureView.setGraphic(imageView);
    }

    @FXML
    public void handleAddParticipant(){
        try {
            AddParticipantsController controller = SceneManager.switchContent(contentPane, "add-participants-page.fxml");

            controller.setConversationId(currentConversationId);

            controller.loadFriendList();

            controller.setOnBack(() -> {
                try {
                    GroupInfoController groupInfoController = SceneManager.switchContent(contentPane, "group-info-page.fxml");

                    groupInfoController.setCurrentConversationId(currentConversationId);

                    groupInfoController.setContentPane(contentPane);

                    groupInfoController.setOnBack(onBack);

                    groupInfoController.loadGroupInformation();
                } catch (Exception _) {
                    AlertUtils.showError(errorLabel, "Couldn't go back");
                }
            });
        } catch (Exception _) {
            AlertUtils.showError(errorLabel, "Couldn't load add participants page");
        }
    }

    ConversationResponseDTO updatedConversation;

    @FXML
    public void handleSave(){
        String groupName = groupNameField.getText().trim();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updatedConversation = conversationService.updateGroupInfo(currentConversationId, groupName, selectedGroupPicture);

                return null;
            }
        };

        task.setOnSucceeded(_ -> {
            if(selectedGroupPicture != null){
                Image image = new Image(selectedGroupPicture.toURI().toString());

                ImageView imageView = new ImageView(image);

                imageView.setFitWidth(300);
                imageView.setFitHeight(300);
                imageView.setPreserveRatio(true);

                Circle clip = new Circle(150, 150, 150);

                imageView.setClip(clip);

                groupPictureView.setGraphic(imageView);
            }

            onGroupUpdated.accept(currentConversationId, groupName, selectedGroupPicture != null ? updatedConversation.getGroupPicture() : null);
        });

        task.setOnFailed(_ -> AlertUtils.showError(errorLabel, "Couldn't save changes"));

        AppExecutor.run(task);
    }

    @FXML
    public void handleToggleInvite(){
        try {
            boolean allowed = allowInviteToggle.isSelected();

            if(isAdmin){
                conversationService.allowParticipantsInvite(currentConversationId, allowed);
            }else{
                AlertUtils.showError(errorLabel, "You are not an admin");
            }
        } catch (Exception _) {
            AlertUtils.showError(errorLabel, "Couldn't change toggle");
        }
    }

    @FXML
    public void leaveGroup(){
        try {
            conversationService.leaveGroup(currentConversationId);

            onBack.run();
        } catch (Exception _) {
            AlertUtils.showError(errorLabel, "");
        }
    }
}