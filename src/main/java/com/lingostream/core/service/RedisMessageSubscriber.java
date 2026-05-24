package com.lingostream.core.service;

import com.lingostream.core.entity.SubtitleEntity;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageSubscriber {

    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;

    public RedisMessageSubscriber(RedissonClient redissonClient, SimpMessagingTemplate messagingTemplate) {
        this.redissonClient = redissonClient;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void subscribeToUpdates() {
        // 1. Connect to a global Redis channel
        RTopic topic = redissonClient.getTopic("global-subtitle-updates");

        // 2. Listen for any SubtitleEntity published by ANY server in the cluster
        topic.addListener(SubtitleEntity.class, (channel, msg) -> {

            // 3. Take the global Redis message and push it to local WebSocket clients
            messagingTemplate.convertAndSend("/topic/subtitles/" + msg.getVideoId(), msg);

            System.out.println("Intercepted Redis broadcast and pushed to local WebSockets: " + msg.getId());
        });
    }
}