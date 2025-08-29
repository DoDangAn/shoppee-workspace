package com.andd.DoDangAn.DoDangAn.services.Cache;

import com.andd.DoDangAn.DoDangAn.models.cache.PendingNotification;
import com.andd.DoDangAn.DoDangAn.repository.Cache.PendingNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationSyncService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSyncService.class);

    @Autowired
    private PendingNotificationRepository pendingNotificationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedRate = 60000) // Mỗi phút
    public void syncPendingNotifications() {
        List<PendingNotification> notifications = pendingNotificationRepository.findAll();
        for (PendingNotification notification : notifications) {
            try {
                Map<String, Object> data = objectMapper.readValue(notification.getPayload(), Map.class);
                redisTemplate.convertAndSend(notification.getChannel(), data);
                messagingTemplate.convertAndSend("/topic/" + notification.getChannel() + "/" + data.get("productId"), data);
                pendingNotificationRepository.delete(notification);
            } catch (Exception e) {
                logger.error("Lỗi khi đồng bộ thông báo: {}", e.getMessage());
            }
        }
    }
}