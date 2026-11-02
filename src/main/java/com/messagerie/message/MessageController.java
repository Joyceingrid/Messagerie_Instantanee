package com.messagerie.message;

import com.messagerie.message.payload.MessageResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class MessageController {
    private final MessageService messages;

    public MessageController(MessageService messages) {
        this.messages = messages;
    }

    @GetMapping("/api/messages/{utilisateurId}")
    public List<MessageResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable long utilisateurId) {
        return messages.history(Long.parseLong(jwt.getSubject()), utilisateurId);
    }
}
