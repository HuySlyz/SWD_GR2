package com.warranty.dao;

import com.warranty.model.Codes.OrderStatus;
import com.warranty.model.Codes.PaymentMethod;
import com.warranty.model.Codes.PaymentStatus;
import com.warranty.model.Codes.WarrantyStatus;
import com.warranty.model.Entities.Order;
import com.warranty.model.Entities.OrderItem;
import com.warranty.model.Entities.Payment;
import com.warranty.model.Entities.Warranty;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** orders, order_items, payments, warranties. */
public final class OrderDAO {

    private static final String ORDER_SELECT = "SELECT o.*, cu.full_name AS customer_name, cu.phone AS customer_phone,"
            + " su.full_name AS support_name FROM orders o JOIN users cu ON cu.id = o.customer_id"
            + " LEFT JOIN users su ON su.id = o.support_id";

    static final String WARRANTY_COLUMNS = "w.id AS w_id, w.order_id AS w_order_id, o.order_code, w.product_id AS w_product_id,"
            + " p.name AS w_product_name, p.sku AS w_sku, w.serial_number, w.start_date, w.end_date, w.status AS w_status,"
            + " o.customer_id AS w_customer_id, cu.full_name AS w_customer_name, cu.phone AS w_customer_phone";
    static final String WARRANTY_JOINS = " JOIN orders o ON o.id = w.order_id JOIN products p ON p.id = w.product_id"
            + " JOIN users cu ON cu.id = o.customer_id";
    private static final String WARRANTY_SELECT = "SELECT " + WARRANTY_COLUMNS + " FROM warranties w" + WARRANTY_JOINS;

    private OrderDAO() {
    }

    // ============================ ORDER ============================
    public static int insertOrder(Connection c, String code, int customerId, int supportId, BigDecimal total)
            throws SQLException {
        return Sql.insert(c, "INSERT INTO orders(order_code, customer_id, support_id, status, total_amount)"
                + " VALUES (?, ?, ?, ?, ?)", code, customerId, supportId, OrderStatus.PENDING, total);
    }

    public static void insertItem(Connection c, int orderId, int productId, int qty, BigDecimal unitPrice)
            throws SQLException {
        Sql.exec(c, "INSERT INTO order_items(order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)",
                orderId, productId, qty, unitPrice);
    }

    public static void setStatus(Connection c, int orderId, OrderStatus status) throws SQLException {
        Sql.exec(c, "UPDATE orders SET status = ? WHERE id = ?", status, orderId);
    }

    public static Order find(Connection c, int id) throws SQLException {
        return Sql.one(c, ORDER_SELECT + " WHERE o.id = ?", OrderDAO::mapOrder, id);
    }

    public static Order findForUpdate(Connection c, int id) throws SQLException {
        return Sql.one(c, ORDER_SELECT.replace("FROM orders o", "FROM orders o WITH (UPDLOCK)") + " WHERE o.id = ?",
                OrderDAO::mapOrder, id);
    }

    public static List<Order> search(Connection c, OrderStatus status, String keyword, Integer customerId)
            throws SQLException {
        String kw = Sql.like(keyword);
        return Sql.list(c, ORDER_SELECT + " WHERE (? IS NULL OR o.status = ?) AND (? IS NULL OR o.customer_id = ?)"
                + " AND (o.order_code LIKE ? OR cu.full_name LIKE ? OR cu.phone LIKE ?) ORDER BY o.order_date DESC",
                OrderDAO::mapOrder, status, status, customerId, customerId, kw, kw, kw);
    }

    public static List<OrderItem> items(Connection c, int orderId) throws SQLException {
        return Sql.list(c, "SELECT i.*, p.sku, p.name FROM order_items i JOIN products p ON p.id = i.product_id"
                + " WHERE i.order_id = ? ORDER BY i.id", rs -> new OrderItem(rs.getInt("id"), rs.getInt("order_id"),
                rs.getInt("product_id"), rs.getString("sku"), rs.getString("name"), rs.getInt("quantity"),
                rs.getBigDecimal("unit_price")), orderId);
    }

    // ============================ PAYMENT ============================
    public static void insertPayment(Connection c, Integer orderId, Integer ticketId, BigDecimal amount,
            PaymentMethod method, PaymentStatus status, String transactionRef) throws SQLException {
        Sql.exec(c, "INSERT INTO payments(order_id, ticket_id, amount, method, status, transaction_ref)"
                + " VALUES (?, ?, ?, ?, ?, ?)", orderId, ticketId, amount, method, status, transactionRef);
    }

    public static List<Payment> paymentsOfOrder(Connection c, int orderId) throws SQLException {
        return Sql.list(c, "SELECT * FROM payments WHERE order_id = ? ORDER BY paid_at", OrderDAO::mapPayment, orderId);
    }

    public static List<Payment> paymentsOfTicket(Connection c, int ticketId) throws SQLException {
        return Sql.list(c, "SELECT * FROM payments WHERE ticket_id = ? ORDER BY paid_at", OrderDAO::mapPayment,
                ticketId);
    }

    // ============================ WARRANTY ============================
    public static void insertWarranty(Connection c, int orderId, int productId, String serial, LocalDate start,
            LocalDate end) throws SQLException {
        Sql.exec(c, "INSERT INTO warranties(order_id, product_id, serial_number, start_date, end_date, status)"
                + " VALUES (?, ?, ?, ?, ?, ?)", orderId, productId, serial, start, end, WarrantyStatus.ACTIVE);
    }

    public static Warranty warrantyBySerial(Connection c, String serial) throws SQLException {
        return Sql.one(c, WARRANTY_SELECT + " WHERE w.serial_number = ?", OrderDAO::mapWarranty, serial);
    }

    public static List<Warranty> warrantiesOfOrder(Connection c, int orderId) throws SQLException {
        return Sql.list(c, WARRANTY_SELECT + " WHERE w.order_id = ? ORDER BY w.id", OrderDAO::mapWarranty, orderId);
    }

    public static List<Warranty> warrantiesOfCustomer(Connection c, int customerId) throws SQLException {
        return Sql.list(c, WARRANTY_SELECT + " WHERE o.customer_id = ? ORDER BY w.start_date DESC",
                OrderDAO::mapWarranty, customerId);
    }

    /** Cap nhat cac bao hanh ACTIVE da qua end_date thanh EXPIRED. */
    public static void expireWarranties(Connection c) throws SQLException {
        Sql.exec(c, "UPDATE warranties SET status = ? WHERE status = ? AND end_date < CAST(SYSDATETIME() AS DATE)",
                WarrantyStatus.EXPIRED, WarrantyStatus.ACTIVE);
    }

    // ============================ MAPPER ============================
    private static Order mapOrder(ResultSet rs) throws SQLException {
        return new Order(rs.getInt("id"), rs.getString("order_code"), rs.getInt("customer_id"),
                rs.getString("customer_name"), rs.getString("customer_phone"), Sql.intOrNull(rs, "support_id"),
                rs.getString("support_name"), Sql.dateTime(rs, "order_date"),
                OrderStatus.valueOf(rs.getString("status")), rs.getBigDecimal("total_amount"));
    }

    private static Payment mapPayment(ResultSet rs) throws SQLException {
        return new Payment(rs.getInt("id"), Sql.intOrNull(rs, "order_id"), Sql.intOrNull(rs, "ticket_id"),
                rs.getBigDecimal("amount"), PaymentMethod.valueOf(rs.getString("method")),
                PaymentStatus.valueOf(rs.getString("status")), Sql.dateTime(rs, "paid_at"),
                rs.getString("transaction_ref"));
    }

    static Warranty mapWarranty(ResultSet rs) throws SQLException {
        return new Warranty(rs.getInt("w_id"), rs.getInt("w_order_id"), rs.getString("order_code"),
                rs.getInt("w_product_id"), rs.getString("w_product_name"), rs.getString("w_sku"),
                rs.getString("serial_number"), Sql.date(rs, "start_date"), Sql.date(rs, "end_date"),
                WarrantyStatus.valueOf(rs.getString("w_status")), rs.getInt("w_customer_id"),
                rs.getString("w_customer_name"), rs.getString("w_customer_phone"));
    }

    /** Ma giao dich gia lap tu cong thanh toan. */
    public static String newTransactionRef() {
        return "TX" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"));
    }
}
