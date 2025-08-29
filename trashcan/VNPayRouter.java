package com.andd.DoDangAn.DoDangAn.config.VNpay;

import com.andd.DoDangAn.DoDangAn.services.VNpay.VNPayHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class VNPayRouter {

    @Bean
    public RouterFunction<ServerResponse> vnpayRoutes(VNPayHandler handler) {
        return route()
                .POST("/api/payment/create-payment", request -> handler.createPayment(request))
                .GET("/api/payment/ipn", request -> handler.handleIpn(request))
                .GET("/api/payment/return", request -> handler.handleReturn(request))
                .build();
    }
}