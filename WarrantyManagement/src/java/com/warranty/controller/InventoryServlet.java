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
 */
@WebServlet({"/inventory/stock", "/inventory/parts", "/inventory/requests", "/inventory/po"})
public class InventoryServlet extends BaseServlet {

    private final InventoryService inventoryService = new InventoryService();
    private final CatalogService catalogService = new CatalogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/inventory/stock": {
                    boolean lowOnly = !"all".equals(param(req, "view"));
                    req.setAttribute("items", Tx.read(c -> CatalogDAO.stock(c, lowOnly)));
                    req.setAttribute("lowOnly", lowOnly);
                    render(req, resp, "inventory/stock");
                    break;
                }
                case "/inventory/parts": {
                    int editId = intParam(req, "id");
                    Tx.read(c -> {
                        req.setAttribute("parts", CatalogDAO.parts(c, param(req, "q")));
                        req.setAttribute("editing", editId > 0 ? CatalogDAO.part(c, editId) : null);
                        return null;
                    });
                    req.setAttribute("keyword", param(req, "q"));
                    render(req, resp, "inventory/parts");
                    break;
                }
                case "/inventory/requests":
                    req.setAttribute("requests", Tx.read(c -> TicketDAO.partRequests(c, false)));
                    render(req, resp, "inventory/requests");
                    break;
                default:
                    showPurchaseOrders(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showPurchaseOrders(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        String action = param(req, "action");
        if ("new".equals(action)) {
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
        } else if ("view".equals(action)) {
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
        } else {
            PoStatus status = enumParam(req, "status", PoStatus.class);
            req.setAttribute("orders", Tx.read(c -> PurchaseOrderDAO.list(c, status)));
            req.setAttribute("status", status);
            render(req, resp, "inventory/po");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        String action = param(req, "action");
        int staffId = currentUser(req).id();
        int id = intParam(req, "id");
        switch (path) {
            case "/inventory/stock":
                perform(req, resp, "/inventory/stock?view=" + (param(req, "view") == null ? "" : param(req, "view")),
                        () -> {
                            inventoryService.adjustStock(param(req, "type"), id, intParam(req, "countedQty"));
                            return "Đã điều chỉnh tồn kho theo số kiểm kê";
                        });
                break;
            case "/inventory/parts":
                perform(req, resp, "/inventory/parts", () -> {
                    if ("delete".equals(action)) {
                        catalogService.deletePart(id);
                        return "Đã xóa linh kiện";
                    }
                    catalogService.savePart(id, param(req, "partCode"), param(req, "name"),
                            intParam(req, "reorderLevel"), moneyParam(req, "unitCost"));
                    return "Đã lưu linh kiện " + param(req, "partCode");
                });
                break;
            case "/inventory/requests":
                perform(req, resp, "/inventory/requests", () -> {
                    int logId = intParam(req, "repairLogId");
                    int partId = intParam(req, "partId");
                    if ("reject".equals(action)) {
                        inventoryService.rejectPart(logId, partId);
                        return "Đã từ chối yêu cầu và hoàn lại tồn kho";
                    }
                    inventoryService.deliverPart(logId, partId, staffId);
                    return "Đã giao linh kiện cho kỹ thuật viên";
                });
                break;
            default:
                if ("receive".equals(action)) {
                    perform(req, resp, "/inventory/po?action=view&id=" + id, () -> {
                        inventoryService.receivePurchaseOrder(id, staffId);
                        return "Đã nhận hàng và cập nhật tồn kho";
                    });
                } else {
                    int supplierId = intParam(req, "supplierId");
                    perform(req, resp, "/inventory/po?action=new&supplierId=" + supplierId, () -> {
                        String code = inventoryService.createPurchaseOrder(staffId, supplierId, poLines(req));
                        return "Đã tạo phiếu nhập " + code + " và gửi nhà cung cấp";
                    });
                }
        }
    }

    /** Doc cac dong item = "PART:3" kem qty_PART:3 / cost_PART:3; bo qua dong so luong 0. */
    private List<PoInput> poLines(HttpServletRequest req) throws BusinessException {
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
