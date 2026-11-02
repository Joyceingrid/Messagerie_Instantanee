package com.messagerie.user;

import com.messagerie.presence.PresenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public record Contact(Long id, String username, boolean connecte) {}

    private final UserRepository users;
    private final PresenceService presence;

    public UserService(UserRepository users, PresenceService presence) {
        this.users = users;
        this.presence = presence;
    }

    public List<Contact> contacts() {
        return users.findAll(Sort.by("username")).stream()
                .map(u -> new Contact(u.getId(), u.getUsername(), presence.online(u.getId()))).toList();
    }
}
