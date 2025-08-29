package com.andd.DoDangAn.DoDangAn.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.andd.DoDangAn.DoDangAn.models.ProductVariant;
import com.andd.DoDangAn.DoDangAn.repository.jpa.ProductVariantRepository;

import java.util.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    // In-memory simple cart storage keyed by a pseudo user (for demo / placeholder)
    private static final Map<String, List<Map<String, Object>>> CARTS = new HashMap<>();

    private final ProductVariantRepository variantRepository;

    public CartController(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    private String getKey() {
        // Placeholder key (in real app use authenticated user id)
        return "guest";
    }

    @GetMapping
    public ResponseEntity<?> getCart() {
        return ResponseEntity.ok(CARTS.getOrDefault(getKey(), Collections.emptyList()));
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> request) {
        logger.info("Add to cart: {}", request);
        String productId = Objects.toString(request.get("productId"), null);
        int quantity = 1;
        try {
            if (request.get("quantity") != null) {
                quantity = Integer.parseInt(request.get("quantity").toString());
            }
        } catch (NumberFormatException ignored) {}

        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "productId required"));
        }

        ProductVariant variant = variantRepository.findById(productId).orElse(null);
        if (variant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product variant not found"));
        }

        List<Map<String, Object>> cart = CARTS.computeIfAbsent(getKey(), k -> new ArrayList<>());
        // Try merge existing line
        for (Map<String, Object> line : cart) {
            if (productId.equals(line.get("productId"))) {
                int oldQ = (int) line.getOrDefault("quantity", 1);
                line.put("quantity", oldQ + quantity);
                return ResponseEntity.ok(Map.of("success", true, "cart", cart));
            }
        }
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("productId", productId);
        line.put("name", variant.getProductName());
        line.put("price", variant.getPrice());
        line.put("quantity", quantity);
        line.put("image", variant.getImageUrl());
        cart.add(line);
        return ResponseEntity.ok(Map.of("success", true, "cart", cart));
    }

    @PutMapping
    public ResponseEntity<?> updateCart(@RequestBody List<Map<String, Object>> items) {
        logger.info("Replace cart items: {}", items.size());
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> reqLine : items) {
            String productId = Objects.toString(reqLine.get("productId"), null);
            if (productId == null) continue;
            ProductVariant variant = variantRepository.findById(productId).orElse(null);
            if (variant == null) continue;
            int quantity;
            try { quantity = Integer.parseInt(reqLine.getOrDefault("quantity", 1).toString()); } catch (NumberFormatException e) { quantity = 1; }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("productId", productId);
            line.put("name", variant.getProductName());
            line.put("price", variant.getPrice());
            line.put("quantity", quantity);
            line.put("image", variant.getImageUrl());
            enriched.add(line);
        }
        CARTS.put(getKey(), enriched);
        return ResponseEntity.ok(Map.of("success", true, "cart", enriched));
    }

    @DeleteMapping("/{index}")
    public ResponseEntity<?> removeItem(@PathVariable int index) {
        List<Map<String, Object>> list = CARTS.getOrDefault(getKey(), new ArrayList<>());
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            return ResponseEntity.ok(Map.of("success", true, "cart", list));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid index"));
    }
}