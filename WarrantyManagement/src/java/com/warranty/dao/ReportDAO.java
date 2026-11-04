package com.warranty.dao;

import com.warranty.model.Codes.OrderStatus;
import com.warranty.model.Codes.PaymentStatus;
import com.warranty.model.Codes.PoStatus;
import com.warranty.model.TicketStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Bao cao & thong ke cho Admin (context diagram: "Reports and statistics"). */
public final class ReportDAO {

    public record TopProduct(String sku, String name, int quantity, BigDecimal revenue) {
    }

    public record TechLoad(String technician, int open, int completed) {
    }

    public record Summary(int customers, int activeWarranties, int lowStockItems, int pendingPartRequests,
            int openPurchaseOrders, BigDecimal salesRevenue, BigDecimal repairRevenue,
            Map<TicketStatus, Integer> ticketsByStatus) {
    }

    private ReportDAO() {
    }

    public static Summary summary(Connection c) throws SQLException {
        Map<TicketStatus, Integer> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus s : TicketStatus.values()) {
            byStatus.put(s, 0);
        }
        Sql.list(c, "SELECT status, COUNT(*) FROM tickets GROUP BY status", rs -> {
            byStatus.put(TicketStatus.valueOf(rs.getString(1)), rs.getInt(2));
            return null;
        });
        return new Summary(
                Sql.count(c, "SELECT COUNT(*) FROM customers"),
                Sql.count(c, "SELECT COUNT(*) FROM warranties WHERE status = 'ACTIVE'"
                        + " AND end_date >= CAST(SYSDATETIME() AS DATE)"),
                Sql.count(c, "SELECT (SELECT COUNT(*) FROM products WHERE is_active = 1 AND stock_qty <= reorder_level)"
                        + " + (SELECT COUNT(*) FROM parts WHERE stock_qty <= reorder_level)"),
                Sql.count(c, "SELECT COUNT(*) FROM repair_parts WHERE issued_by IS NULL"),
                Sql.count(c, "SELECT COUNT(*) FROM purchase_orders WHERE status = ?", PoStatus.SENT),
                Sql.sum(c, "SELECT SUM(total_amount) FROM orders WHERE status = ?", OrderStatus.PAID),
                Sql.sum(c, "SELECT SUM(amount) FROM payments WHERE ticket_id IS NOT NULL AND status = ?",
                        PaymentStatus.PAID),
                byStatus);
    }

    public static List<TopProduct> topProducts(Connection c) throws SQLException {
        return Sql.list(c, "SELECT TOP 5 p.sku, p.name, SUM(i.quantity) AS qty, SUM(i.quantity * i.unit_price) AS revenue"
                + " FROM order_items i JOIN orders o ON o.id = i.order_id JOIN products p ON p.id = i.product_id"
                + " WHERE o.status = ? GROUP BY p.sku, p.name ORDER BY qty DESC, revenue DESC",
                rs -> new TopProduct(rs.getString("sku"), rs.getString("name"), rs.getInt("qty"),
                        rs.getBigDecimal("revenue")), OrderStatus.PAID);
    }

    public static List<TechLoad> technicianLoad(Connection c) throws SQLException {
        return Sql.list(c, "SELECT u.full_name,"
                + " SUM(CASE WHEN t.status IN ('ASSIGNED','IN_REPAIR') THEN 1 ELSE 0 END) AS open_cnt,"
                + " SUM(CASE WHEN t.status IN ('COMPLETED','CLOSED') THEN 1 ELSE 0 END) AS done_cnt"
                + " FROM technicians te JOIN users u ON u.id = te.user_id"
                + " LEFT JOIN tickets t ON t.technician_id = te.user_id GROUP BY u.full_name ORDER BY u.full_name",
                rs -> new TechLoad(rs.getString(1), rs.getInt(2), rs.getInt(3)));
    }
}
