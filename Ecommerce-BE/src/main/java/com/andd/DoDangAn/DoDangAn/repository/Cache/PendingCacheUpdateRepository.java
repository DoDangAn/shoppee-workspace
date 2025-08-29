package com.andd.DoDangAn.DoDangAn.repository.Cache;

import com.andd.DoDangAn.DoDangAn.models.cache.PendingCacheUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingCacheUpdateRepository extends JpaRepository<PendingCacheUpdate, String> {
}