package com.warranty.model;

/** Cac bo gia tri dang chuoi luu trong CSDL. */
public final class Codes {

    private Codes() {
    }

    /** orders.status */
    public enum OrderStatus {
        PENDING("Chờ thanh toán"), PAID("Đã thanh toán"), CANCELLED("Đã hủy");
        private final String label;
        OrderStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** payments.status */
    public enum PaymentStatus {
        PAID("Thành công"), FAILED("Thất bại");
        private final String label;
        PaymentStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** payments.method */
    public enum PaymentMethod {
        CASH("Tiền mặt"), CARD("Thẻ"), BANK_TRANSFER("Chuyển khoản"), E_WALLET("Ví điện tử");
        private final String label;
        PaymentMethod(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** warranties.status */
    public enum WarrantyStatus {
        ACTIVE("Còn bảo hành"), EXPIRED("Hết hạn"), VOID("Mất hiệu lực");
        private final String label;
        WarrantyStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** tickets.channel */
    public enum Channel {
        WEB("Website"), PHONE("Điện thoại"), STORE("Tại cửa hàng");
        private final String label;
        Channel(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** purchase_orders.status */
    public enum PoStatus {
        SENT("Đã gửi NCC"), RECEIVED("Đã nhận hàng");
        private final String label;
        PoStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
}
