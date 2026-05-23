package com.lingostream.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // This is the prefix for the "channels" users can subscribe to (e.g., /topic/subtitles)
        config.enableSimpleBroker("/topic");

        // This is the prefix for messages sent FROM the browser TO the server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the URL browsers will use to establish the initial connection
        // setAllowedOriginPatterns("*") is required for local testing so our frontend isn't blocked by CORS
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}