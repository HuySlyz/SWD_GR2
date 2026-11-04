package com.warranty.dao;

import com.warranty.model.Entities.Part;
import com.warranty.model.Entities.Product;
import com.warranty.model.Entities.Seller;
import com.warranty.model.Entities.StockItem;
import com.warranty.model.Entities.Supplier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** sellers, suppliers, products, parts, supplier_products, supplier_parts. */
public final class CatalogDAO {

    private static final String PRODUCT_SELECT = "SELECT p.*, s.name AS seller_name FROM products p"
            + " LEFT JOIN sellers s ON s.id = p.seller_id";

    private CatalogDAO() {
    }

    // ============================ SELLER ============================
    public static List<Seller> sellers(Connection c) throws SQLException {
        return Sql.list(c, "SELECT * FROM sellers ORDER BY name", rs -> new Seller(rs.getInt("id"),
                rs.getString("name"), rs.getString("phone"), rs.getString("email")));
    }

    public static void saveSeller(Connection c, int id, String name, String phone, String email) throws SQLException {
        if (id == 0) {
            Sql.exec(c, "INSERT INTO sellers(name, phone, email) VALUES (?, ?, ?)", name, phone, email);
        } else {
            Sql.exec(c, "UPDATE sellers SET name = ?, phone = ?, email = ? WHERE id = ?", name, phone, email, id);
        }
    }

    public static void deleteSeller(Connection c, int id) throws SQLException {
        Sql.exec(c, "DELETE FROM sellers WHERE id = ?", id);
    }

    // ============================ SUPPLIER ============================
    public static List<Supplier> suppliers(Connection c) throws SQLException {
        return Sql.list(c, "SELECT * FROM suppliers ORDER BY name", rs -> new Supplier(rs.getInt("id"),
                rs.getString("name"), rs.getString("phone"), rs.getString("email"), rs.getString("address")));
    }

    public static Supplier supplier(Connection c, int id) throws SQLException {
        return Sql.one(c, "SELECT * FROM suppliers WHERE id = ?", rs -> new Supplier(rs.getInt("id"),
                rs.getString("name"), rs.getString("phone"), rs.getString("email"), rs.getString("address")), id);
    }

    /** Tra ve id nha cung cap (moi tao hoac da co). */
    public static int saveSupplier(Connection c, int id, String name, String phone, String email, String address)
            throws SQLException {
        if (id == 0) {
            return Sql.insert(c, "INSERT INTO suppliers(name, phone, email, address) VALUES (?, ?, ?, ?)",
                    name, phone, email, address);
        }
        Sql.exec(c, "UPDATE suppliers SET name = ?, phone = ?, email = ?, address = ? WHERE id = ?",
                name, phone, email, address, id);
        return id;
    }

    public static void deleteSupplier(Connection c, int id) throws SQLException {
        Sql.exec(c, "DELETE FROM supplier_products WHERE supplier_id = ?", id);
        Sql.exec(c, "DELETE FROM supplier_parts WHERE supplier_id = ?", id);
        Sql.exec(c, "DELETE FROM suppliers WHERE id = ?", id);
    }

    /** Mat hang ma NCC cung cap: "PRODUCT:1", "PART:3". */
    public static List<String> supplierItemKeys(Connection c, int supplierId) throws SQLException {
        return Sql.list(c, "SELECT 'PRODUCT:' + CAST(product_id AS VARCHAR) FROM supplier_products WHERE supplier_id = ?"
                + " UNION ALL SELECT 'PART:' + CAST(part_id AS VARCHAR) FROM supplier_parts WHERE supplier_id = ?",
                rs -> rs.getString(1), supplierId, supplierId);
    }

    public static void setSupplierItems(Connection c, int supplierId, List<Integer> productIds, List<Integer> partIds)
            throws SQLException {
        Sql.exec(c, "DELETE FROM supplier_products WHERE supplier_id = ?", supplierId);
        Sql.exec(c, "DELETE FROM supplier_parts WHERE supplier_id = ?", supplierId);
        for (int p : productIds) {
            Sql.exec(c, "INSERT INTO supplier_products(supplier_id, product_id) VALUES (?, ?)", supplierId, p);
        }
        for (int p : partIds) {
            Sql.exec(c, "INSERT INTO supplier_parts(supplier_id, part_id) VALUES (?, ?)", supplierId, p);
        }
    }

    // ============================ PRODUCT ============================
    public static List<Product> products(Connection c, String keyword, boolean activeOnly) throws SQLException {
        String kw = Sql.like(keyword);
        return Sql.list(c, PRODUCT_SELECT + " WHERE (p.sku LIKE ? OR p.name LIKE ?)"
                + (activeOnly ? " AND p.is_active = 1" : "") + " ORDER BY p.name", CatalogDAO::mapProduct, kw, kw);
    }

    public static Product product(Connection c, int id) throws SQLException {
        return Sql.one(c, PRODUCT_SELECT + " WHERE p.id = ?", CatalogDAO::mapProduct, id);
    }

    public static Product productForUpdate(Connection c, int id) throws SQLException {
        return Sql.one(c, "SELECT p.*, NULL AS seller_name FROM products p WITH (UPDLOCK) WHERE p.id = ?",
                CatalogDAO::mapProduct, id);
    }

    public static boolean skuExists(Connection c, String sku, int exceptId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM products WHERE sku = ? AND id <> ?", sku, exceptId) > 0;
    }

    public static void saveProduct(Connection c, int id, Integer sellerId, String sku, String name, BigDecimal price,
            int warrantyMonths, int reorderLevel) throws SQLException {
        if (id == 0) {
            Sql.exec(c, "INSERT INTO products(seller_id, sku, name, price, warranty_months, stock_qty, reorder_level)"
                    + " VALUES (?, ?, ?, ?, ?, 0, ?)", sellerId, sku, name, price, warrantyMonths, reorderLevel);
        } else {
            Sql.exec(c, "UPDATE products SET seller_id = ?, sku = ?, name = ?, price = ?, warranty_months = ?,"
                    + " reorder_level = ? WHERE id = ?", sellerId, sku, name, price, warrantyMonths, reorderLevel, id);
        }
    }

    public static void setProductActive(Connection c, int id, boolean active) throws SQLException {
        Sql.exec(c, "UPDATE products SET is_active = ? WHERE id = ?", active, id);
    }

    public static void deleteProduct(Connection c, int id) throws SQLException {
        Sql.exec(c, "DELETE FROM supplier_products WHERE product_id = ?", id);
        Sql.exec(c, "DELETE FROM products WHERE id = ?", id);
    }

    public static void addProductStock(Connection c, int id, int delta) throws SQLException {
        Sql.exec(c, "UPDATE products SET stock_qty = stock_qty + ? WHERE id = ?", delta, id);
    }

    public static void setProductStock(Connection c, int id, int qty) throws SQLException {
        Sql.exec(c, "UPDATE products SET stock_qty = ? WHERE id = ?", qty, id);
    }

    // ============================ PART ============================
    public static List<Part> parts(Connection c, String keyword) throws SQLException {
        String kw = Sql.like(keyword);
        return Sql.list(c, "SELECT * FROM parts WHERE part_code LIKE ? OR name LIKE ? ORDER BY name",
                CatalogDAO::mapPart, kw, kw);
    }

    public static Part part(Connection c, int id) throws SQLException {
        return Sql.one(c, "SELECT * FROM parts WHERE id = ?", CatalogDAO::mapPart, id);
    }

    public static Part partForUpdate(Connection c, int id) throws SQLException {
        return Sql.one(c, "SELECT * FROM parts WITH (UPDLOCK) WHERE id = ?", CatalogDAO::mapPart, id);
    }

    public static boolean partCodeExists(Connection c, String code, int exceptId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM parts WHERE part_code = ? AND id <> ?", code, exceptId) > 0;
    }

    public static void savePart(Connection c, int id, String code, String name, int reorderLevel, BigDecimal unitCost)
            throws SQLException {
        if (id == 0) {
            Sql.exec(c, "INSERT INTO parts(part_code, name, stock_qty, reorder_level, unit_cost) VALUES (?, ?, 0, ?, ?)",
                    code, name, reorderLevel, unitCost);
        } else {
            Sql.exec(c, "UPDATE parts SET part_code = ?, name = ?, reorder_level = ?, unit_cost = ? WHERE id = ?",
                    code, name, reorderLevel, unitCost, id);
        }
    }

    public static void deletePart(Connection c, int id) throws SQLException {
        Sql.exec(c, "DELETE FROM supplier_parts WHERE part_id = ?", id);
        Sql.exec(c, "DELETE FROM parts WHERE id = ?", id);
    }

    public static void addPartStock(Connection c, int id, int delta) throws SQLException {
        Sql.exec(c, "UPDATE parts SET stock_qty = stock_qty + ? WHERE id = ?", delta, id);
    }

    public static void setPartStock(Connection c, int id, int qty) throws SQLException {
        Sql.exec(c, "UPDATE parts SET stock_qty = ? WHERE id = ?", qty, id);
    }

    // ============================ TON KHO ============================
    /** Flow 4 buoc 2: SELECT products/parts (lowOnly = WHERE stock_qty <= reorder_level). */
    public static List<StockItem> stock(Connection c, boolean lowOnly) throws SQLException {
        String where = lowOnly ? " WHERE stock_qty <= reorder_level" : "";
        return Sql.list(c, "SELECT 'PRODUCT' AS type, id, sku AS code, name, stock_qty, reorder_level, price AS cost"
                + " FROM products" + where + (lowOnly ? " AND is_active = 1" : "")
                + " UNION ALL SELECT 'PART', id, part_code, name, stock_qty, reorder_level, unit_cost FROM parts" + where
                + " ORDER BY type DESC, stock_qty", rs -> new StockItem(rs.getString("type"), rs.getInt("id"),
                rs.getString("code"), rs.getString("name"), rs.getInt("stock_qty"), rs.getInt("reorder_level"),
                rs.getBigDecimal("cost")));
    }

    private static Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("id"), Sql.intOrNull(rs, "seller_id"), rs.getString("seller_name"),
                rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("warranty_months"),
                rs.getInt("stock_qty"), rs.getInt("reorder_level"), rs.getBoolean("is_active"));
    }

    private static Part mapPart(ResultSet rs) throws SQLException {
        return new Part(rs.getInt("id"), rs.getString("part_code"), rs.getString("name"), rs.getInt("stock_qty"),
                rs.getInt("reorder_level"), rs.getBigDecimal("unit_cost"));
    }
}
