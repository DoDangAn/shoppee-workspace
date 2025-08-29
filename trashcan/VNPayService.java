package com.andd.DoDangAn.DoDangAn.services.VNpay;

import com.andd.DoDangAn.DoDangAn.config.VNpay.VNPayConfig;
import com.andd.DoDangAn.DoDangAn.models.OrderInfo;
import com.andd.DoDangAn.DoDangAn.models.VNpay.PaymentRequest;
import com.andd.DoDangAn.DoDangAn.repository.jpa.OrderInfoRepository;
import com.andd.DoDangAn.DoDangAn.repository.jpa.OrderRepository;
import com.andd.DoDangAn.DoDangAn.services.IVNPayService;
import com.andd.DoDangAn.DoDangAn.Util.VNpay.VNPayUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayService implements IVNPayService {

    private static final Logger logger = LoggerFactory.getLogger(VNPayService.class);

    private final VNPayConfig vnpayConfig;
    private final VNPayUtils vnpayUtils;
    private final OrderRepository orderRepository;
    private final OrderInfoRepository orderInfoRepository;

    @Override
    public Mono<Map<String, Object>> createPayment(ServerRequest request, PaymentRequest paymentRequest) {
        return Mono.defer(() -> {
            try {
                // Tạo orderId ngẫu nhiên và kiểm tra định dạng
                String orderIdStr = vnpayUtils.getRandomNumber(8);
                Long orderId = Long.valueOf(orderIdStr); // Chuyển sang Long để khớp với JPA
                OrderInfo order = new OrderInfo();
                order.setId(orderId); // Sử dụng id thay vì orderId để khớp với JPA
                order.setAmount(paymentRequest.getAmount());
                order.setStatus("0"); // Đang chờ xử lý
                order.setOrderInfo(paymentRequest.getOrderInfo());
                order.setCreatedDate(Instant.now());

                return orderInfoRepository.save(order)
                        .then(Mono.defer(() -> buildPaymentUrl(orderIdStr, paymentRequest, request)))
                        .onErrorResume(e -> {
                            logger.error("Lỗi khi tạo thanh toán cho orderId {}: {}", orderIdStr, e.getMessage(), e);
                            return Mono.just(createErrorResponse("99", "Lỗi máy chủ: " + e.getMessage()));
                        });
            } catch (NumberFormatException e) {
                logger.error("Lỗi định dạng orderId: {}", e.getMessage(), e);
                return Mono.just(createErrorResponse("98", "Lỗi định dạng orderId"));
            } catch (Exception e) {
                logger.error("Lỗi không xác định khi tạo thanh toán: {}", e.getMessage(), e);
                return Mono.just(createErrorResponse("99", "Lỗi không xác định: " + e.getMessage()));
            }
        });
    }

    private Mono<Map<String, Object>> buildPaymentUrl(String orderId, PaymentRequest paymentRequest, ServerRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(paymentRequest.getAmount() * 100));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", paymentRequest.getOrderInfo());
        vnpParams.put("vnp_OrderType", Optional.ofNullable(paymentRequest.getOrderType()).orElse("other"));
        vnpParams.put("vnp_Locale", Optional.ofNullable(paymentRequest.getLanguage()).orElse("vn"));
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", vnpayUtils.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        if (paymentRequest.getBankCode() != null && !paymentRequest.getBankCode().isEmpty()) {
            vnpParams.put("vnp_BankCode", paymentRequest.getBankCode());
        }

        return generatePaymentUrl(vnpParams)
                .map(paymentUrl -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("code", "00");
                    response.put("message", "Thành công");
                    response.put("data", paymentUrl);
                    return response;
                });
    }

    private Mono<String> generatePaymentUrl(Map<String, String> vnpParams) {
        return Mono.fromCallable(() -> {
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    try {
                        String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                        hashData.append(fieldName).append('=').append(encodedValue);
                        query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                                .append('=').append(encodedValue);
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi mã hóa: " + e.getMessage(), e);
                    }
                    if (!fieldName.equals(fieldNames.get(fieldNames.size() - 1))) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }

            String vnpSecureHash = vnpayUtils.hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
            query.append("&vnp_SecureHash=").append(vnpSecureHash);
            return vnpayConfig.getPaymentUrl() + "?" + query.toString();
        });
    }

    @Override
    public Mono<Map<String, Object>> handleIpn(ServerRequest request) {
        return Mono.just(request.queryParams())
                .flatMap(params -> {
                    String vnpSecureHash = params.getFirst("vnp_SecureHash");
                    if (vnpSecureHash == null) {
                        return Mono.just(createErrorResponse("97", "Thiếu vnp_SecureHash"));
                    }

                    Map<String, String> vnpParams = new HashMap<>();
                    params.forEach((key, values) -> vnpParams.put(key, values.get(0)));
                    vnpParams.remove("vnp_SecureHash");

                    return verifySignatureAndUpdateOrder(vnpParams, vnpSecureHash);
                })
                .onErrorResume(e -> {
                    logger.error("Lỗi khi xử lý IPN: {}", e.getMessage(), e);
                    return Mono.just(createErrorResponse("99", "Lỗi không xác định: " + e.getMessage()));
                });
    }

    private Mono<Map<String, Object>> verifySignatureAndUpdateOrder(Map<String, String> vnpParams, String vnpSecureHash) {
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (!fieldName.equals(fieldNames.get(fieldNames.size() - 1))) {
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    return Mono.just(createErrorResponse("99", "Lỗi mã hóa: " + e.getMessage()));
                }
            }
        }

        String secureHash = vnpayUtils.hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
        String orderIdStr = vnpParams.get("vnp_TxnRef");
        if (orderIdStr == null) {
            return Mono.just(createErrorResponse("01", "Thiếu vnp_TxnRef"));
        }

        Long orderId;
        try {
            orderId = Long.valueOf(orderIdStr);
        } catch (NumberFormatException e) {
            return Mono.just(createErrorResponse("98", "Lỗi định dạng orderId"));
        }

        String vnpAmountStr = vnpParams.get("vnp_Amount");
        if (vnpAmountStr == null) {
            return Mono.just(createErrorResponse("04", "Thiếu vnp_Amount"));
        }

        long vnpAmount;
        try {
            vnpAmount = Long.parseLong(vnpAmountStr) / 100;
        } catch (NumberFormatException e) {
            return Mono.just(createErrorResponse("04", "Định dạng số tiền không hợp lệ"));
        }

        String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
        String vnpTransactionStatus = vnpParams.get("vnp_TransactionStatus");

        if (!secureHash.equals(vnpSecureHash)) {
            return Mono.just(createErrorResponse("97", "Chữ ký không hợp lệ"));
        }

        return orderInfoRepository.findById(orderId)
                .switchIfEmpty(Mono.just(new OrderInfo()))
                .flatMap(order -> {
                    if (order.getId() == null) {
                        return Mono.just(createErrorResponse("01", "Không tìm thấy đơn hàng"));
                    }
                    if (order.getAmount() != vnpAmount) {
                        return Mono.just(createErrorResponse("04", "Số tiền không hợp lệ"));
                    }
                    if (!"0".equals(order.getStatus())) {
                        return Mono.just(createErrorResponse("02", "Đơn hàng đã được xác nhận"));
                    }
                    order.setStatus("00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus) ? "1" : "2");
                    return orderInfoRepository.save(order)
                            .thenReturn(createErrorResponse("00", "Xác nhận thành công"));
                });
    }

    @Override
    public Mono<Map<String, Object>> handleReturn(ServerRequest request) {
        return Mono.just(request.queryParams())
                .flatMap(params -> {
                    String vnpSecureHash = params.getFirst("vnp_SecureHash");
                    if (vnpSecureHash == null) {
                        return Mono.just(createErrorResponse("97", "Thiếu vnp_SecureHash"));
                    }

                    Map<String, String> vnpParams = new HashMap<>();
                    params.forEach((key, values) -> vnpParams.put(key, values.get(0)));
                    vnpParams.remove("vnp_SecureHash");

                    return verifyReturnSignature(vnpParams, vnpSecureHash);
                })
                .onErrorResume(e -> {
                    logger.error("Lỗi khi xử lý return: {}", e.getMessage(), e);
                    return Mono.just(createErrorResponse("99", "Lỗi không xác định: " + e.getMessage()));
                });
    }

    private Mono<Map<String, Object>> verifyReturnSignature(Map<String, String> vnpParams, String vnpSecureHash) {
        return Mono.fromCallable(() -> {
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    try {
                        hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                        if (!fieldName.equals(fieldNames.get(fieldNames.size() - 1))) {
                            hashData.append('&');
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi mã hóa: " + e.getMessage(), e);
                    }
                }
            }

            String secureHash = vnpayUtils.hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", vnpParams.get("vnp_TxnRef") != null ? vnpParams.get("vnp_TxnRef") : "");
            response.put("amount", vnpParams.get("vnp_Amount") != null ? Long.parseLong(vnpParams.get("vnp_Amount")) / 100 : 0);
            response.put("orderInfo", vnpParams.get("vnp_OrderInfo") != null ? vnpParams.get("vnp_OrderInfo") : "");
            response.put("transactionNo", vnpParams.get("vnp_TransactionNo") != null ? vnpParams.get("vnp_TransactionNo") : "");
            response.put("responseCode", vnpParams.get("vnp_ResponseCode") != null ? vnpParams.get("vnp_ResponseCode") : "");

            response.put("result", secureHash.equals(vnpSecureHash)
                    ? "00".equals(vnpParams.get("vnp_ResponseCode")) ? "Thành công" : "Lỗi: " + vnpParams.get("vnp_ResponseCode")
                    : "Lỗi: Chữ ký không hợp lệ");
            return response;
        });
    }

    private Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }
}