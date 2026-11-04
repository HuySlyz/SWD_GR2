package com.warranty.controller;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.ReportDAO;
import com.warranty.dao.UserDAO;
import com.warranty.model.Entities.User;
import com.warranty.model.Role;
import com.warranty.service.CatalogService;
import com.warranty.service.Tx;
import com.warranty.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Use case Admin: bao cao thong ke, quan ly tai khoan (nhan vien + khach hang),
 * san pham & chinh sach bao hanh, nguoi ban, nha cung cap.
 */
@WebServlet({"/admin/dashboard", "/admin/users", "/admin/products", "/admin/partners"})
public class AdminServlet extends BaseServlet {

    private final UserService userService = new UserService();
    private final CatalogService catalogService = new CatalogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/admin/dashboard":
                    Tx.read(c -> {
                        req.setAttribute("summary", ReportDAO.summary(c));
                        req.setAttribute("topProducts", ReportDAO.topProducts(c));
                        req.setAttribute("techLoad", ReportDAO.technicianLoad(c));
                        req.setAttribute("lowStock", CatalogDAO.stock(c, true));
                        return null;
                    });
                    render(req, resp, "admin/dashboard");
                    break;
                case "/admin/users": {
                    Role role = enumParam(req, "role", Role.class);
                    int editId = intParam(req, "id");
                    Tx.read(c -> {
                        req.setAttribute("users", UserDAO.search(c, role, param(req, "q")));
                        req.setAttribute("editing", editId > 0 ? UserDAO.findById(c, editId) : null);
                        return null;
                    });
                    req.setAttribute("role", role);
                    req.setAttribute("keyword", param(req, "q"));
                    render(req, resp, "admin/users");
                    break;
                }
                case "/admin/products": {
                    int editId = intParam(req, "id");
                    Tx.read(c -> {
                        req.setAttribute("products", CatalogDAO.products(c, param(req, "q"), false));
                        req.setAttribute("sellers", CatalogDAO.sellers(c));
                        req.setAttribute("editing", editId > 0 ? CatalogDAO.product(c, editId) : null);
                        return null;
                    });
                    req.setAttribute("keyword", param(req, "q"));
                    render(req, resp, "admin/products");
                    break;
                }
                default: {
                    int supplierId = intParam(req, "supplierId");
                    Tx.read(c -> {
                        req.setAttribute("sellers", CatalogDAO.sellers(c));
                        req.setAttribute("suppliers", CatalogDAO.suppliers(c));
                        req.setAttribute("products", CatalogDAO.products(c, null, false));
                        req.setAttribute("parts", CatalogDAO.parts(c, null));
                        req.setAttribute("editSupplier", supplierId > 0 ? CatalogDAO.supplier(c, supplierId) : null);
                        req.setAttribute("supplierItems", supplierId > 0
                                ? CatalogDAO.supplierItemKeys(c, supplierId) : java.util.List.of());
                        return null;
                    });
                    req.setAttribute("editSellerId", intParam(req, "sellerId"));
                    render(req, resp, "admin/partners");
                }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        String action = param(req, "action");
        User me = currentUser(req);
        int id = intParam(req, "id");
        switch (path) {
            case "/admin/users":
                perform(req, resp, "/admin/users", () -> {
                    if ("toggle".equals(action)) {
                        boolean activate = boolParam(req, "active");
                        userService.setActive(id, activate, me.id());
                        return activate ? "Đã kích hoạt tài khoản" : "Đã vô hiệu hóa tài khoản";
                    }
                    if ("delete".equals(action)) {
                        userService.delete(id, me.id());
                        return "Đã xóa tài khoản";
                    }
                    if (id == 0) {
                        userService.create(enumParam(req, "role", Role.class), param(req, "username"),
                                req.getParameter("password"), param(req, "fullName"), param(req, "email"),
                                param(req, "phone"), param(req, "address"));
                        return "Đã tạo tài khoản " + param(req, "username");
                    }
                    userService.update(id, param(req, "fullName"), param(req, "email"), param(req, "phone"),
                            param(req, "address"), req.getParameter("password"));
                    return "Đã cập nhật tài khoản";
                });
                break;
            case "/admin/products":
                perform(req, resp, "/admin/products", () -> {
                    if ("toggle".equals(action)) {
                        catalogService.setProductActive(id, boolParam(req, "active"));
                        return "Đã cập nhật trạng thái kinh doanh";
                    }
                    if ("delete".equals(action)) {
                        catalogService.deleteProduct(id);
                        return "Đã xóa sản phẩm";
                    }
                    BigDecimal price = moneyParam(req, "price");
                    catalogService.saveProduct(id, optInt(req, "sellerId"), param(req, "sku"), param(req, "name"),
                            price, intParam(req, "warrantyMonths"), intParam(req, "reorderLevel"));
                    return "Đã lưu sản phẩm " + param(req, "sku");
                });
                break;
            default:
                perform(req, resp, "/admin/partners", () -> {
                    switch (action == null ? "" : action) {
                        case "saveSeller":
                            catalogService.saveSeller(id, param(req, "name"), param(req, "phone"), param(req, "email"));
                            return "Đã lưu người bán";
                        case "deleteSeller":
                            catalogService.deleteSeller(id);
                            return "Đã xóa người bán";
                        case "saveSupplier":
                            catalogService.saveSupplier(id, param(req, "name"), param(req, "phone"),
                                    param(req, "email"), param(req, "address"), intValues(req, "productIds"),
                                    intValues(req, "partIds"));
                            return "Đã lưu nhà cung cấp";
                        case "deleteSupplier":
                            catalogService.deleteSupplier(id);
                            return "Đã xóa nhà cung cấp";
                        default:
                            return null;
                    }
                });
        }
    }
}
