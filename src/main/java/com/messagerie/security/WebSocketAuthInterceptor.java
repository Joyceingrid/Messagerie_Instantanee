package com.messagerie.security;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final JwtDecoder decoder;

    public WebSocketAuthInterceptor(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler handler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("token");
        if (token != null && !token.isBlank()) {
            try {
                Jwt jwt = decoder.decode(token);
                if (jwt.getExpiresAt() != null && Instant.now().isBefore(jwt.getExpiresAt())) {
                    attributes.put("user", Long.parseLong(jwt.getSubject()));
                    attributes.put("expires", jwt.getExpiresAt());
                    return true;
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid handshake credentials.
            }
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler handler, Exception error) {}
}
