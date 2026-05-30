package edu.ap.bib_backend.service.gamification.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;

@Service
public class GamificationNotificationSseService {

    private static final long SSE_TIMEOUT_MILLISECONDS = 30L * 60L * 1000L;
    private static final long HEARTBEAT_INTERVAL_MILLISECONDS = 25_000L;
    private static final String NOTIFICATION_EVENT_NAME = "gamification-notification";
    private static final String CONNECTED_EVENT_NAME = "connected";
    private static final String HEARTBEAT_EVENT_NAME = "heartbeat";

    private final Map<String, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLISECONDS);

        emittersByUserId
                .computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(userId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> {
            removeEmitter(userId, emitter);
            emitter.completeWithError(error);
        });

        sendEvent(userId, emitter, CONNECTED_EVENT_NAME, "connected");

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
            sendEvent(userId, emitter, NOTIFICATION_EVENT_NAME, notification);
        }
    }

    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MILLISECONDS)
    public void sendHeartbeat() {
        for (Map.Entry<String, List<SseEmitter>> entry : emittersByUserId.entrySet()) {
            String userId = entry.getKey();

            for (SseEmitter emitter : entry.getValue()) {
                sendEvent(userId, emitter, HEARTBEAT_EVENT_NAME, "ping");
            }
        }
    }

    private void sendEvent(String userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException | IllegalStateException exception) {
            closeEmitter(userId, emitter, exception);
        }
    }

    private void closeEmitter(String userId, SseEmitter emitter, Throwable error) {
        removeEmitter(userId, emitter);

        if (error == null) {
            emitter.complete();
            return;
        }

        emitter.completeWithError(error);
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);

        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);

        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId, emitters);
        }
    }
}
