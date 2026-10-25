package com.chatappbackend.backend.service.message;

import com.chatappbackend.backend.entity.Message;
import com.chatappbackend.backend.entity.MessageEmbedding;
import com.chatappbackend.backend.exception.EmbeddingException;
import com.chatappbackend.backend.repository.MessageEmbeddingRepository;
import com.chatappbackend.backend.repository.MessageRepository;
import com.chatappbackend.backend.service.embedding.EmbeddingService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class MessageEmbeddingWriter {
    private final MessageRepository messageRepository;
    private final MessageEmbeddingRepository messageEmbeddingRepository;
    private final EmbeddingService embeddingService;

    public MessageEmbeddingWriter(MessageRepository messageRepository, MessageEmbeddingRepository messageEmbeddingRepository, EmbeddingService embeddingService) {
        this.messageRepository = messageRepository;
        this.messageEmbeddingRepository = messageEmbeddingRepository;
        this.embeddingService = embeddingService;
    }

    @Async("embeddingExecutor")
    @Transactional
    public void embedMessage(Long messageId){
        try {
            Optional<Message> optionalMessage = messageRepository.findById(messageId);

            if(optionalMessage.isEmpty()){
                return;
            }

            Message message = optionalMessage.get();

            float[] embedding = embeddingService.messageToVector(message.getMessage());

            MessageEmbedding messageEmbedding = new MessageEmbedding();

            messageEmbedding.setMessage(message);
            messageEmbedding.setEmbedding(embedding);

            messageEmbeddingRepository.save(messageEmbedding);
        } catch (EmbeddingException e) {
            log.warn("Embedding failed for message {}, the sweep will retry", messageId, e);
        } catch (Exception e) {
            log.error("Unexpected error embedding message {}", messageId, e);
        }
    }
}