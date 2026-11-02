package com.messagerie.message;

import com.messagerie.message.payload.MessageResponse;
import com.messagerie.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {
    private final MessageRepository messages;
    private final UserRepository users;

    public MessageService(MessageRepository messages, UserRepository users) {
        this.messages = messages;
        this.users = users;
    }

    @Transactional
    public MessageResponse send(long sender, long recipient, String content) {
        if (content == null || content.isBlank() || content.length() > 4000) {
            throw new IllegalArgumentException("Le texte doit contenir entre 1 et 4000 caractères.");
        }
        var target = users.findById(recipient)
                .orElseThrow(() -> new IllegalArgumentException("Destinataire inconnu."));
        var message = new Message(users.getReferenceById(sender), target, content.strip());
        return MessageResponse.from(messages.save(message));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> history(long user, long peer) {
        return messages.findConversation(user, peer).stream().map(MessageResponse::from).toList();
    }
}
