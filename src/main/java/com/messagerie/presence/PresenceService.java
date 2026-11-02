package com.messagerie.presence;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Service
public class PresenceService {
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void add(WebSocketSession session) {
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5000, 131072));
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    private boolean active(WebSocketSession s, long user) {
        return s.isOpen() && (long) s.getAttributes().get("user") == user
                && Instant.now().isBefore((Instant) s.getAttributes().get("expires"));
    }

    public boolean online(long user) {
        return sessions.values().stream().anyMatch(s -> active(s, user));
    }

    public void deliver(long user, TextMessage frame) {
        for (WebSocketSession session : sessions.values()) {
            if (active(session, user)) {
                try {
                    session.sendMessage(frame);
                } catch (IOException | RuntimeException e) {
                    remove(session);
                    try {
                        session.close(CloseStatus.SERVER_ERROR);
                    } catch (IOException ignored) {
                        // Transport already closed.
                    }
                }
            }
        }
    }
}
