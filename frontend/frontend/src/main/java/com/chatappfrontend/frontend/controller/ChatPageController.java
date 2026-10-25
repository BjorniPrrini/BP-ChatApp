package com.chatappfrontend.frontend.controller;

import com.chatappfrontend.frontend.cell.*;
import com.chatappfrontend.frontend.factory.MessageBubbleFactory;
import com.chatappfrontend.frontend.manager.*;
import com.chatappfrontend.frontend.model.*;
import com.chatappfrontend.frontend.model.ui.SearchResultItem;
import com.chatappfrontend.frontend.service.*;
import com.chatappfrontend.frontend.util.*;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ChatPageController {
    @FXML
    private Button friendsIconButton;
    @FXML
    private VBox friendsPanel;
    @FXML
    private Label notificationLabel;
    @FXML
    private Pane backgroundPane;
    @FXML
    private Button chatsIconButton;
    @FXML
    private Button settingsIconButton;
    @FXML
    private VBox fixedPanel;
    @FXML
    private VBox conversationsPanel;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<SearchResultItem> conversationList;
    @FXML
    private VBox settingsPanel;
    @FXML
    private HBox chatHeader;
    @FXML
    private Label chatNameLabel;
    @FXML
    private ScrollPane messagesScrollPane;
    @FXML
    private VBox messagesContainer;
    @FXML
    private HBox messageInputArea;
    @FXML
    private TextField messageInput;
    @FXML
    private VBox replyPreviewBox;
    @FXML
    private StackPane contentPane;
    @FXML
    private VBox chatArea;
    @FXML
    private Button createConversationButton;
    @FXML
    private Button editGroupButton;
    @FXML
    private Button summarizeButton;
    @FXML
    private HBox conversationContent;
    @FXML
    private BorderPane summaryPanel;
    @FXML
    private SummaryPanelController summaryPanelController;

    private Long currentConversationId;
    private final WebSocketService webSocketService = new WebSocketService();
    private MessageResponseDTO replyingTo;
    private MessageBubbleFactory messageBubbleFactory;
    private ConversationListManager conversationListManager;
    private PanelManager panelManager;
    private WebSocketConnectionManager webSocketConnectionManager;
    private MessageEventManager messageEventManager;
    private MessageActionManager messageActionManager;
    private MessagePaginationManager messagePaginationManager;
    private MessageSearchManager messageSearchManager;
    private boolean currentConversationIsGroup;
    private boolean sending = false;
    private final MessageService messageService = new MessageService();

    @FXML
    public void initialize(){
        summarizeButton.setVisible(false);
        summarizeButton.setManaged(false);

        conversationList.setCellFactory(_ -> new ConversationCell(conversationId -> {
            conversationListManager.removeConversation(conversationId);

            if(conversationId.equals(currentConversationId)){
                clearChatPane();
            }
        }));

        messageBubbleFactory = new MessageBubbleFactory(SessionManager.getInstance().getUserId(), this::handleReply, message -> messageActionManager.handleEdit(message), (message, bubble) -> messageActionManager.handleDeleteForMe(message, bubble), (message, bubble) -> messageActionManager.handleDeleteForEveryone(message, bubble));

        conversationListManager = new ConversationListManager(conversationList, message -> AlertUtils.showError(notificationLabel, message));

        messageEventManager = new MessageEventManager(messagesContainer,messageBubbleFactory, conversationListManager, webSocketService);

        messageActionManager = new MessageActionManager(messageEventManager, conversationListManager, message -> AlertUtils.showError(notificationLabel, message), messagesContainer);

        messagePaginationManager = new MessagePaginationManager(messagesContainer, messageBubbleFactory, messagesScrollPane, message -> AlertUtils.showError(notificationLabel, message));

        messageSearchManager = new MessageSearchManager(conversationList, message -> AlertUtils.showError(notificationLabel, message));

        panelManager = new PanelManager(List.of(conversationsPanel, settingsPanel, friendsPanel));

        webSocketConnectionManager = new WebSocketConnectionManager(webSocketService);

        conversationListManager.loadConversations();

        conversationList.setOnMouseClicked(_ -> {
            SearchResultItem selected = conversationList.getSelectionModel().getSelectedItem();

            if(selected == null){
                return;
            }

            switch (selected) {
                case SearchResultItem.ConversationResult(ConversationResponseDTO conversation) -> openConversation(conversation);
                case SearchResultItem.MessageResult(MessageSearchResultResponseDTO message) -> conversationListManager.openOrFetchConversation(message.getConversationId(), this::openConversation);
                case SearchResultItem.LoadingResult _ -> {}
            }
        });

        messageInput.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                handleSendMessage();
            }
        });

        searchField.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                String query = searchField.getText().trim();

                if(query.isEmpty()){
                    return;
                }

                messageSearchManager.clearSearchResults();
                conversationListManager.filter(query);
                messageSearchManager.search(query);
            }
        });

        searchField.textProperty().addListener((_, _, newValue) -> {
            if(newValue.isEmpty()){
                messageSearchManager.clearSearchResults();
                conversationListManager.loadConversations();
            }
        });

        messagesContainer.heightProperty().addListener((_, _, _) -> messagesScrollPane.setVvalue(1.0));

        try {
            webSocketConnectionManager.connect(SessionManager.getInstance().getUserId(), messageEventManager::handleUserQueueEvent, messageEventManager::handleUserStatusEvent);

            webSocketConnectionManager.setMembershipHandler(this::handleMembershipEvent);
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Could not connect to real time service");
        }
    }

    private void setActiveConversationId(Long conversationId){
        messageEventManager.setCurrentConversationId(conversationId);
        messageEventManager.setCurrentConversationIsGroup(currentConversationIsGroup);
        messageActionManager.setCurrentConversationId(conversationId);
        messagePaginationManager.setCurrentConversationId(conversationId);
        messagePaginationManager.setCurrentConversationIsGroup(currentConversationIsGroup);
    }

    private void openConversation(ConversationResponseDTO selected){
        summaryPanel.setVisible(false);
        summaryPanel.setManaged(false);

        if(selected.isGroup()){
            editGroupButton.setVisible(true);
            editGroupButton.setManaged(true);
        }else{
            editGroupButton.setVisible(false);
            editGroupButton.setManaged(false);
        }

        showChatContent();

        currentConversationId = selected.getConversationId();
        currentConversationIsGroup = selected.isGroup();

        summarizeButton.setVisible(true);
        summarizeButton.setManaged(true);

        setActiveConversationId(currentConversationId);

        messagePaginationManager.resetPagination();

        webSocketConnectionManager.subscribeToConversation(currentConversationId, messageEventManager::handleConversationEvent);

        if(selected.isGroup()){
            chatNameLabel.setText(selected.getGroupName());
        }else{
            chatNameLabel.setText(selected.getParticipants().getFirst().getNickname() != null ? selected.getParticipants().getFirst().getNickname() : selected.getParticipants().getFirst().getName() + " " + selected.getParticipants().getFirst().getSurname());
        }

        messagesContainer.getChildren().clear();

        try {
            MessagePageDTO messagePage = messageService.getMessages(currentConversationId, null);

            List<MessageResponseDTO> messages = messagePage.getMessages();

            for(MessageResponseDTO message : messages){
                HBox bubble = messageBubbleFactory.createMessageBubble(message, selected.isGroup());

                messagesContainer.getChildren().add(bubble);
            }

            messagePaginationManager.setInitialMessages(messagePage);

            messageInput.requestFocus();
        } catch (Exception _){
            AlertUtils.showError(notificationLabel, "Couldn't get the messages");
        }
    }

    private void handleReply(MessageResponseDTO message){
        replyingTo = message;

        replyPreviewBox.getChildren().clear();

        Label replyLabel = new Label("Replying to: " + message.getMessage());

        replyLabel.setWrapText(true);
        replyLabel.setMaxWidth(350);
        replyLabel.setStyle("-fx-background-color: #333333;" + "-fx-text-fill: white;" + "-fx-padding: 8;" + "-fx-background-radius: 8;");

        Button cancelButton = new Button("X");

        cancelButton.setOnAction(_ -> cancelReply());

        HBox preview = new HBox(10);

        preview.setAlignment(Pos.CENTER_LEFT);
        preview.getChildren().addAll(replyLabel, cancelButton);

        replyPreviewBox.getChildren().add(preview);
        replyPreviewBox.setVisible(true);
        replyPreviewBox.setManaged(true);

        messageInput.requestFocus();
    }

    private void cancelReply(){
        replyingTo = null;

        replyPreviewBox.getChildren().clear();
        replyPreviewBox.setVisible(false);
        replyPreviewBox.setManaged(false);
    }

    private void showChatContent(){
        contentPane.getChildren().setAll(chatArea);
    }

    @FXML
    public void showConversations(){
        showChatContent();

        panelManager.showPanel(conversationsPanel);
    }

    @FXML
    public void showSettings(){
        panelManager.showPanel(settingsPanel);
    }

    @FXML
    public void showFriends(){
        panelManager.showPanel(friendsPanel);
    }

    @FXML
    public void handleLogout(){
        webSocketService.disconnect();

        SessionManager.getInstance().clear();

        try {
            SceneManager.switchTo("login-page.fxml");
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load loading page");
        }
    }

    @FXML
    public void handleChangePassword(){
        try {
            SceneManager.switchContent(contentPane, "change-password.fxml");
        } catch (IOException _) {
            AlertUtils.showError(notificationLabel, "Couldn't load change password page");
        }
    }

    @FXML
    public void handleEditProfile(){
        try {
            SceneManager.switchContent(contentPane, "edit-profile.fxml");
        } catch (IOException _) {
            AlertUtils.showError(notificationLabel, "Couldn't load edit profile page");
        }
    }

    @FXML
    public void handleProfilePicture(){
        try {
            UploadProfilePictureController controller = SceneManager.switchContent(contentPane, "upload-profile-picture.fxml");

            controller.loadProfilePicture();
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load upload profile picture page");
        }
    }

    @FXML
    public void handleThemeManager(){
        try {
            SceneManager.switchContent(contentPane, "theme-manager-page.fxml");
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load theme manager page");
        }
    }

    @FXML
    public void handleOpenStartChatPopup(){
        try {
            PopupManager.openPopup("Create Conversation", "start-chat-popup.fxml", (StartChatPopupController controller, Stage stage) -> controller.setOnStartConversation(conversation -> {
                    stage.close();

                    panelManager.showPanel(conversationsPanel);

                    openConversation(conversation);
                })
            );
        } catch (IOException _) {
            AlertUtils.showError(notificationLabel, "Couldn't open create conversation popup");
        }
    }

    @FXML
    public void handleSendMessage(){
        if(sending){
            return;
        }

        String message = messageInput.getText().trim();

        if(message.isEmpty()){
            AlertUtils.showError(notificationLabel, "Can't send empty messages");

            return;
        }

        if(currentConversationId == null){
            AlertUtils.showError(notificationLabel, "Not a valid conversation");

            return;
        }

        final Long conversationId = currentConversationId;
        final Long replyToId = replyingTo != null ? replyingTo.getId() : null;

        sending = true;
        messageInput.clear();
        cancelReply();

        Task<MessageResponseDTO> task = new Task<>() {
            @Override
            protected MessageResponseDTO call() throws Exception {
                if(replyToId != null){
                    return messageService.replyMessage(conversationId, replyToId, message);
                }

                return messageService.sendMessage(conversationId, message);
            }
        };

        task.setOnSucceeded(_ -> {
            sending = false;
            MessageResponseDTO sent = task.getValue();

            conversationListManager.updateConversationPreview(conversationId, sent.getMessage(), sent.getSentAt());

            if(conversationId.equals(currentConversationId)){
                messageEventManager.addMessageIfAbsent(sent);
            }
        });

        task.setOnFailed(_ -> {
            sending = false;

            if(messageInput.getText().isEmpty()){
                messageInput.setText(message);
            }

            AlertUtils.showError(notificationLabel, "Couldn't send message");
        });

        AppExecutor.run(task);
    }

    private void clearChatPane(){
        summaryPanel.setVisible(false);
        summaryPanel.setManaged(false);
        currentConversationId = null;
        setActiveConversationId(null);
        messagesContainer.getChildren().clear();
        chatNameLabel.setText("");
        summarizeButton.setVisible(false);
        summarizeButton.setManaged(false);
        webSocketService.unsubscribe();
    }

    @FXML
    public void handleAddFriends(){
        try {
            SceneManager.switchContent(contentPane, "add-friend-page.fxml");
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load add friend page");
        }
    }

    @FXML
    public void handleFriendRequests(){
        try {
            SceneManager.switchContent(contentPane, "friend-requests-page.fxml");
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load friend requests page");
        }
    }

    @FXML
    public void handleBlockedUsers(){
        try {
            SceneManager.switchContent(contentPane, "blocked-users-page.fxml");
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load blocked users page");
        }
    }

    @FXML
    public void handleSuggestedFriends(){
        try {
            SceneManager.switchContent(contentPane, "suggested-users-page.fxml");
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load suggested friends page");
        }
    }

    @FXML
    public void handleMyFriends(){
        try {
            MyFriendsController controller = SceneManager.switchContent(contentPane, "my-friends-page.fxml");

            controller.setOnStartConversation(conversation -> {
                panelManager.showPanel(conversationsPanel);

                openConversation(conversation);
            });
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load friends page");
        }
    }

    @FXML
    public void handleEditGroup(){
        try {
            GroupInfoController controller = SceneManager.switchContent(contentPane, "group-info-page.fxml");

            controller.setCurrentConversationId(currentConversationId);

            controller.setContentPane(contentPane);

            controller.setOnBack(() -> {
                webSocketConnectionManager.unsubscribe();

                showChatContent();
            });

            controller.loadGroupInformation();

            controller.setOnGroupUpdated(this::refreshGroupHeader);
        } catch (Exception _) {
            AlertUtils.showError(notificationLabel, "Couldn't load group information page");
        }
    }

    private void refreshGroupHeader(Long conversationId, String newName, String profilePicture){
        if(currentConversationId.equals(conversationId)){
            chatNameLabel.setText(newName);
        }

        conversationListManager.updateConversationGroupInfo(conversationId, newName, profilePicture);
    }

    private void handleMembershipEvent(ConversationMembershipEventDTO event){
        if(event.getType().equals("KICKED") && event.getConversationId().equals(currentConversationId)){
            webSocketConnectionManager.unsubscribe();

            AlertUtils.showError(notificationLabel, "You were removed from this group");
        }
    }

    @FXML
    public void handleOpenSummary(){
        summaryPanel.setVisible(true);
        summaryPanel.setManaged(true);

        summaryPanelController.startSummarizing(currentConversationId);
    }
}