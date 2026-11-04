package com.warranty.dao;

import com.warranty.model.Codes.PoStatus;
import com.warranty.model.Entities.PoLine;
import com.warranty.model.Entities.PurchaseOrder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** purchase_orders, purchase_order_products, purchase_order_parts. */
public final class PurchaseOrderDAO {

    private static final String SELECT = "SELECT po.*, s.name AS supplier_name, st.full_name AS staff_name,"
            + " rb.full_name AS received_name FROM purchase_orders po JOIN suppliers s ON s.id = po.supplier_id"
            + " JOIN users st ON st.id = po.inventory_staff_id LEFT JOIN users rb ON rb.id = po.received_by";

    private PurchaseOrderDAO() {
    }

    public static int insert(Connection c, String code, int supplierId, int staffId, BigDecimal total)
            throws SQLException {
        return Sql.insert(c, "INSERT INTO purchase_orders(po_code, supplier_id, inventory_staff_id, status, total_amount)"
                + " VALUES (?, ?, ?, ?, ?)", code, supplierId, staffId, PoStatus.SENT, total);
    }

    public static void insertLine(Connection c, int poId, String type, int itemId, int qty, BigDecimal unitCost)
            throws SQLException {
        String table = "PART".equals(type) ? "purchase_order_parts(purchase_order_id, part_id"
                : "purchase_order_products(purchase_order_id, product_id";
        Sql.exec(c, "INSERT INTO " + table + ", quantity, unit_cost) VALUES (?, ?, ?, ?)", poId, itemId, qty, unitCost);
    }

    public static List<PurchaseOrder> list(Connection c, PoStatus status) throws SQLException {
        return Sql.list(c, SELECT + " WHERE (? IS NULL OR po.status = ?) ORDER BY po.order_date DESC",
                PurchaseOrderDAO::map, status, status);
    }

    public static PurchaseOrder find(Connection c, int id) throws SQLException {
        return Sql.one(c, SELECT + " WHERE po.id = ?", PurchaseOrderDAO::map, id);
    }

    public static PurchaseOrder findForUpdate(Connection c, int id) throws SQLException {
        return Sql.one(c, SELECT.replace("FROM purchase_orders po", "FROM purchase_orders po WITH (UPDLOCK)")
                + " WHERE po.id = ?", PurchaseOrderDAO::map, id);
    }

    public static List<PoLine> lines(Connection c, int poId) throws SQLException {
        return Sql.list(c, "SELECT 'PRODUCT' AS type, p.id, p.sku AS code, p.name, l.quantity, l.unit_cost"
                + " FROM purchase_order_products l JOIN products p ON p.id = l.product_id WHERE l.purchase_order_id = ?"
                + " UNION ALL SELECT 'PART', p.id, p.part_code, p.name, l.quantity, l.unit_cost"
                + " FROM purchase_order_parts l JOIN parts p ON p.id = l.part_id WHERE l.purchase_order_id = ?",
                rs -> new PoLine(rs.getString("type"), rs.getInt("id"), rs.getString("code"), rs.getString("name"),
                        rs.getInt("quantity"), rs.getBigDecimal("unit_cost")), poId, poId);
    }

    public static void markReceived(Connection c, int poId, int staffId) throws SQLException {
        Sql.exec(c, "UPDATE purchase_orders SET status = ?, received_by = ?, received_at = ? WHERE id = ?",
                PoStatus.RECEIVED, staffId, LocalDateTime.now(), poId);
    }

    private static PurchaseOrder map(ResultSet rs) throws SQLException {
        return new PurchaseOrder(rs.getInt("id"), rs.getString("po_code"), rs.getInt("supplier_id"),
                rs.getString("supplier_name"), rs.getString("staff_name"), Sql.dateTime(rs, "order_date"),
                PoStatus.valueOf(rs.getString("status")), rs.getBigDecimal("total_amount"),
                rs.getString("received_name"), Sql.dateTime(rs, "received_at"));
    }
}
