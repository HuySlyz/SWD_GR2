package com.warranty.model;

import com.warranty.model.Codes.Channel;
import com.warranty.model.Codes.OrderStatus;
import com.warranty.model.Codes.PaymentMethod;
import com.warranty.model.Codes.PaymentStatus;
import com.warranty.model.Codes.PoStatus;
import com.warranty.model.Codes.WarrantyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cac doi tuong du lieu (Model) anh xa tu bang trong so do physicalv2.
 * Dung record (Java 17): bat bien, co san accessor, equals/hashCode.
 */
public final class Entities {

    private Entities() {
    }

    /** users + customers.address (null voi nhan vien). */
    public record User(int id, Role role, String username, String fullName, String email, String phone,
            boolean active, LocalDateTime createdAt, String address) {
    }

    public record Seller(int id, String name, String phone, String email) {
    }

    public record Supplier(int id, String name, String phone, String email, String address) {
    }

    public record Product(int id, Integer sellerId, String sellerName, String sku, String name, BigDecimal price,
            int warrantyMonths, int stockQty, int reorderLevel, boolean active) {
        public boolean lowStock() { return stockQty <= reorderLevel; }
    }

    public record Part(int id, String partCode, String name, int stockQty, int reorderLevel, BigDecimal unitCost) {
        public boolean lowStock() { return stockQty <= reorderLevel; }
    }

    public record Order(int id, String orderCode, int customerId, String customerName, String customerPhone,
            Integer supportId, String supportName, LocalDateTime orderDate, OrderStatus status,
            BigDecimal totalAmount) {
    }

    public record OrderItem(int id, int orderId, int productId, String sku, String productName, int quantity,
            BigDecimal unitPrice) {
        public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }

    public record Payment(int id, Integer orderId, Integer ticketId, BigDecimal amount, PaymentMethod method,
            PaymentStatus status, LocalDateTime paidAt, String transactionRef) {
    }

    public record Warranty(int id, int orderId, String orderCode, int productId, String productName, String sku,
            String serialNumber, LocalDate startDate, LocalDate endDate, WarrantyStatus status,
            int customerId, String customerName, String customerPhone) {

        /** Con hieu luc tai ngay {@code date}: chua qua end_date va khong bi VOID. */
        public boolean isValidOn(LocalDate date) {
            return status != WarrantyStatus.VOID && !date.isAfter(endDate);
        }

        /** Trang thai thuc te tai ngay xem (ACTIVE het han thi hien EXPIRED). */
        public WarrantyStatus effectiveStatus(LocalDate date) {
            return status == WarrantyStatus.ACTIVE && date.isAfter(endDate) ? WarrantyStatus.EXPIRED : status;
        }
    }

    public record Ticket(int id, String ticketCode, Warranty warranty, Integer createdBySupportId,
            String createdBySupportName, Integer supportId, String supportName, Integer technicianId,
            String technicianName, Channel channel, String issueDescription, TicketStatus status,
            BigDecimal repairFee, LocalDateTime createdAt, LocalDateTime closedAt) {

        /** Bao hanh con hieu luc tai thoi diem tao ticket -> mien phi; nguoc lai sua co phi. */
        public boolean inWarranty() {
            return warranty.isValidOn(createdAt.toLocalDate());
        }
    }

    public record RepairLog(int id, int ticketId, int technicianId, String technicianName, String diagnosis,
            String repairResult, String shiftNote, LocalDateTime createdAt) {
    }

    /** repair_parts + thong tin hien thi. issuedBy null = cho kho giao. */
    public record RepairPart(int repairLogId, int partId, String partCode, String partName, int quantity,
            Integer issuedBy, String issuedByName, LocalDateTime issuedAt, int ticketId, String ticketCode,
            String technicianName, LocalDateTime requestedAt) {
        public boolean delivered() { return issuedBy != null; }
    }

    public record Handover(int id, int ticketId, String fromName, String toName, String note,
            LocalDateTime handedOverAt) {
    }

    public record PurchaseOrder(int id, String poCode, int supplierId, String supplierName, String staffName,
            LocalDateTime orderDate, PoStatus status, BigDecimal totalAmount, String receivedByName,
            LocalDateTime receivedAt) {
    }

    /** Dong PO: type = PRODUCT (purchase_order_products) hoac PART (purchase_order_parts). */
    public record PoLine(String type, int itemId, String code, String name, int quantity, BigDecimal unitCost) {
        public BigDecimal lineTotal() { return unitCost.multiply(BigDecimal.valueOf(quantity)); }
    }

    public record Notification(int id, int customerId, Integer ticketId, String title, String content,
            boolean read, LocalDateTime sentAt) {
    }

    /** Dong bao cao ton kho (san pham hoac linh kien). */
    public record StockItem(String type, int id, String code, String name, int stockQty, int reorderLevel,
            BigDecimal unitCost) {
        public boolean lowStock() { return stockQty <= reorderLevel; }
    }
}
