package com.chatappfrontend.frontend.manager;

import com.chatappfrontend.frontend.factory.MessageBubbleFactory;
import com.chatappfrontend.frontend.model.MessageEventDTO;
import com.chatappfrontend.frontend.model.MessageResponseDTO;
import com.chatappfrontend.frontend.model.UserStatusEventDTO;
import com.chatappfrontend.frontend.service.WebSocketService;
import com.chatappfrontend.frontend.util.SessionManager;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageEventManager {
    private final VBox messagesContainer;
    private final MessageBubbleFactory messageBubbleFactory;
    private final ConversationListManager conversationListManager;
    private final WebSocketService webSocketService;
    private final Map<Long, String> pendingMessageStatuses = new HashMap<>();

    @Setter
    private Long currentConversationId;
    @Setter
    private boolean currentConversationIsGroup;

    public MessageEventManager(VBox messageContainer, MessageBubbleFactory messageBubbleFactory, ConversationListManager conversationListManager, WebSocketService webSocketService) {
        this.messagesContainer = messageContainer;
        this.messageBubbleFactory = messageBubbleFactory;
        this.conversationListManager = conversationListManager;
        this.webSocketService = webSocketService;
    }

    public void handleUserQueueEvent(MessageEventDTO event){
        Platform.runLater(() -> {
            if(event.getType().equals("NEW")){
                webSocketService.sendDeliveredReceipt(event.getMessageId());

                if(!event.getConversationId().equals(currentConversationId)){
                    conversationListManager.updateConversationPreview(event.getConversationId(), event.getMessage().getMessage(), event.getMessage().getSentAt());
                }
            }else if(event.getType().equals("STATUS")){
                handleStatusUpdate(event.getMessageIds(), event.getStatus());
            }
        });
    }

    public void handleUserStatusEvent(UserStatusEventDTO event){
        Platform.runLater(() -> conversationListManager.updateFriendStatus(event.getUserId(), event.getStatus()));
    }

    public void handleConversationEvent(MessageEventDTO event){
        Platform.runLater(() -> {
            switch (event.getType()) {
                case "NEW" -> {
                    MessageResponseDTO message = event.getMessage();

                    if(pendingMessageStatuses.containsKey(message.getId())){
                        message.setStatus(pendingMessageStatuses.remove(message.getId()));
                    }

                    if(!message.getSenderId().equals(SessionManager.getInstance().getUserId())){
                        webSocketService.sendReadReceipt(currentConversationId);
                    }

                    boolean alreadyShown = messagesContainer.getChildren().stream().anyMatch(node -> message.getId().equals(node.getProperties().get("messageId")));

                    if(!alreadyShown){
                        HBox bubble = messageBubbleFactory.createMessageBubble(message, currentConversationIsGroup);

                        messagesContainer.getChildren().add(bubble);
                    }

                    conversationListManager.updateConversationPreview(currentConversationId, message.getMessage(), message.getSentAt());
                }
                case "EDIT" -> {
                    MessageResponseDTO message = event.getMessage();

                    refreshMessageBubble(message);

                    if(isLastMessageInContainer(message.getId())){
                        conversationListManager.updateConversationPreview(currentConversationId, message.getMessage(), message.getSentAt());
                    }
                }
                case "DELETE" -> {
                    boolean wasLast = isLastMessageInContainer(event.getMessageId());

                    messagesContainer.getChildren().removeIf(node -> event.getMessageId().equals(node.getProperties().get("messageId")));

                    if(wasLast){
                        syncPreviewToNewLastMessage();
                    }
                }
                case "STATUS" -> handleStatusUpdate(event.getMessageIds(), event.getStatus());
            }
        });
    }

    public boolean isLastMessageInContainer(Long messageId){
        if(messagesContainer.getChildren().isEmpty()){
            return false;
        }

        Node lastNode = messagesContainer.getChildren().getLast();

        return messageId.equals(lastNode.getProperties().get("messageId"));
    }

    public void syncPreviewToNewLastMessage(){
        if(messagesContainer.getChildren().isEmpty()){
            conversationListManager.updateConversationPreview(currentConversationId, "", null);

            return;
        }

        Node lastNode = messagesContainer.getChildren().getLast();

        MessageResponseDTO lastMessage = (MessageResponseDTO) lastNode.getProperties().get("messageObj");

        if(lastMessage != null){
            conversationListManager.updateConversationPreview(currentConversationId, lastMessage.getMessage(), lastMessage.getSentAt());
        }
    }

    public void refreshMessageBubble(MessageResponseDTO message){
        for(int i = 0; i < messagesContainer.getChildren().size(); i++){
            Node node = messagesContainer.getChildren().get(i);

            if(message.getId().equals(node.getProperties().get("messageId"))){
                HBox newBubble = messageBubbleFactory.createMessageBubble(message, currentConversationIsGroup);

                messagesContainer.getChildren().set(i, newBubble);

                return;
            }
        }
    }

    private void handleStatusUpdate(List<Long> messageIds, String status){
        for(Long messageId : messageIds){
            boolean found = false;

            for(Node node : messagesContainer.getChildren()){
                if(messageId.equals(node.getProperties().get("messageId"))){
                    found = true;

                    MessageResponseDTO messageObj = (MessageResponseDTO) node.getProperties().get("messageObj");
                    String currentStatus = messageObj != null ? messageObj.getStatus() : null;

                    if(currentStatus != null && statusRank(status) < statusRank(currentStatus)){
                        break;
                    }

                    Label statusLabel = (Label) node.getProperties().get("statusLabel");

                    if(messageObj != null){
                        messageObj.setStatus(status);
                    }

                    if(statusLabel != null){
                        statusLabel.setText(messageBubbleFactory.formatStatus(status));
                        statusLabel.setStyle("-fx-text-fill: " + messageBubbleFactory.getStatusColor(status) + "; -fx-font-size: 10px;");
                    }

                    break;
                }
            }

            if(!found){
                pendingMessageStatuses.put(messageId, status);
            }
        }
    }

    public void addMessageIfAbsent(MessageResponseDTO message){
        boolean alreadyShown = messagesContainer.getChildren().stream()
                .anyMatch(node -> message.getId().equals(node.getProperties().get("messageId")));

        if(!alreadyShown){
            messagesContainer.getChildren().add(messageBubbleFactory.createMessageBubble(message, currentConversationIsGroup));
        }
    }

    private int statusRank(String status){
        return switch (status) {
            case "sent" -> 1;
            case "delivered" -> 2;
            case "read" -> 3;
            default -> 0;
        };
    }
}