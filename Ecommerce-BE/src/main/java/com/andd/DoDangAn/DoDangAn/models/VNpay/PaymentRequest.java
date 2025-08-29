package com.andd.DoDangAn.DoDangAn.models.VNpay;

public class PaymentRequest {
        private long amount;
        private String orderInfo;
        private String orderType;
        private String bankCode;
        private String language;

        // Getters and setters
        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }
        public String getOrderInfo() { return orderInfo; }
        public void setOrderInfo(String orderInfo) { this.orderInfo = orderInfo; }
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        public String getBankCode() { return bankCode; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
}