package com.andd.DoDangAn.DoDangAn.repository.jpa;

import com.andd.DoDangAn.DoDangAn.models.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OrderInfoRepository extends JpaRepository<OrderInfo, String> {
    //List<OrderInfo> findByUserId(String userId); // Nếu cần lọc theo user
}