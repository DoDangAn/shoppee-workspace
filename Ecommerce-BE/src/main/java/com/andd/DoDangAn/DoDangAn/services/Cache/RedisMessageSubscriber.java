package com.andd.DoDangAn.DoDangAn.services.Cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            if (channel.equals("comments")) {
                messagingTemplate.convertAndSend("/topic/comments", body);
            } else if (channel.equals("Order")) {
                messagingTemplate.convertAndSend("/topic/Order", body);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý thông báo Redis: {}", e.getMessage());
        }
    }
}