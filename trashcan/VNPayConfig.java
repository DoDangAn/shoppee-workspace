package com.andd.DoDangAn.DoDangAn.config.VNpay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {
    private String tmnCode;
    private String hashSecret;
    private String paymentUrl;
    private String returnUrl;
    private String ipnUrl;

    // Getters and setters
    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }
    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getIpnUrl() { return ipnUrl; }
    public void setIpnUrl(String ipnUrl) { this.ipnUrl = ipnUrl; }
}