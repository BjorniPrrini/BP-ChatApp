package com.chatappfrontend.frontend.service;

import com.chatappfrontend.frontend.model.ConversationResponseDTO;
import com.chatappfrontend.frontend.model.GroupConversationRequestDTO;
import com.chatappfrontend.frontend.model.ParticipantDTO;
import com.chatappfrontend.frontend.util.ApiExceptionHandler;
import com.chatappfrontend.frontend.util.AppConfig;
import com.chatappfrontend.frontend.util.JsonMapper;
import com.chatappfrontend.frontend.util.SessionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.*;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;

public class ConversationService {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = JsonMapper.get();
    private static final String BASE_URL = AppConfig.get("api.base.url") + "/api/conversation";

    public List<ConversationResponseDTO> getConversations() throws Exception{
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/getUserConversations"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, ConversationResponseDTO.class));
        }

        ApiExceptionHandler.handle(response);

        return null;
    }

    public ConversationResponseDTO createConversation(Long receiverId) throws Exception{
        String body = String.format("{\"receiverId\":%d}", receiverId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/createConversation"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return objectMapper.readValue(response.body(), ConversationResponseDTO.class);
        }

        ApiExceptionHandler.handle(response);

        return null;
    }

    public void deleteConversation(Long conversationId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + conversationId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return;
        }

        ApiExceptionHandler.handle(response);
    }

    public ConversationResponseDTO createGroupConversation(List<Long> participantIds, String groupName, String groupPicture) throws Exception {
        GroupConversationRequestDTO requestDTO = new GroupConversationRequestDTO();

        requestDTO.setParticipants(participantIds);
        requestDTO.setGroupName(groupName);
        requestDTO.setGroupPicture(groupPicture);

        String body = objectMapper.writeValueAsString(requestDTO);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/createGroupConversation"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return objectMapper.readValue(response.body(), ConversationResponseDTO.class);
        }

        ApiExceptionHandler.handle(response);

        return null;
    }

    public void kickParticipant(Long conversationId, Long participantToKickId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/conversation/" + conversationId + "/participant/" + participantToKickId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return;
        }

        ApiExceptionHandler.handle(response);
    }

    public void leaveGroup(Long conversationId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/leaveGroup/" + conversationId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return;
        }

        ApiExceptionHandler.handle(response);
    }

    public ParticipantDTO addParticipant(Long conversationId, Long participantToAddId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/conversation/" + conversationId + "/addParticipant/" + participantToAddId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return objectMapper.readValue(response.body(), ParticipantDTO.class);
        }

        ApiExceptionHandler.handle(response);

        return null;
    }

    public void demoteAdmin(Long conversationId, Long demoteUserId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/conversation/" + conversationId + "/demoteUser/" + demoteUserId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return;
        }

        ApiExceptionHandler.handle(response);
    }

    public void promoteToAdmin(Long conversationId, Long promoteUserId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/conversation/" + conversationId + "/promoteUser/" + promoteUserId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return;
        }

        ApiExceptionHandler.handle(response);
    }

    public ConversationResponseDTO updateGroupInfo(Long conversationId, String groupName, File groupPicture) throws Exception {
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        if(groupName != null){
            bodyBuilder.addFormDataPart("groupName", groupName);
        }

        if(groupPicture != null){
            String mimeType = Files.probeContentType(groupPicture.toPath());

            bodyBuilder.addFormDataPart("groupPicture", groupPicture.getName(), RequestBody.create(groupPicture, MediaType.parse(mimeType)));
        }

        RequestBody body = bodyBuilder.build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/conversation/" + conversationId)
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .patch(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if(response.isSuccessful()){
                return objectMapper.readValue(response.body().string(), ConversationResponseDTO.class);
            }

            ApiExceptionHandler.handle(response);
        }

        return null;
    }

    public Boolean isAdmin(Long conversationId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/conversation/" + conversationId + "/participant/self"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return objectMapper.readValue(response.body(), Boolean.class);
        }

        ApiExceptionHandler.handle(response);

        return false;
    }

    public List<ParticipantDTO> getFriendsNotInConversation(Long conversationId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/notInConversation/" + conversationId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, ParticipantDTO.class));
        }

        ApiExceptionHandler.handle(response);

        return null;
    }

    public ConversationResponseDTO getConversationById(Long conversationId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + conversationId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return objectMapper.readValue(response.body(), ConversationResponseDTO.class);
        }

        ApiExceptionHandler.handle(response);

        return null;
    }

    public void allowParticipantsInvite(Long conversationId, boolean allow) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/conversation/" + conversationId + "/allowInvite/" + allow))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() >= 200 && response.statusCode() < 300){
            return;
        }

        ApiExceptionHandler.handle(response);
    }
}