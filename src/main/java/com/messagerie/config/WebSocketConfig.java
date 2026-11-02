package com.messagerie.config;

import com.messagerie.message.MessageWebSocketHandler;
import com.messagerie.security.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final MessageWebSocketHandler messages;
    private final WebSocketAuthInterceptor auth;

    public WebSocketConfig(MessageWebSocketHandler messages, WebSocketAuthInterceptor auth) {
        this.messages = messages;
        this.auth = auth;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messages, "/ws/messages").addInterceptors(auth);
    }
}
