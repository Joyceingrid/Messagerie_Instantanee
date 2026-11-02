package com.messagerie.user;

import com.messagerie.user.payload.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController {
    private final UserRepository users;

    private final UserService directory;

    public UserController(UserRepository users, UserService directory) {
        this.users = users;
        this.directory = directory;
    }

    @GetMapping("/api/utilisateurs")
    public java.util.List<UserService.Contact> contacts() {
        return directory.contacts();
    }

    @GetMapping("/api/comptes/me")
    public UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        User user = users.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new UserResponse(user.getId(), user.getUsername());
    }
}
