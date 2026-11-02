package com.messagerie.message;

import com.messagerie.message.payload.MessageRequest;
import com.messagerie.presence.PresenceService;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {
    private final PresenceService presence;
    private final MessageService messages;
    private final ObjectMapper json;

    public MessageWebSocketHandler(PresenceService presence, MessageService messages, ObjectMapper json) {
        this.presence = presence;
        this.messages = messages;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(65536);
        presence.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        presence.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage frame) throws Exception {
        if (!Instant.now().isBefore((Instant) session.getAttributes().get("expires"))) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        long sender = (long) session.getAttributes().get("user");
        try {
            var request = json.readValue(frame.getPayload(), MessageRequest.class);
            if (request == null || request.destinataireId() == null || request.destinataireId() == sender) {
                throw new IllegalArgumentException("Sélectionnez un correspondant.");
            }
            long recipient = request.destinataireId();
            if ("typing".equals(request.type())) {
                presence.deliver(recipient, encode(Map.of("type", "typing", "expediteurId", sender)));
            } else if ("message".equals(request.type())) {
                var saved = encode(messages.send(sender, recipient, request.contenu()));
                presence.deliver(sender, saved);
                presence.deliver(recipient, saved);
            } else {
                throw new IllegalArgumentException("Type inconnu.");
            }
        } catch (JacksonException | IllegalArgumentException e) {
            presence.deliver(sender, encode(Map.of("type", "error", "detail", "Message invalide.")));
        }
    }

    private TextMessage encode(Object value) {
        return new TextMessage(json.writeValueAsString(value));
    }
}
