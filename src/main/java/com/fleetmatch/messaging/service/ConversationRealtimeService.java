package com.fleetmatch.messaging.service;

import com.fleetmatch.messaging.dto.ConversationStreamEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class ConversationRealtimeService {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByConversation =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID conversationId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        emittersByConversation
                .computeIfAbsent(conversationId, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(conversationId, emitter));
        emitter.onTimeout(() -> removeEmitter(conversationId, emitter));
        emitter.onError(ignored -> removeEmitter(conversationId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("subscribed"));
        } catch (IOException exception) {
            removeEmitter(conversationId, emitter);
        }

        return emitter;
    }

    public void publish(UUID conversationId, ConversationStreamEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                event
        );

        var emitters = emittersByConversation.get(conversationId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType())
                        .data(event));
            } catch (IOException exception) {
                removeEmitter(conversationId, emitter);
            }
        });
    }

    private void removeEmitter(UUID conversationId, SseEmitter emitter) {
        var emitters = emittersByConversation.get(conversationId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByConversation.remove(conversationId);
        }
    }
}
