package com.andd.DoDangAn.DoDangAn.Controller;

import com.andd.DoDangAn.DoDangAn.models.*;
import com.andd.DoDangAn.DoDangAn.models.cache.PendingNotification;
import com.andd.DoDangAn.DoDangAn.repository.Cache.PendingNotificationRepository;
import com.andd.DoDangAn.DoDangAn.repository.jpa.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.andd.DoDangAn.DoDangAn.models.response.ProductVariantDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping(path = "/api")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingNotificationRepository pendingNotificationRepository;
    @Autowired
    private OrderInfoRepository orderInfoRepository;

    @Value("${vnpay.payUrl}")
    private String vnp_PayUrl;
    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;
    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;
    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl; // Ví dụ: http://yourdomain/api/payment/vnpay_return
    @Value("${frontend.baseUrl:}")
    private String frontendBaseUrl; // optional, e.g. https://frontend-host (no trailing slash preferred)
    @Value("${vnpay.ipnUrl}")
    private String vnp_IpnUrl;

    private void sendRealTimeNotification(String channel, String destination, Map<String, Object> data) {
        try {
            redisTemplate.convertAndSend(channel, data);
            messagingTemplate.convertAndSend(destination, data);
        } catch (Exception e) {
            logger.warn("Lỗi khi gửi thông báo real-time: {}, lưu vào hàng đợi", e.getMessage());
            savePendingNotification(channel, data);
        }
    }

    @GetMapping("/home")
    public ResponseEntity<Map<String, Object>> home(@RequestParam("field") Optional<String> field,
                                                    @RequestParam("field2") Optional<String> field2,
                                                    @RequestParam("field3") Optional<String> field3) {
        Map<String, Object> data = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedUser = auth.getName();
        String sortField1 = field.orElse("viewCount");
        String sortField2 = field2.orElse("rate");
        // Map FE alias "score" to entity property "rate"
        String sortField3 = field3.orElse("releaseDate");

        Sort sort = Sort.by(Sort.Direction.DESC, sortField1);
        Sort sort2 = Sort.by(Sort.Direction.DESC, sortField2);
        Sort sort3 = Sort.by(Sort.Direction.DESC, sortField3);
        data.put("loggedUser", loggedUser);
        data.put("totalMovies", productRepository.count());
        data.put("totalCategories", categoryRepository.count());
        data.put("totalUsers", userRepository.count());
        data.put("loggedInUser", auth.getPrincipal());
        data.put("category", categoryRepository.findAll());
        data.put("products", productRepository.findAll());
        data.put("price", productRepository.findAll(sort));
        data.put("score", productRepository.findAll(sort2));
        data.put("release", productRepository.findAll(sort3));
        return new ResponseEntity<>(data, HttpStatus.OK);
    }
    

    @PostMapping("/comment")
    @Transactional
    @CacheEvict(value = "comments", key = "#newComment.product.id")
    public ResponseEntity<Map<String, Object>> addComment(@Valid @RequestBody Comment newComment) {
    // removed unused variable 'response'
        logger.info("Nhận yêu cầu thêm bình luận: content={}, productId={}",
                newComment.getContent(), newComment.getProduct() != null ? newComment.getProduct().getId() : "null");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để bình luận."));
        }

        if (newComment.getContent() == null || newComment.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nội dung bình luận không được để trống."));
        }

        Product product = newComment.getProduct();
        if (product == null || !productRepository.existsById(product.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Sản phẩm không tồn tại."));
        }

        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
            newComment.setUser(user);
            newComment.setCreatedAt(LocalDateTime.now());
            newComment.setUpdatedAt(LocalDateTime.now());
            Comment savedComment = commentRepository.save(newComment);

            Map<String, Object> commentData = Map.of(
                    "id", savedComment.getId(),
                    "content", savedComment.getContent(),
                    "username", user.getUsername(),
                    "createdAt", savedComment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                    "productId", product.getId()
            );
            sendRealTimeNotification("comments", "/topic/comments/" + product.getId(), commentData);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bình luận đã được thêm thành công!",
                    "comment", Map.of(
                            "id", savedComment.getId(),
                            "content", savedComment.getContent(),
                            "username", user.getUsername(),
                            "createdAt", savedComment.getCreatedAt().format(formatter)
                    )
            ));
        } catch (Exception e) {
            logger.error("Lỗi khi lưu bình luận: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi lưu bình luận: " + e.getMessage()));
        }
    }

    @PutMapping("/comment/{commentId}")
    @Transactional
    @CacheEvict(value = "comments", key = "#commentId")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody Comment updatedComment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để cập nhật bình luận."));
        }

        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
            Comment existingComment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("Bình luận không tồn tại"));

            if (!existingComment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền cập nhật bình luận này."));
            }

            existingComment.setContent(updatedComment.getContent());
            existingComment.setUpdatedAt(LocalDateTime.now());
            commentRepository.save(existingComment);

            Map<String, Object> commentData = Map.of(
                    "id", existingComment.getId(),
                    "content", existingComment.getContent(),
                    "username", user.getUsername(),
                    "createdAt", existingComment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                    "productId", existingComment.getProduct().getId()
            );
            sendRealTimeNotification("comments", "/topic/comments/" + existingComment.getProduct().getId(), commentData);

            return ResponseEntity.ok(Map.of("message", "Bình luận đã được cập nhật thành công!"));
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật bình luận: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi cập nhật bình luận: " + e.getMessage()));
        }
    }

    @DeleteMapping("/comment/{commentId}")
    @Transactional
    @CacheEvict(value = "comments", key = "#commentId")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable String commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để xóa bình luận."));
        }

        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("Bình luận không tồn tại"));

            if (!comment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền xóa bình luận này."));
            }

            commentRepository.delete(comment);

            Map<String, Object> commentData = Map.of(
                    "id", commentId,
                    "productId", comment.getProduct().getId(),
                    "action", "delete"
            );
            sendRealTimeNotification("comments", "/topic/comments/" + comment.getProduct().getId(), commentData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bình luận đã được xóa thành công!",
                    "commentId", commentId
            ));
        } catch (Exception e) {
            logger.error("Lỗi khi xóa bình luận: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi xóa bình luận: " + e.getMessage()));
        }
    }

    @GetMapping("/orderinfo/{orderInfoId}")
    public Mono<ResponseEntity<Map<String, Object>>> getOrderInfo(@PathVariable String orderInfoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để xem thông tin đơn hàng.")));
        }

        String username = authentication.getName();

        return Mono.fromCallable(() -> {
                    OrderInfo orderInfo = orderInfoRepository.findById(orderInfoId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đơn hàng."));
                    List<Order> orders = orderRepository.findByOrderInfo_Id(orderInfo.getId());
                    return ResponseEntity.ok(Map.of(
                            "message", "Lấy thông tin đơn hàng thành công",
                            "loggedInUser", username,
                            "orders", orders
                    ));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Lỗi khi lấy thông tin đơn hàng: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Có lỗi xảy ra: " + e.getMessage())));
                });
    }

    @PostMapping("/order/add/{variantId}")
    @Transactional
    @CacheEvict(value = "order", key = "#user.id")
    public Mono<ResponseEntity<Map<String, Object>>> addVariantToOrder(
            @PathVariable String variantId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Integer> request) {

        if (user == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để thêm sản phẩm vào đơn hàng.")));
        }

        return Mono.just(user)
                .publishOn(Schedulers.boundedElastic())
                .mapNotNull(currentUser -> {
                    ProductVariant variant = productVariantRepository.findById(variantId)
                            .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại"));

                    if (variant.getQuantity() <= 0) {
                        throw new IllegalStateException("Sản phẩm đã hết hàng");
                    }

                    int quantity = request.getOrDefault("quantity", 1);
                    if (variant.getQuantity() < quantity) {
                        throw new IllegalStateException("Số lượng yêu cầu vượt quá tồn kho");
                    }

                    OrderInfo orderInfo = new OrderInfo();
                    orderInfo.setUser(currentUser);
                    OrderInfo savedOrderInfo = orderInfoRepository.save(orderInfo);

                    Order order = new Order();
                    order.setOrderInfo(savedOrderInfo);
                    order.setProduct(variant.getProduct()); // Giữ liên kết với Product mẫu
                    order.setQuantity(quantity);
                    order.setTotalPrice(variant.getPrice() * quantity); // Tính totalPrice dựa trên variant
                    order.setAddedDate(LocalDateTime.now());
                    orderRepository.save(order);

                    variant.setQuantity(variant.getQuantity() - quantity);
                    productVariantRepository.save(variant);

                    List<Order> orders = orderRepository.findByUserId(currentUser.getId());
                    List<ProductVariant> variants = orders.stream()
                            .map(o -> productVariantRepository.findByProductId(o.getProduct().getId())) // Giả sử cần list variants
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

                    sendRealTimeNotification("order", "/topic/order/" + currentUser.getId(),
                            Map.of("userId", currentUser.getId(), "variantId", variant.getId(), "action", "add"));

                    return ResponseEntity.ok(Map.of(
                            "message", "Đã thêm đơn hàng thành công",
                            "loggedInUser", currentUser.getUsername(),
                            "orderList", variants
                    ));
                })
                .onErrorResume(e -> {
                    logger.error("Lỗi khi thêm sản phẩm vào đơn hàng: {}", e.getMessage(), e);
                    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                    if (e instanceof IllegalArgumentException) {
                        status = HttpStatus.BAD_REQUEST;
                    }
                    if (e instanceof IllegalStateException) {
                        status = HttpStatus.BAD_REQUEST;
                    }
                    return Mono.just(ResponseEntity.status(status)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    @DeleteMapping("/order/remove/{productId}")
    @Transactional
    @CacheEvict(value = "order", key = "#result.loggedInUser.id")
    public Mono<ResponseEntity<Map<String, Object>>> removeFromOrder(@PathVariable String productId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đng nhập để xóa đơn hàng.")));
        }

        return Mono.fromCallable(() -> {
                    String username = authentication.getName();
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

                    List<Order> orders = orderRepository.findByUserIdAndProductId(user.getId(), productId);
                    if (orders.isEmpty()) {
                        throw new IllegalArgumentException("Đơn hàng không tồn tại.");
                    }

                    orderRepository.delete(orders.get(0));

                    List<Order> userOrders = orderRepository.findByUserId(user.getId());
                    List<Product> products = userOrders.stream()
                            .map(Order::getProduct)
                            .collect(Collectors.toList());

                    sendRealTimeNotification("order", "/topic/order/" + user.getId(),
                            Map.of("userId", user.getId(), "productId", productId, "action", "remove"));

                    return ResponseEntity.ok(Map.of(
                            "message", "Đã xóa đơn hàng thành công",
                            "loggedInUser", user,
                            "orderList", products
                    ));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Lỗi khi xóa đơn hàng: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Có lỗi xảy ra: " + e.getMessage())));
                });
    }

    @GetMapping("/product/{productId}")
    @Cacheable(value = "product", key = "#productId")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        logger.info("GET /product/{}", productId);
        try {
            List<ProductVariant> productVariants = productVariantRepository.findByProductId(productId);
            if (productVariants.isEmpty()) {
                logger.warn("No product variants found for productId: {}", productId);
                return ResponseEntity.notFound().build();
            }
            List<Comment> comments = commentRepository.findByProductId(productId);

            return ResponseEntity.ok(Map.of(
                    "product", productVariants.get(0),
                    "productVariants", productVariants,
                    "comments", comments != null ? comments : Collections.emptyList()
            ));
        } catch (Exception e) {
            logger.error("Lỗi khi lấy sản phẩm: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }
    @PostMapping("/payment/vnpay/{orderInfoId}")
    public ResponseEntity<Map<String, Object>> createVnpayPayment(@PathVariable String orderInfoId,
                                                                  @RequestParam String bankCode,
                                                                  @RequestParam String locale,
                                                                  @RequestParam String ipAddr) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để thanh toán."));
        }

        try {
            OrderInfo orderInfo = orderInfoRepository.findById(orderInfoId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đơn hàng."));

            // Tính tổng tiền (amount * 100 vì VNPAY dùng đơn vị VND * 100)
            long amount = orderInfo.getAmount() * 100;

            // Tạo mã đơn hàng ngẫu nhiên hoặc dùng orderInfoId
            String vnp_TxnRef = orderInfo.getId(); // Hoặc UUID.randomUUID().toString()

            // Tạo thời gian
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());

            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());

            // Xây dựng params
            Map<String, String> vnp_Params = new TreeMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_BankCode", bankCode != null ? bankCode : "");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", orderInfo.getOrderInfo() != null ? orderInfo.getOrderInfo() : "Thanh toan don hang " + orderInfo.getId());
            vnp_Params.put("vnp_OrderType", "other"); // Hoặc loại đơn hàng phù hợp
            vnp_Params.put("vnp_Locale", locale != null ? locale : "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", ipAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Tạo hash
            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                    hashData.append('&');
                }
            }
            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
            }

            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(vnp_HashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secretKey);
            byte[] hashBytes = sha512_HMAC.doFinal(hashData.toString().getBytes(StandardCharsets.UTF_8));
            String secureHash = bytesToHex(hashBytes);

            vnp_Params.put("vnp_SecureHash", secureHash);

            // Xây dựng URL
            StringBuilder paymentUrl = new StringBuilder(vnp_PayUrl);
            paymentUrl.append('?');
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    paymentUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                    paymentUrl.append('=');
                    paymentUrl.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                    paymentUrl.append('&');
                }
            }
            if (paymentUrl.charAt(paymentUrl.length() - 1) == '&') {
                paymentUrl.deleteCharAt(paymentUrl.length() - 1);
            }

            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl.toString()));
        } catch (Exception e) {
            logger.error("Lỗi khi tạo URL thanh toán VNPAY: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi tạo URL thanh toán: " + e.getMessage()));
        }
    }

    // Endpoint xử lý return từ VNPAY
    @GetMapping("/payment/vnpay_return")
    public ResponseEntity<Map<String, Object>> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");

            // Tạo hashData từ params
            StringBuilder hashData = new StringBuilder();
            TreeMap<String, String> sortedParams = new TreeMap<>(params);
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                    hashData.append('&');
                }
            }
            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
            }

            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(vnp_HashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secretKey);
            byte[] hashBytes = sha512_HMAC.doFinal(hashData.toString().getBytes(StandardCharsets.UTF_8));
            String checkSum = bytesToHex(hashBytes);

            if (!checkSum.equals(vnp_SecureHash)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chữ ký không hợp lệ"));
            }

            String orderId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");

            OrderInfo orderInfo = orderInfoRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

            if ("00".equals(responseCode)) {
                orderInfo.setStatus("PAID"); // Cập nhật trạng thái đơn hàng
                orderInfoRepository.save(orderInfo);
                sendRealTimeNotification("order", "/topic/order/" + orderInfo.getUser().getId(),
                        Map.of("orderId", orderId, "status", "PAID"));
                // Redirect to frontend payment result page
                try {
                    String msg = URLEncoder.encode("Thanh toán thành công", StandardCharsets.UTF_8.toString());
                    String id = URLEncoder.encode(orderId, StandardCharsets.UTF_8.toString());
                    String path = "/payment/result?message=" + msg + "&orderId=" + id;
                    String target = (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) ? (frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length()-1) + path : frontendBaseUrl + path) : path;
                    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
                } catch (Exception ex) {
                    return ResponseEntity.ok(Map.of("message", "Thanh toán thành công", "orderId", orderId));
                }
            } else {
                try {
                    String msg = URLEncoder.encode("Thanh toán thất bại", StandardCharsets.UTF_8.toString());
                    String id = URLEncoder.encode(orderId == null ? "" : orderId, StandardCharsets.UTF_8.toString());
                    String path = "/payment/result?message=" + msg + "&orderId=" + id;
                    String target = (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) ? (frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length()-1) + path : frontendBaseUrl + path) : path;
                    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
                } catch (Exception ex) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Thanh toán thất bại", "code", responseCode));
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý return VNPAY: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi xử lý thanh toán: " + e.getMessage()));
        }
    }

    // Endpoint xử lý IPN từ VNPAY (nếu cần, asynchronous)
    @PostMapping("/payment/vnpay_ipn")
    public ResponseEntity<Map<String, Object>> vnpayIpn(@RequestParam Map<String, String> params) {
        // Tương tự như return, nhưng không redirect, chỉ cập nhật status
        // Code tương tự vnpayReturn, nhưng return RSP code cho VNPAY
        try {
            // ... (tương tự kiểm tra hash và cập nhật order)
            
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknow error"));
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void savePendingNotification(String channel, Map<String, Object> data) {
        try {
            PendingNotification notification = new PendingNotification();
            notification.setId(UUID.randomUUID().toString());
            notification.setChannel(channel);
            notification.setPayload(objectMapper.writeValueAsString(data));
            notification.setCreatedAt(LocalDateTime.now());
            pendingNotificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Lỗi khi lưu thông báo tạm thời: {}", e.getMessage());
        }
    }


    // Simple list endpoint at /products returning plain array (FE friendly)
    @GetMapping("/products")
    @Transactional
    public ResponseEntity<List<ProductVariantDTO>> listProductVariants(
            @RequestParam(value = "search", required = false) String search) {
        List<ProductVariant> variants = (search != null && !search.isBlank()) ?
                productVariantRepository.findByProductNameWithJoins(search.trim()) :
                productVariantRepository.findAllWithJoinsOrderByDateDesc();
        logger.info("/api/products returning {} items (search={})", variants.size(), search);
        return ResponseEntity.ok(variants.stream().map(ProductVariantDTO::fromEntity).toList());
    }

    // Paginated / filtered version moved to /products/page
    @GetMapping("/products/page")
    @Transactional
    public ResponseEntity<Map<String,Object>> getProductVariantsPaged(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "countryId", required = false) String countryId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "date") String sort,
            @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction) {
        search = (search != null && !search.isBlank()) ? search.trim() : null;
        categoryId = (categoryId != null && !categoryId.isBlank()) ? categoryId : null;
        countryId = (countryId != null && !countryId.isBlank()) ? countryId : null;
        String sortField = switch (sort) {
            case "price", "viewCount", "date", "productName" -> sort;
            default -> "date";
        };
        var dir = "asc".equalsIgnoreCase(direction) ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(dir, sortField));
        var pageResult = productVariantRepository.searchPaged(search, categoryId, countryId, pageable);
        long totalRowsInTable = productVariantRepository.count();
        logger.info("/api/products/page search={} categoryId={} countryId={} page={} size={} sort={} dir={} -> {} items (page totalElements={} tableCount={})",
                search, categoryId, countryId, page, size, sortField, dir, pageResult.getNumberOfElements(), pageResult.getTotalElements(), totalRowsInTable);
        var dtoList = pageResult.getContent().stream().map(ProductVariantDTO::fromEntity).toList();
        Map<String,Object> body = new HashMap<>();
        body.put("content", dtoList);
        body.put("page", pageResult.getNumber());
        body.put("size", pageResult.getSize());
        body.put("totalElements", pageResult.getTotalElements());
        body.put("totalPages", pageResult.getTotalPages());
        body.put("last", pageResult.isLast());
        return ResponseEntity.ok(body);
    }
}