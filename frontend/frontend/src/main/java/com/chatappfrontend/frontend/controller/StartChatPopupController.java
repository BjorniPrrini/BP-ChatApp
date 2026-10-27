package com.chatappfrontend.frontend.controller;

import com.chatappfrontend.frontend.cell.SelectFriendCellDM;
import com.chatappfrontend.frontend.cell.SelectFriendCellGroup;
import com.chatappfrontend.frontend.model.ConversationResponseDTO;
import com.chatappfrontend.frontend.model.FriendResponseDTO;
import com.chatappfrontend.frontend.service.ConversationService;
import com.chatappfrontend.frontend.service.FriendService;
import com.chatappfrontend.frontend.util.AlertUtils;
import com.chatappfrontend.frontend.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StartChatPopupController {
    @FXML
    private VBox choiceView;
    @FXML
    private VBox singleSelectView;
    @FXML
    private ListView<FriendResponseDTO> singleSelectFriendList;
    @FXML
    private VBox multiSelectView;
    @FXML
    private Label selectedCountLabel;
    @FXML
    private ListView<FriendResponseDTO> multiSelectFriendList;
    @FXML
    private Button nextButton;
    @FXML
    private VBox groupDetailsView;
    @FXML
    private TextField groupNameField;
    @FXML
    private Label errorLabel;

    @Setter
    private Consumer<ConversationResponseDTO> onStartConversation;

    private final FriendService friendService = new FriendService();
    private final ConversationService conversationService = new ConversationService();

    private final Set<FriendResponseDTO> selectedFriends = new HashSet<>();

    @FXML
    public void initialize(){
        singleSelectFriendList.setCellFactory(_ -> new SelectFriendCellDM());
        multiSelectFriendList.setCellFactory(_ -> new SelectFriendCellGroup(selectedFriends, () -> {
            selectedCountLabel.setText(selectedFriends.size() + " selected");

            nextButton.setDisable(selectedFriends.size() < 2);
        }));

        singleSelectFriendList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        singleSelectFriendList.getSelectionModel()
                .selectedItemProperty()
                .addListener((_,_,newFriend) -> {
                    if(newFriend != null){
                        handleFriendSelected(newFriend);
                    }
                });

        multiSelectFriendList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        loadFriends();

        showView(choiceView);
    }

    private void handleFriendSelected(FriendResponseDTO newFriend){
        try {
            Long friendId = newFriend.getSenderId().equals(SessionManager.getInstance().getUserId()) ? newFriend.getReceiverId() : newFriend.getSenderId();

            ConversationResponseDTO conversation = conversationService.createConversation(friendId);

            if(onStartConversation != null){
                onStartConversation.accept(conversation);
            }
        } catch (Exception _) {
            AlertUtils.showError(errorLabel, "Couldn't start conversation");
        }
    }

    @FXML
    public void showGroupDetailsView(){
        if(selectedFriends.size() < 2){
            AlertUtils.showError(errorLabel,"Select at least two friends");

            return;
        }

        showView(groupDetailsView);
    }

    @FXML
    public void handleCreateGroup(){
        try {
            String groupName = groupNameField.getText().trim();

            if(groupName.isEmpty()){
                AlertUtils.showError(errorLabel,"Enter a group name");

                return;
            }

            List<Long> participantsId = selectedFriends
                    .stream()
                    .map(friend -> friend.getSenderId().equals(SessionManager.getInstance().getUserId()) ? friend.getReceiverId() : friend.getSenderId())
                    .toList();

            ConversationResponseDTO conversation = conversationService.createGroupConversation(participantsId, groupName, null);

            if(onStartConversation != null){
                onStartConversation.accept(conversation);
            }
        } catch (Exception _) {
            AlertUtils.showError(errorLabel,"Couldn't create group");
        }
    }

    private void loadFriends(){
        try {
            List<FriendResponseDTO> friends = friendService.getFriends();

            singleSelectFriendList.getItems().setAll(friends);
            multiSelectFriendList.getItems().setAll(friends);
        } catch (Exception _) {
            AlertUtils.showError(errorLabel, "Couldn't load friends");
        }
    }

    @FXML
    public void showSingleSelectView(){
        showView(singleSelectView);
    }

    @FXML
    public void showMultiSelectView(){
        showView(multiSelectView);
    }

    @FXML
    public void showChoiceView(){
        showView(choiceView);
    }

    @FXML
    public void handleUploadGroupPicture(){

    }

    private void showView(VBox viewToShow) {
        choiceView.setVisible(false);
        choiceView.setManaged(false);

        singleSelectView.setVisible(false);
        singleSelectView.setManaged(false);

        multiSelectView.setVisible(false);
        multiSelectView.setManaged(false);

        groupDetailsView.setVisible(false);
        groupDetailsView.setManaged(false);

        viewToShow.setVisible(true);
        viewToShow.setManaged(true);
    }
}