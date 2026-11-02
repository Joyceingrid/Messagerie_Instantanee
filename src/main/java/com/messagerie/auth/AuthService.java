package com.messagerie.auth;

import com.messagerie.auth.payload.*;
import com.messagerie.user.*;
import com.messagerie.user.payload.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public UserResponse register(RegisterRequest request) {
        String username = request.username().strip();
        User user = new User(username, passwords.encode(request.motDePasse()));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (users.findByUsername(username).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Identifiant deja utilise");
            }
            throw e;
        }
        return new UserResponse(user.getId(), user.getUsername());
    }

    public LoginResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username().strip())
                .filter(u -> passwords.matches(request.motDePasse(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new LoginResponse(tokens.issue(user.getId()));
    }
}
