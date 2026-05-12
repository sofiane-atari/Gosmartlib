package edu.ap.bib_backend.service.gamification.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;

@Service
public class GamificationNotificationSseService {

    private static final long SSE_TIMEOUT_MILLISECONDS = 30L * 60L * 1000L;
    private static final String NOTIFICATION_EVENT_NAME = "gamification-notification";

    private final Map<String, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLISECONDS);

        emittersByUserId
                .computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        sendConnectionOpenedEvent(emitter);

        return emitter;
    }

    public void sendNotificationToUser(
        String userId,
        GamificationNotificationDTO notification
    ) {

        List<SseEmitter> emitters = emittersByUserId.get(userId);

        if (emitters == null || emitters.isEmpty()) {
            return;
        }


        for (SseEmitter emitter : emitters) {
            sendNotification(userId, emitter, notification);
        }
    }

    private void sendNotification(
            String userId,
            SseEmitter emitter,
            GamificationNotificationDTO notification
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(NOTIFICATION_EVENT_NAME)
                    .data(notification));
        } catch (IOException exception) {
            removeEmitter(userId, emitter);
        }
    }

    private void sendConnectionOpenedEvent(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException ignored) {
            emitter.complete();
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);

        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);

        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}