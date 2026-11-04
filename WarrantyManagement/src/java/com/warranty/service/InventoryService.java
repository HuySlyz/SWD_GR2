package com.warranty.service;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.PurchaseOrderDAO;
import com.warranty.dao.Sql;
import com.warranty.dao.TicketDAO;
import com.warranty.model.Codes.PoStatus;
import com.warranty.model.Entities.PoLine;
import com.warranty.model.Entities.PurchaseOrder;
import com.warranty.model.Entities.RepairPart;
import com.warranty.model.Entities.Supplier;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Nhan vien kho:
 *  - Xu ly yeu cau linh kien cua ky thuat (Deliver Components to Tech / Reject Request)
 *  - Flow 4: bao cao ton thap -> lap purchase order gui NCC -> xac nhan nhan hang, cong kho
 *  - Kiem kho (Count Stock / Compare Records / Adjust Quantity)
 */
public class InventoryService {

    /** Dong nhap tu form PO. type = PRODUCT | PART. */
    public record PoInput(String type, int itemId, int quantity, BigDecimal unitCost) {
    }

    public void deliverPart(int repairLogId, int partId, int staffId) throws SQLException, BusinessException {
        Tx.write(c -> {
            RepairPart rp = pending(TicketDAO.repairPart(c, repairLogId, partId));
            TicketDAO.issueRepairPart(c, rp.repairLogId(), rp.partId(), staffId);
            return null;
        });
    }

    /** Tu choi yeu cau: tra lai so luong da giu vao kho. */
    public void rejectPart(int repairLogId, int partId) throws SQLException, BusinessException {
        Tx.write(c -> {
            RepairPart rp = pending(TicketDAO.repairPart(c, repairLogId, partId));
            TicketDAO.deleteRepairPart(c, repairLogId, partId);
            CatalogDAO.addPartStock(c, partId, rp.quantity());
            return null;
        });
    }

    private static RepairPart pending(RepairPart rp) throws BusinessException {
        if (rp == null) {
            throw new BusinessException("Yêu cầu linh kiện không tồn tại");
        }
        if (rp.delivered()) {
            throw new BusinessException("Linh kiện đã được giao");
        }
        return rp;
    }

    /** Flow 4 buoc 5-8: tao PO (status = SENT) va gui NCC. */
    public String createPurchaseOrder(int staffId, int supplierId, List<PoInput> lines)
            throws SQLException, BusinessException {
        if (lines.isEmpty()) {
            throw new BusinessException("Phiếu nhập phải có ít nhất 1 mặt hàng");
        }
        return Tx.write(c -> {
            Supplier s = CatalogDAO.supplier(c, supplierId);
            if (s == null) {
                throw new BusinessException("Chọn nhà cung cấp");
            }
            List<String> supplied = CatalogDAO.supplierItemKeys(c, supplierId);
            BigDecimal total = BigDecimal.ZERO;
            for (PoInput l : lines) {
                if (l.quantity() <= 0 || l.unitCost() == null || l.unitCost().signum() < 0) {
                    throw new BusinessException("Số lượng / đơn giá nhập không hợp lệ");
                }
                if (!supplied.contains(l.type() + ":" + l.itemId())) {
                    throw new BusinessException("Nhà cung cấp " + s.name() + " không cung cấp mặt hàng đã chọn");
                }
                total = total.add(l.unitCost().multiply(BigDecimal.valueOf(l.quantity())));
            }
            String code = Sql.nextCode(c, "purchase_orders", "po_code", "PO");
            int poId = PurchaseOrderDAO.insert(c, code, supplierId, staffId, total);
            for (PoInput l : lines) {
                PurchaseOrderDAO.insertLine(c, poId, l.type(), l.itemId(), l.quantity(), l.unitCost());
            }
            return code;
        });
    }

    /** Flow 4 buoc 9-12: NCC giao hang -> cong stock_qty, PO = RECEIVED. */
    public void receivePurchaseOrder(int poId, int staffId) throws SQLException, BusinessException {
        Tx.write(c -> {
            PurchaseOrder po = PurchaseOrderDAO.findForUpdate(c, poId);
            if (po == null) {
                throw new BusinessException("Phiếu nhập không tồn tại");
            }
            if (po.status() != PoStatus.SENT) {
                throw new BusinessException("Phiếu nhập đã được nhận hàng");
            }
            for (PoLine l : PurchaseOrderDAO.lines(c, poId)) {
                if ("PART".equals(l.type())) {
                    CatalogDAO.addPartStock(c, l.itemId(), l.quantity());
                } else {
                    CatalogDAO.addProductStock(c, l.itemId(), l.quantity());
                }
            }
            PurchaseOrderDAO.markReceived(c, poId, staffId);
            return null;
        });
    }

    /** Kiem kho: dieu chinh so luong thuc dem. */
    public void adjustStock(String type, int id, int countedQty) throws SQLException, BusinessException {
        if (countedQty < 0) {
            throw new BusinessException("Số lượng kiểm kê không được âm");
        }
        Tx.write(c -> {
            if ("PART".equals(type)) {
                CatalogDAO.setPartStock(c, id, countedQty);
            } else {
                CatalogDAO.setProductStock(c, id, countedQty);
            }
            return null;
        });
    }
}
