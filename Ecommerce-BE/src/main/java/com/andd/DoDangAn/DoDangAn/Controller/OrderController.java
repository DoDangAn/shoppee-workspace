package com.andd.DoDangAn.DoDangAn.Controller;

import com.andd.DoDangAn.DoDangAn.models.*;
import com.andd.DoDangAn.DoDangAn.repository.jpa.OrderInfoRepository;
import com.andd.DoDangAn.DoDangAn.repository.jpa.OrderRepository;
import com.andd.DoDangAn.DoDangAn.repository.jpa.ProductVariantRepository;
import com.andd.DoDangAn.DoDangAn.repository.jpa.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;
    private final OrderInfoRepository orderInfoRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    public OrderController(OrderRepository orderRepository,
                           OrderInfoRepository orderInfoRepository,
                           ProductVariantRepository variantRepository,
                           UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderInfoRepository = orderInfoRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> listMyOrders() {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","Login required"));
        List<Order> orders = orderRepository.findByUserId(user.getId());
        // group by orderInfo
        Map<String, Map<String,Object>> grouped = new LinkedHashMap<>();
        for (Order o: orders) {
            String oiId = o.getOrderInfo().getId();
            Map<String,Object> g = grouped.computeIfAbsent(oiId, k -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id", k);
                m.put("date", o.getAddedDate());
                m.put("status", o.getOrderInfo().getStatus()!=null?o.getOrderInfo().getStatus():"pending");
                m.put("items", new ArrayList<>());
                m.put("total", 0.0);
                return m;
            });
            @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String,Object>>) g.get("items");
            Map<String,Object> line = new LinkedHashMap<>();
            line.put("productId", o.getProduct().getId());
            line.put("name", o.getProduct().getProductName());
            line.put("quantity", o.getQuantity());
            line.put("price", o.getTotalPrice() / o.getQuantity());
            items.add(line);
            double newTotal = (double) g.get("total") + o.getTotalPrice();
            g.put("total", newTotal);
        }
        return ResponseEntity.ok(new ArrayList<>(grouped.values()));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody Map<String,Object> payload) {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","Login required"));
        @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String,Object>>) payload.get("items");
        if (items == null || items.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","items required"));

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUser(user);
        orderInfo.setSeller(user); // placeholder
        orderInfo.setStatus("PENDING");
        orderInfo.setCreatedDate(Instant.now());
        OrderInfo savedInfo = orderInfoRepository.save(orderInfo);

        double total = 0d;
        for (Map<String,Object> it: items) {
            String productId = Objects.toString(it.get("productId"), null);
            int quantity;
            try { quantity = Integer.parseInt(it.getOrDefault("quantity",1).toString()); } catch (Exception e) { quantity = 1; }
            if (productId == null || quantity <= 0) continue;
            var variantOpt = variantRepository.findById(productId);
            if (variantOpt.isEmpty()) continue;
            var variant = variantOpt.get();
            Order order = new Order();
            order.setUser(user);
            order.setProduct(variant.getProduct());
            order.setQuantity(quantity);
            order.setTotalPrice(variant.getPrice() * quantity);
            order.setAddedDate(LocalDateTime.now());
            order.setOrderInfo(savedInfo);
            orderRepository.save(order);
            total += variant.getPrice() * quantity;
        }
    savedInfo.setAmount((long) total);
    orderInfoRepository.save(savedInfo);
    log.info("Created orderInfo {} with {} items amount {}", savedInfo.getId(), items.size(), total);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("orderInfoId", savedInfo.getId(), "total", total));
    }
}
