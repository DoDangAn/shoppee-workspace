package com.andd.DoDangAn.DoDangAn.services.VNpay;

import com.andd.DoDangAn.DoDangAn.models.VNpay.PaymentRequest;
import com.andd.DoDangAn.DoDangAn.models.response.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class VNPayHandler {

    @Autowired
    private VNPayService vnpayService;

    public Mono<ServerResponse> createPayment(ServerRequest request) {
        return request.bodyToMono(PaymentRequest.class)
                .flatMap(paymentRequest -> vnpayService.createPayment(request, paymentRequest))
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ResponseObject.builder()
                                .status(org.springframework.http.HttpStatus.OK)
                                .message(response.get("message"))
                                .data(response.get("data"))
                                .build()));
    }

    public Mono<ServerResponse> handleIpn(ServerRequest request) {
        return vnpayService.handleIpn(request)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> handleReturn(ServerRequest request) {
        return vnpayService.handleReturn(request)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ResponseObject.builder()
                                .status(org.springframework.http.HttpStatus.OK)
                                .message(response.get("result"))
                                .data(response)
                                .build()));
    }
}