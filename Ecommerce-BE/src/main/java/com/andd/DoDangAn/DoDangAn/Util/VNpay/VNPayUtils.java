package com.andd.DoDangAn.DoDangAn.Util.VNpay;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Component
public class VNPayUtils {

    public String hmacSHA512(String key, String data) throws Exception {
        Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512_HMAC.init(secret_key);
        byte[] hash = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String getIpAddress(ServerRequest request) {
        return request.headers().firstHeader("X-FORWARDED-FOR") != null
                ? request.headers().firstHeader("X-FORWARDED-FOR")
                : request.remoteAddress().map(addr -> addr.getAddress().getHostAddress()).orElse("127.0.0.1");
    }
}