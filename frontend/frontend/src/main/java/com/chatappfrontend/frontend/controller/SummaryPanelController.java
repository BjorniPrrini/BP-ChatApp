package com.chatappfrontend.frontend.controller;

import com.chatappfrontend.frontend.model.SummaryResponseDTO;
import com.chatappfrontend.frontend.service.SummaryService;
import com.chatappfrontend.frontend.util.AppExecutor;
import com.chatappfrontend.frontend.util.SummaryInProgressException;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class SummaryPanelController {
    @FXML
    private ProgressIndicator summaryLoadingIndicator;
    @FXML
    private ScrollPane summaryScrollPane;
    @FXML
    private Label summaryLabel;
    @FXML
    private Label quotaLabel;
    @FXML
    private Button resummarizeButton;
    @FXML
    private BorderPane summaryPanel;
    @FXML
    private Label errorLabel;
    @FXML
    private Label refreshedLabel;

    private static final DateTimeFormatter REFRESH_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final long POLL_INTERVAL_MILLIS = 5_000;
    private static final long POLL_TIMEOUT_MILLIS = 120_000;

    private Long currentConversationId;
    private final Map<Long, SummaryEntry> summaryCache = new HashMap<>();
    private final SummaryService summaryService = new SummaryService();

    public void startSummarizing(Long conversationId){
        currentConversationId = conversationId;

        SummaryEntry entry = summaryCache.get(conversationId);

        if(entry == null){
            entry = new SummaryEntry();

            summaryCache.put(conversationId, entry);

            requestSummary(conversationId, entry);

            return;
        }

        if(entry.getStatus() == SummaryStatus.FAILED && entry.getText() == null){
            requestSummary(conversationId, entry);

            return;
        }

        render(entry);
    }

    private void requestSummary(Long conversationId, SummaryEntry entry){
        entry.setStatus(SummaryStatus.IN_PROGRESS);

        renderIfShown(conversationId, entry);

        Task<SummaryResponseDTO> task = new Task<>() {
            @Override
            protected SummaryResponseDTO call() throws Exception {
                return fetchWithPolling(conversationId);
            }
        };

        task.setOnSucceeded(_ -> {
            SummaryResponseDTO result = task.getValue();

            entry.setStatus(SummaryStatus.ARRIVED);
            entry.setText(result.getSummary());
            entry.setRefreshedAt(LocalDateTime.now());

            quotaLabel.setText("Tries left today: " + result.getRemainingQuota());

            renderIfShown(conversationId, entry);
        });

        task.setOnFailed(_ -> {
            entry.setStatus(SummaryStatus.FAILED);

            renderIfShown(conversationId, entry);
        });

        AppExecutor.run(task);
    }

    private SummaryResponseDTO fetchWithPolling(Long conversationId) throws Exception {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;

        while(true){
            try {
                return summaryService.getSummary(conversationId);
            } catch (SummaryInProgressException e) {
                if(System.currentTimeMillis() + POLL_INTERVAL_MILLIS >= deadline){
                    throw e;
                }

                Thread.sleep(POLL_INTERVAL_MILLIS);
            }
        }
    }

    private void renderIfShown(Long conversationId, SummaryEntry entry){
        if(summaryPanel.isVisible() && conversationId.equals(currentConversationId)){
            render(entry);
        }
    }

    private void render(SummaryEntry entry){
        setShown(summaryLoadingIndicator, false);
        setShown(summaryScrollPane, false);
        setShown(errorLabel, false);
        setShown(refreshedLabel, false);

        resummarizeButton.setDisable(false);

        switch (entry.getStatus()) {
            case IN_PROGRESS -> {
                setShown(summaryLoadingIndicator, true);

                resummarizeButton.setDisable(true);
            }
            case ARRIVED -> showSummary(entry);
            case FAILED -> {
                errorLabel.setText("Failed to generate summary");

                setShown(errorLabel, true);

                if(entry.getText() != null){
                    showSummary(entry);
                }
            }
        }
    }

    private void showSummary(SummaryEntry entry){
        summaryLabel.setText(entry.getText());

        setShown(summaryScrollPane, true);

        if(entry.getRefreshedAt() != null){
            refreshedLabel.setText("Last refreshed: " + entry.getRefreshedAt().format(REFRESH_FORMAT));

            setShown(refreshedLabel, true);
        }
    }

    private void setShown(Node node, boolean shown){
        node.setVisible(shown);
        node.setManaged(shown);
    }

    @FXML
    private void handleResummarize() {
        SummaryEntry entry = summaryCache.get(currentConversationId);

        if(entry == null || entry.getStatus() == SummaryStatus.IN_PROGRESS){
            return;
        }

        requestSummary(currentConversationId, entry);
    }

    @FXML
    private void handleClose() {
        summaryPanel.setVisible(false);
        summaryPanel.setManaged(false);
    }

    @Data
    private static class SummaryEntry {
        private SummaryStatus status;
        private String text;
        private LocalDateTime refreshedAt;
    }

    private enum SummaryStatus {
        IN_PROGRESS,
        ARRIVED,
        FAILED
    }
}