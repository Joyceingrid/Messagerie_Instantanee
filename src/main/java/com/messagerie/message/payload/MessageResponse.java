package com.messagerie.message.payload;

import com.messagerie.message.Message;
import java.time.Instant;

public record MessageResponse(String type, Long id, Long expediteurId, Long destinataireId,
                              String contenu, Instant dateEnvoi) {
    public static MessageResponse from(Message m) {
        return new MessageResponse("message", m.getId(), m.getExpediteur().getId(),
                m.getDestinataire().getId(), m.getContenu(), m.getDateEnvoi());
    }
}
