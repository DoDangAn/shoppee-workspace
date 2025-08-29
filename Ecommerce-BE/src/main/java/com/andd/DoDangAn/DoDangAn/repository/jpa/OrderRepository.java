package com.andd.DoDangAn.DoDangAn.repository.jpa;

import com.andd.DoDangAn.DoDangAn.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByOrderInfo_Id(String orderInfoId);
    //List<Order> findByProductId(int productId);

    List<Order> findByUserIdAndProductId(String id, String productId);
}