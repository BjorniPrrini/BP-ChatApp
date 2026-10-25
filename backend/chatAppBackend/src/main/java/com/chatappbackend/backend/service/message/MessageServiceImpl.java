package com.chatappbackend.backend.service.message;

import com.chatappbackend.backend.dto.message.MessageEventDTO;
import com.chatappbackend.backend.dto.message.MessagePageDTO;
import com.chatappbackend.backend.dto.message.MessageRequestDTO;
import com.chatappbackend.backend.dto.message.MessageResponseDTO;
import com.chatappbackend.backend.entity.*;
import com.chatappbackend.backend.exception.BadRequestException;
import com.chatappbackend.backend.exception.ForbiddenException;
import com.chatappbackend.backend.exception.ResourceNotFoundException;
import com.chatappbackend.backend.mapper.MessageMapper;
import com.chatappbackend.backend.repository.*;
import com.chatappbackend.backend.service.blocked.BlockedUserService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService{
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageDeleteRepository messageDeleteRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final BlockedUserService blockedUserService;
    private final FriendRequestRepository friendRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageMapper messageMapper;
    private final MessageDeliveryRepository messageDeliveryRepository;
    private final MessageReadRepository messageReadRepository;
    private final MessageEmbeddingWriter messageEmbeddingWriter;

    public MessageServiceImpl(UserRepository userRepository, ConversationRepository conversationRepository, MessageRepository messageRepository, MessageDeleteRepository messageDeleteRepository, ConversationParticipantRepository conversationParticipantRepository, BlockedUserService blockedUserService, FriendRequestRepository friendRequestRepository, SimpMessagingTemplate messagingTemplate, MessageMapper messageMapper, MessageDeliveryRepository messageDeliveryRepository, MessageReadRepository messageReadRepository, MessageEmbeddingWriter messageEmbeddingWriter){
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageDeleteRepository = messageDeleteRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.blockedUserService = blockedUserService;
        this.friendRequestRepository = friendRequestRepository;
        this.messagingTemplate = messagingTemplate;
        this.messageMapper = messageMapper;
        this.messageDeliveryRepository = messageDeliveryRepository;
        this.messageReadRepository = messageReadRepository;
        this.messageEmbeddingWriter = messageEmbeddingWriter;
    }

    @Override
    public MessageResponseDTO sendMessage(Long userId, MessageRequestDTO request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Conversation conversation = conversationRepository.findById(request.getConversationId()).orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        List<User> otherUserList = conversationParticipantRepository.findOtherParticipants(request.getConversationId(), userId);
        ConversationParticipant senderParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversation.getId(), userId).orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        if(senderParticipant.getLeftAt() != null){
            throw new ForbiddenException("You are not part of this conversation anymore");
        }

        if(otherUserList.isEmpty()){
            throw new ResourceNotFoundException("Participant not found");
        }

        if(conversation.getIsGroup().equals(false)){
            User otherUser = otherUserList.getFirst();

            if(blockedUserService.isBlocked(userId, otherUser.getId())){
                throw new BadRequestException("Cannot send message to this user");
            }

            if(!friendRequestRepository.areFriends(userId, otherUser.getId())){
                throw new BadRequestException("You must be friends to message this user");
            }
        }

        for(User otherUser : otherUserList){
            conversationParticipantRepository.restoreForUser(conversation.getId(), otherUser.getId());
        }

        Message message = new Message();

        message.setMessage(request.getMessage());
        message.setSender(user);
        message.setConversation(conversation);
        message.setSentAt(LocalDateTime.now());
        message.setStatus("sent");

        if(request.getReplyToId() != null){
            Message replyTo = messageRepository.findById(request.getReplyToId()).orElseThrow(() -> new ResourceNotFoundException("Message not found"));

            if(!replyTo.getConversation().getId().equals(conversation.getId())){
                throw new BadRequestException("Reply message does not belong to this conversation");
            }

            message.setReplyTo(replyTo);
        }

        Message savedMessage = messageRepository.save(message);

        MessageResponseDTO dto = messageMapper.toMessageResponseDTO(savedMessage);

        messagingTemplate.convertAndSend("/topic/conversation." + conversation.getId(), new MessageEventDTO("NEW", conversation.getId(), dto.getId(), dto, null, null));

        otherUserList.forEach(otherUser -> messagingTemplate.convertAndSend("/queue/user." + otherUser.getId(), new MessageEventDTO("NEW", conversation.getId(), dto.getId(), dto, null, null)));

        messageEmbeddingWriter.embedMessage(savedMessage.getId());

        return dto;
    }

    @Override
    @Transactional
    public MessagePageDTO getMessages(Long userId, Long conversationId, LocalDateTime before) {
        markConversationAsRead(userId, conversationId);

        ConversationParticipant conversationParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId).orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        LocalDateTime clearedAt = conversationParticipant.getClearedAt();
        LocalDateTime joinedAt = conversationParticipant.getJoinedAt();

        if(clearedAt == null){
            clearedAt = LocalDateTime.of(1970,1,1,0,0);
        }

        LocalDateTime latestEvent;

        if(clearedAt.isAfter(joinedAt)){
            latestEvent = clearedAt;
        }else{
            latestEvent = joinedAt;
        }

        List<Message> messages = messageRepository.findMessages(userId, conversationId, before, PageRequest.of(0, 50), latestEvent);

        List<MessageResponseDTO> messagesResponse = messages.stream()
                .map(messageMapper::toMessageResponseDTO)
                .collect(Collectors.toList());

        Collections.reverse(messagesResponse);

        MessagePageDTO messagesDto = new MessagePageDTO();

        messagesDto.setMessages(messagesResponse);
        messagesDto.setHasMore(messages.size() == 50);

        return messagesDto;
    }

    @Override
    public void deleteMessageForMe(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MessageDelete messageDelete = new MessageDelete();

        messageDelete.setMessage(message);
        messageDelete.setUser(user);
        messageDelete.setDeletedAt(LocalDateTime.now());

        messageDeleteRepository.save(messageDelete);

        long deleteCount = messageDeleteRepository.countByMessage(message);
        long participantCount = conversationParticipantRepository.countByConversationId(message.getConversation().getId());

        if(deleteCount >= participantCount){
            messageRepository.delete(message);
        }
    }

    @Override
    public void deleteMessageForEveryone(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if(!message.getSender().getId().equals(userId)){
            throw new ForbiddenException("You can only delete your own message");
        }

        if(message.getSentAt().isBefore(LocalDateTime.now().minusMinutes(1))){
            throw new BadRequestException("You can't delete a message after 1 minutes");
        }

        Long conversationId = message.getConversation().getId();

        messageRepository.delete(message);

        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, new MessageEventDTO("DELETE", conversationId, messageId, null, null, null));
    }

    @Override
    public MessageResponseDTO editMessage(Long userId, Long messageId, String newMessage) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if(!Objects.equals(message.getSender().getId(), userId)){
            throw new ForbiddenException("You are not this message sender");
        }

        if(message.getSentAt().isBefore(LocalDateTime.now().minusMinutes(15))){
            throw new BadRequestException("You can't edit a message after 15 minutes");
        }

        message.setMessage(newMessage);
        message.setEditedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        MessageResponseDTO dto = messageMapper.toMessageResponseDTO(savedMessage);

        messagingTemplate.convertAndSend("/topic/conversation." + savedMessage.getConversation().getId(), new MessageEventDTO("EDIT", savedMessage.getConversation().getId(), savedMessage.getId(), dto, null, null));

        return dto;
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long userId, Long conversationId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Long> unreadMessagesList = messageRepository.findUnreadMessageIds(conversationId, userId);

        unreadMessagesList.forEach(messageId -> {
                    Message message = messageRepository.findByIdForUpdate(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));

                    MessageRead messageRead = new MessageRead();

                    messageRead.setMessage(message);
                    messageRead.setUser(user);
                    messageRead.setReadAt(LocalDateTime.now());

                    try {
                        messageReadRepository.save(messageRead);
                    } catch (DataIntegrityViolationException _) {
                        throw new BadRequestException("Couldn't save message");
                    }

                    long countParticipants = conversationParticipantRepository.countActiveOtherParticipants(conversationId, message.getSender().getId());
                    long countRead = messageReadRepository.countByMessage(message);

                    if(countParticipants == countRead){
                        message.setStatus("read");

                        messageRepository.save(message);

                        MessageEventDTO event = new MessageEventDTO();

                        event.setStatus("read");
                        event.setMessageIds(List.of(messageId));
                        event.setType("STATUS");
                        event.setConversationId(message.getConversation().getId());

                        messagingTemplate.convertAndSend("/topic/conversation." + message.getConversation().getId(), event);
                        messagingTemplate.convertAndSend("/queue/user." + message.getSender().getId(), event);
                    }
                });
    }

    @Override
    public void markMessageAsDelivered(Long userId, Long messageId){
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Message message = messageRepository.findByIdForUpdate(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        MessageDelivery messageDelivery = new MessageDelivery();

        messageDelivery.setMessage(message);
        messageDelivery.setUser(user);
        messageDelivery.setDeliveredAt(LocalDateTime.now());

        try {
            messageDeliveryRepository.save(messageDelivery);
        } catch (DataIntegrityViolationException _) {
            throw new BadRequestException("Couldn't save message");
        }

        long countParticipants = conversationParticipantRepository.countActiveOtherParticipants(message.getConversation().getId(), message.getSender().getId());
        long countDelivery = messageDeliveryRepository.countByMessage(message);

        if(countParticipants == countDelivery){
            if(statusRank(message.getStatus()) > statusRank("delivered")){
                return;
            }

            message.setStatus("delivered");

            messageRepository.save(message);

            MessageEventDTO event = new MessageEventDTO();

            event.setStatus("delivered");
            event.setMessageIds(List.of(messageId));
            event.setType("STATUS");
            event.setConversationId(message.getConversation().getId());

            messagingTemplate.convertAndSend("/topic/conversation." + message.getConversation().getId(), event);
            messagingTemplate.convertAndSend("/queue/user." + message.getSender().getId(), event);
        }
    }

    @Override
    @Transactional
    public void markAllUndeliveredAsDelivered(Long userId){
        List<UndeliveredMessageProjection> undelivered = messageRepository.findUndeliveredMessages(userId);

        if(undelivered.isEmpty()){
            return;
        }

        undelivered.forEach(message -> markMessageAsDelivered(userId, message.getMessageId()));
    }

    private int statusRank(String status){
        return switch(status){
            case "sent" -> 1;
            case "delivered" -> 2;
            case "read" -> 3;
            default -> 0;
        };
    }
}