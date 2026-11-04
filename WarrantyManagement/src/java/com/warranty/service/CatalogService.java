package com.warranty.service;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.DBContext;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Danh muc: san pham + chinh sach bao hanh (Admin), nguoi ban, nha cung cap (Admin), linh kien (Inventory).
 * Chinh sach bao hanh trong physicalv2 la cot products.warranty_months.
 */
public class CatalogService {

    // ============================ PRODUCT ============================
    public void saveProduct(int id, Integer sellerId, String sku, String name, BigDecimal price, int warrantyMonths,
            int reorderLevel) throws SQLException, BusinessException {
        String s = Tx.require(sku, "SKU là bắt buộc");
        String n = Tx.require(name, "Tên sản phẩm là bắt buộc");
        if (price == null || price.signum() < 0) {
            throw new BusinessException("Giá bán không hợp lệ");
        }
        if (warrantyMonths < 0 || warrantyMonths > 120) {
            throw new BusinessException("Thời hạn bảo hành phải từ 0 đến 120 tháng");
        }
        if (reorderLevel < 0) {
            throw new BusinessException("Mức tồn tối thiểu không hợp lệ");
        }
        Tx.write(c -> {
            if (CatalogDAO.skuExists(c, s, id)) {
                throw new BusinessException("SKU " + s + " đã tồn tại");
            }
            CatalogDAO.saveProduct(c, id, sellerId, s, n, price, warrantyMonths, reorderLevel);
            return null;
        });
    }

    /** Use case "Deactivate Product": ngung ban nhung giu lich su don hang / bao hanh. */
    public void setProductActive(int id, boolean active) throws SQLException, BusinessException {
        Tx.write(c -> {
            CatalogDAO.setProductActive(c, id, active);
            return null;
        });
    }

    public void deleteProduct(int id) throws SQLException, BusinessException {
        guardDelete(() -> Tx.write(c -> {
            CatalogDAO.deleteProduct(c, id);
            return null;
        }), "Sản phẩm đã có đơn hàng / bảo hành / phiếu nhập. Hãy ngừng kinh doanh thay vì xóa.");
    }

    // ============================ SELLER / SUPPLIER ============================
    public void saveSeller(int id, String name, String phone, String email) throws SQLException, BusinessException {
        String n = Tx.require(name, "Tên người bán là bắt buộc");
        if (!Tx.blank(email)) {
            Tx.checkEmail(email.trim());
        }
        Tx.write(c -> {
            CatalogDAO.saveSeller(c, id, n, Tx.trimOrNull(phone), Tx.trimOrNull(email));
            return null;
        });
    }

    public void deleteSeller(int id) throws SQLException, BusinessException {
        guardDelete(() -> Tx.write(c -> {
            CatalogDAO.deleteSeller(c, id);
            return null;
        }), "Người bán đang có sản phẩm, không thể xóa");
    }

    public void saveSupplier(int id, String name, String phone, String email, String address,
            List<Integer> productIds, List<Integer> partIds) throws SQLException, BusinessException {
        String n = Tx.require(name, "Tên nhà cung cấp là bắt buộc");
        if (!Tx.blank(email)) {
            Tx.checkEmail(email.trim());
        }
        Tx.write(c -> {
            int supplierId = CatalogDAO.saveSupplier(c, id, n, Tx.trimOrNull(phone), Tx.trimOrNull(email),
                    Tx.trimOrNull(address));
            CatalogDAO.setSupplierItems(c, supplierId, productIds, partIds);
            return null;
        });
    }

    public void deleteSupplier(int id) throws SQLException, BusinessException {
        guardDelete(() -> Tx.write(c -> {
            CatalogDAO.deleteSupplier(c, id);
            return null;
        }), "Nhà cung cấp đã có đơn nhập hàng, không thể xóa");
    }

    // ============================ PART (Inventory) ============================
    public void savePart(int id, String code, String name, int reorderLevel, BigDecimal unitCost)
            throws SQLException, BusinessException {
        String pc = Tx.require(code, "Mã linh kiện là bắt buộc");
        String n = Tx.require(name, "Tên linh kiện là bắt buộc");
        if (reorderLevel < 0 || unitCost == null || unitCost.signum() < 0) {
            throw new BusinessException("Mức tồn tối thiểu / đơn giá không hợp lệ");
        }
        Tx.write(c -> {
            if (CatalogDAO.partCodeExists(c, pc, id)) {
                throw new BusinessException("Mã linh kiện " + pc + " đã tồn tại");
            }
            CatalogDAO.savePart(c, id, pc, n, reorderLevel, unitCost);
            return null;
        });
    }

    public void deletePart(int id) throws SQLException, BusinessException {
        guardDelete(() -> Tx.write(c -> {
            CatalogDAO.deletePart(c, id);
            return null;
        }), "Linh kiện đã được dùng trong sửa chữa / phiếu nhập, không thể xóa");
    }

    @FunctionalInterface
    private interface Action {
        void run() throws SQLException, BusinessException;
    }

    /** Doi loi khoa ngoai thanh thong bao nghiep vu. */
    private static void guardDelete(Action action, String message) throws SQLException, BusinessException {
        try {
            action.run();
        } catch (SQLException e) {
            if (e.getErrorCode() == DBContext.FK_VIOLATION) {
                throw new BusinessException(message);
            }
            throw e;
        }
    }
}
