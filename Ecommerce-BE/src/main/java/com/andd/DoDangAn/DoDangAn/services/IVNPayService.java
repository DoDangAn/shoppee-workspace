package com.andd.DoDangAn.DoDangAn.services;

import com.andd.DoDangAn.DoDangAn.models.VNpay.PaymentRequest;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface IVNPayService {
    Mono<Map<String, String>> createPayment(ServerRequest request, PaymentRequest paymentRequest);
    Mono<Map<String, String>> handleIpn(ServerRequest request);
    Mono<Map<String, String>> handleReturn(ServerRequest request);
}
