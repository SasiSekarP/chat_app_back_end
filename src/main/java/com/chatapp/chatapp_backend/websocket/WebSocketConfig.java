package com.chatapp.chatapp_backend.websocket;

import com.chatapp.chatapp_backend.service.PersonalChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PersonalChatService personalChatService;

    @Autowired
    public WebSocketConfig(PersonalChatService personalChatService) {
        this.personalChatService = personalChatService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatHandler(personalChatService), "/chat")
                .setAllowedOrigins("*"); // Allow cross-origin requests
    }
}
