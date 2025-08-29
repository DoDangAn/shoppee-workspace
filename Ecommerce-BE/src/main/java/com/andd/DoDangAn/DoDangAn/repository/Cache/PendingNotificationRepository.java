package com.andd.DoDangAn.DoDangAn.repository.Cache;

import com.andd.DoDangAn.DoDangAn.models.cache.PendingNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingNotificationRepository extends JpaRepository<PendingNotification, String> {
}