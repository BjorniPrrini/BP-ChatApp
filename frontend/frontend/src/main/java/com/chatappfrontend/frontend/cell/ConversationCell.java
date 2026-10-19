package com.chatappfrontend.frontend.cell;

import com.chatappfrontend.frontend.model.ConversationResponseDTO;
import com.chatappfrontend.frontend.model.MessageSearchResultResponseDTO;
import com.chatappfrontend.frontend.model.ParticipantDTO;
import com.chatappfrontend.frontend.model.ui.SearchResultItem;
import com.chatappfrontend.frontend.util.AvatarUtils;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class ConversationCell extends ListCell<SearchResultItem> {
    private final Consumer<Long> onDelete;

    public ConversationCell(Consumer<Long> onDelete) {
        this.onDelete = onDelete;
    }

    @Override
    protected void updateItem(SearchResultItem item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null){
            setGraphic(null);

            setStyle("-fx-background-color: transparent;");

            return;
        }

        HBox cell = switch (item) {
            case SearchResultItem.ConversationResult(ConversationResponseDTO conversation) -> buildConversationRow(conversation);
            case SearchResultItem.MessageResult(MessageSearchResultResponseDTO message) -> buildMessageRow(message);
            case SearchResultItem.LoadingResult _ -> buildLoadingRow();
        };

        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setStyle("-fx-padding: 8 5;");

        setGraphic(cell);
        setStyle("-fx-background-color: transparent;");
    }

    private HBox buildConversationRow(ConversationResponseDTO conversation){
        Label avatar = new Label();

        String displayName;
        String borderColor;
        String profilePicturePath;

        if(conversation.isGroup()){
            displayName = conversation.getGroupName();

            profilePicturePath = conversation.getGroupPicture();

            borderColor = "-app-border-strong";
        }else{
            ParticipantDTO otherUser = conversation.getParticipants().getFirst();

            displayName = otherUser.getName().substring(0, 1).toUpperCase(Locale.ROOT) + otherUser.getName().substring(1).toLowerCase(Locale.ROOT) + " " + otherUser.getSurname().substring(0, 1).toUpperCase(Locale.ROOT) + otherUser.getSurname().substring(1).toLowerCase(Locale.ROOT);

            profilePicturePath = otherUser.getProfilePicture();

            borderColor = otherUser.isOnline() ? "-app-accent" : "-app-border-strong";
        }

        String initial = displayName.substring(0, 1);

        avatar.setStyle("-fx-background-color: -app-bg; -fx-text-fill: -app-accent; -fx-font-weight: bold; -fx-min-width: 40; -fx-min-height: 40; -fx-background-radius: 50; -fx-alignment: center; -fx-border-color: " + borderColor + "; -fx-border-radius: 20; -fx-border-width: 2;");

        AvatarUtils.applyAvatar(avatar, profilePicturePath, initial, true, 40);

        Label nameLabel = new Label(displayName);

        nameLabel.setStyle("-fx-text-fill: -app-text; -fx-font-weight: bold; -fx-font-size: 13px;");

        String lastMsg = conversation.getLastMessage() != null ? conversation.getLastMessage() : "No messages yet";

        Label lastMessageLabel = new Label(lastMsg);

        lastMessageLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        VBox textBox = new VBox(3, nameLabel, lastMessageLabel);

        HBox cell = new HBox(10, avatar, textBox);

        MenuItem deleteItem = new MenuItem("Delete conversation");

        deleteItem.setOnAction(_ -> onDelete.accept(conversation.getConversationId()));

        ContextMenu menu = new ContextMenu();

        menu.getItems().add(deleteItem);

        cell.setOnContextMenuRequested(event -> menu.show(cell, event.getScreenX(), event.getScreenY()));

        return cell;
    }

    private HBox buildMessageRow(MessageSearchResultResponseDTO message){
        String fullName = message.getSenderName().substring(0, 1).toUpperCase(Locale.ROOT) + message.getSenderName().substring(1).toLowerCase(Locale.ROOT) + " " + message.getSenderSurname().substring(0, 1).toUpperCase(Locale.ROOT) + message.getSenderSurname().substring(1).toLowerCase(Locale.ROOT);
        String timeLabelText = formatRelativeTime(message.getSentAt());

        Label nameLabel = new Label(fullName);
        nameLabel.setStyle("-fx-text-fill: -app-accent; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label messageLabel = new Label(message.getMessage());
        messageLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12px;");

        Label timeLabel = new Label(timeLabelText);
        timeLabel.setStyle("-fx-text-fill: -app-text-faint; -fx-font-size: 10px;");

        VBox textBox = new VBox(3, nameLabel, messageLabel, timeLabel);

        return new HBox(10, textBox);
    }

    private HBox buildLoadingRow(){
        ProgressIndicator spinner = new ProgressIndicator();

        spinner.setMaxWidth(30);

        return new HBox(10, new VBox(3, spinner, new Label("Searching")));
    }

    private String formatRelativeTime(LocalDateTime sentAt){
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(sentAt, now);

        LocalDate sentDate = sentAt.toLocalDate();
        LocalDate today = now.toLocalDate();

        if(duration.toSeconds() < 60){
            return "now";
        }else if(duration.toMinutes() < 60){
            return duration.toMinutes() + (duration.toMinutes() == 1 ? " minute ago" : " minutes ago");
        }else if(sentDate.equals(today)){
            return duration.toHours() + (duration.toHours() == 1 ? " hour ago" : " hours ago");
        }else if(sentDate.equals(today.minusDays(1))){
            return "Yesterday";
        }else{
            return sentAt.format(DateTimeFormatter.ofPattern("d MM, yyyy"));
        }
    }
}