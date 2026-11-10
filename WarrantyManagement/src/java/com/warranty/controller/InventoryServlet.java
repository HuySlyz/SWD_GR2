package com.warranty.controller;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.PurchaseOrderDAO;
import com.warranty.dao.TicketDAO;
import com.warranty.model.Codes.PoStatus;
import com.warranty.model.Entities.PurchaseOrder;
import com.warranty.model.Entities.StockItem;
import com.warranty.model.Entities.Supplier;
import com.warranty.service.BusinessException;
import com.warranty.service.CatalogService;
import com.warranty.service.InventoryService;
import com.warranty.service.InventoryService.PoInput;
import com.warranty.service.Tx;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case Inventory Staff:
 *  /inventory/stock     bao cao ton kho, hang duoi muc toi thieu, kiem kho (Flow 4 buoc 1-4)
 *  /inventory/parts     them / sua / xoa linh kien (component)
 *  /inventory/requests  yeu cau linh kien tu ky thuat: giao / tu choi
 *  /inventory/po        purchase order: tao, gui NCC, xac nhan nhan hang (Flow 4 buoc 5-12)
 * doGet / doPost chi dieu huong theo duong dan + action; xu ly nam trong cac ham rieng ben duoi.
 */
@WebServlet({"/inventory/stock", "/inventory/parts", "/inventory/requests", "/inventory/po"})
public class InventoryServlet extends BaseServlet {

    private final InventoryService inventoryService = new InventoryService();
    private final CatalogService catalogService = new CatalogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        try {
            switch (req.getServletPath()) {
                case "/inventory/stock":
                    showStock(req, resp);
                    break;
                case "/inventory/parts":
                    showParts(req, resp);
                    break;
                case "/inventory/requests":
                    showPartRequests(req, resp);
                    break;
                default:
                    if ("new".equals(action)) {
                        showCreatePurchaseOrderForm(req, resp);
                    } else if ("view".equals(action)) {
                        showPurchaseOrderDetail(req, resp);
                    } else {
                        showPurchaseOrderList(req, resp);
                    }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        switch (req.getServletPath()) {
            case "/inventory/stock":
                adjustStock(req, resp);
                break;
            case "/inventory/parts":
                if ("delete".equals(action)) {
                    deletePart(req, resp);
                } else {
                    savePart(req, resp);
                }
                break;
            case "/inventory/requests":
                if ("reject".equals(action)) {
                    rejectPartRequest(req, resp);
                } else {
                    deliverPart(req, resp);
                }
                break;
            default:
                if ("receive".equals(action)) {
                    receivePurchaseOrder(req, resp);
                } else {
                    createPurchaseOrder(req, resp);
                }
        }
    }

    // ============================ GET ============================

    /** Bao cao ton kho: mac dinh chi hang duoi muc toi thieu, ?view=all de xem tat ca. */
    private void showStock(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        boolean lowOnly = !"all".equals(param(req, "view"));
        req.setAttribute("items", Tx.read(c -> CatalogDAO.stock(c, lowOnly)));
        req.setAttribute("lowOnly", lowOnly);
        render(req, resp, "inventory/stock");
    }

    /** Danh sach linh kien (tim kiem) + linh kien dang sua neu co ?id=. */
    private void showParts(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        int editId = intParam(req, "id");
        Tx.read(c -> {
            req.setAttribute("parts", CatalogDAO.parts(c, param(req, "q")));
            req.setAttribute("editing", editId > 0 ? CatalogDAO.part(c, editId) : null);
            return null;
        });
        req.setAttribute("keyword", param(req, "q"));
        render(req, resp, "inventory/parts");
    }

    /** Yeu cau linh kien tu ky thuat vien (chua giao len dau). */
    private void showPartRequests(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        req.setAttribute("requests", Tx.read(c -> TicketDAO.partRequests(c, false)));
        render(req, resp, "inventory/requests");
    }

    /** Form lap PO: chon NCC, chi hien mat hang NCC do cung cap. */
    private void showCreatePurchaseOrderForm(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        int supplierId = intParam(req, "supplierId");
        Tx.read(c -> {
            req.setAttribute("suppliers", CatalogDAO.suppliers(c));
            if (supplierId > 0) {
                Supplier s = CatalogDAO.supplier(c, supplierId);
                List<String> keys = CatalogDAO.supplierItemKeys(c, supplierId);
                List<StockItem> items = new ArrayList<>();
                for (StockItem i : CatalogDAO.stock(c, false)) {
                    if (keys.contains(i.type() + ":" + i.id())) {
                        items.add(i);
                    }
                }
                req.setAttribute("supplier", s);
                req.setAttribute("items", items);
            }
            return null;
        });
        render(req, resp, "inventory/po-new");
    }

    /** Chi tiet PO kem cac dong hang. */
    private void showPurchaseOrderDetail(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        int id = intParam(req, "id");
        PurchaseOrder po = Tx.read(c -> PurchaseOrderDAO.find(c, id));
        if (po == null) {
            flashError(req, "Phiếu nhập không tồn tại");
            redirect(req, resp, "/inventory/po");
            return;
        }
        req.setAttribute("po", po);
        req.setAttribute("lines", Tx.read(c -> PurchaseOrderDAO.lines(c, id)));
        render(req, resp, "inventory/po-detail");
    }

    /** Danh sach PO, loc theo trang thai. */
    private void showPurchaseOrderList(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        PoStatus status = enumParam(req, "status", PoStatus.class);
        req.setAttribute("orders", Tx.read(c -> PurchaseOrderDAO.list(c, status)));
        req.setAttribute("status", status);
        render(req, resp, "inventory/po");
    }

    // ============================ POST ============================

    /** Kiem kho: ghi de ton kho bang so dem thuc te. */
    private void adjustStock(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String view = param(req, "view");
        perform(req, resp, "/inventory/stock?view=" + (view == null ? "" : view), () -> {
            inventoryService.adjustStock(param(req, "type"), intParam(req, "id"), intParam(req, "countedQty"));
            return "Đã điều chỉnh tồn kho theo số kiểm kê";
        });
    }

    /** Them moi (id = 0) hoac cap nhat linh kien. */
    private void savePart(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/inventory/parts", () -> {
            catalogService.savePart(intParam(req, "id"), param(req, "partCode"), param(req, "name"),
                    intParam(req, "reorderLevel"), moneyParam(req, "unitCost"));
            return "Đã lưu linh kiện " + param(req, "partCode");
        });
    }

    private void deletePart(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/inventory/parts", () -> {
            catalogService.deletePart(intParam(req, "id"));
            return "Đã xóa linh kiện";
        });
    }

    /** Giao linh kien cho ky thuat vien (ghi nguoi giao + thoi diem). */
    private void deliverPart(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int staffId = currentUser(req).id();
        perform(req, resp, "/inventory/requests", () -> {
            inventoryService.deliverPart(intParam(req, "repairLogId"), intParam(req, "partId"), staffId);
            return "Đã giao linh kiện cho kỹ thuật viên";
        });
    }

    /** Tu choi yeu cau: xoa yeu cau va cong tra ton kho da giu. */
    private void rejectPartRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        perform(req, resp, "/inventory/requests", () -> {
            inventoryService.rejectPart(intParam(req, "repairLogId"), intParam(req, "partId"));
            return "Đã từ chối yêu cầu và hoàn lại tồn kho";
        });
    }

    /** Flow 4 buoc 5-8: lap PO (SENT) gui nha cung cap. */
    private void createPurchaseOrder(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int staffId = currentUser(req).id();
        int supplierId = intParam(req, "supplierId");
        perform(req, resp, "/inventory/po?action=new&supplierId=" + supplierId, () -> {
            String code = inventoryService.createPurchaseOrder(staffId, supplierId, readPoLines(req));
            return "Đã tạo phiếu nhập " + code + " và gửi nhà cung cấp";
        });
    }

    /** Flow 4 buoc 9-12: nhan hang, cong ton kho, PO -> RECEIVED. */
    private void receivePurchaseOrder(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int staffId = currentUser(req).id();
        int id = intParam(req, "id");
        perform(req, resp, "/inventory/po?action=view&id=" + id, () -> {
            inventoryService.receivePurchaseOrder(id, staffId);
            return "Đã nhận hàng và cập nhật tồn kho";
        });
    }

    /** Doc cac dong item = "PART:3" kem qty_PART:3 / cost_PART:3; bo qua dong so luong 0. */
    private List<PoInput> readPoLines(HttpServletRequest req) throws BusinessException {
        List<PoInput> out = new ArrayList<>();
        String[] keys = req.getParameterValues("item");
        if (keys == null) {
            return out;
        }
        for (String key : keys) {
            int qty = intParam(req, "qty_" + key);
            if (qty == 0) {
                continue;
            }
            BigDecimal cost = moneyParam(req, "cost_" + key);
            String[] p = key.split(":");
            if (p.length != 2 || cost == null) {
                throw new BusinessException("Dữ liệu phiếu nhập không hợp lệ");
            }
            out.add(new PoInput(p[0], Integer.parseInt(p[1]), qty, cost));
        }
        return out;
    }
}
