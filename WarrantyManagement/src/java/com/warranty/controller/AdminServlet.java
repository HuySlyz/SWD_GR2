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
import java.util.List;

/**
 * Use case Admin: bao cao thong ke, quan ly tai khoan (nhan vien + khach hang),
 * san pham & chinh sach bao hanh, nguoi ban, nha cung cap.
 * doGet / doPost chi dieu huong theo duong dan + action; xu ly nam trong cac ham rieng ben duoi.
 */
@WebServlet({"/admin/dashboard", "/admin/users", "/admin/products", "/admin/partners"})
public class AdminServlet extends BaseServlet {

    private final UserService userService = new UserService();
    private final CatalogService catalogService = new CatalogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/admin/dashboard":
                    showDashboard(req, resp);
                    break;
                case "/admin/users":
                    showUsers(req, resp);
                    break;
                case "/admin/products":
                    showProducts(req, resp);
                    break;
                default:
                    showPartners(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        int id = intParam(req, "id");
        switch (req.getServletPath()) {
            case "/admin/users":
                if ("toggle".equals(action)) {
                    toggleUserActive(req, resp);
                } else if ("delete".equals(action)) {
                    deleteUser(req, resp);
                } else if (id == 0) {
                    createUser(req, resp);
                } else {
                    updateUser(req, resp);
                }
                break;
            case "/admin/products":
                if ("toggle".equals(action)) {
                    toggleProductActive(req, resp);
                } else if ("delete".equals(action)) {
                    deleteProduct(req, resp);
                } else {
                    saveProduct(req, resp);
                }
                break;
            default:
                switch (action == null ? "" : action) {
                    case "saveSeller":
                        saveSeller(req, resp);
                        break;
                    case "deleteSeller":
                        deleteSeller(req, resp);
                        break;
                    case "saveSupplier":
                        saveSupplier(req, resp);
                        break;
                    case "deleteSupplier":
                        deleteSupplier(req, resp);
                        break;
                    default:
                        redirect(req, resp, "/admin/partners");
                }
        }
    }

    // ============================ GET ============================

    /** Bao cao thong ke: tong quan, san pham ban chay, tai cong viec KTV, hang duoi muc ton. */
    private void showDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        Tx.read(c -> {
            req.setAttribute("summary", ReportDAO.summary(c));
            req.setAttribute("topProducts", ReportDAO.topProducts(c));
            req.setAttribute("techLoad", ReportDAO.technicianLoad(c));
            req.setAttribute("lowStock", CatalogDAO.stock(c, true));
            return null;
        });
        render(req, resp, "admin/dashboard");
    }

    /** Danh sach tai khoan (loc role / tu khoa) + tai khoan dang sua neu co ?id=. */
    private void showUsers(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
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
    }

    /** Danh sach san pham + nguoi ban + san pham dang sua neu co ?id=. */
    private void showProducts(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        int editId = intParam(req, "id");
        Tx.read(c -> {
            req.setAttribute("products", CatalogDAO.products(c, param(req, "q"), false));
            req.setAttribute("sellers", CatalogDAO.sellers(c));
            req.setAttribute("editing", editId > 0 ? CatalogDAO.product(c, editId) : null);
            return null;
        });
        req.setAttribute("keyword", param(req, "q"));
        render(req, resp, "admin/products");
    }

    /** Nguoi ban + nha cung cap (kem mat hang cung cap cua NCC dang sua). */
    private void showPartners(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        int supplierId = intParam(req, "supplierId");
        Tx.read(c -> {
            req.setAttribute("sellers", CatalogDAO.sellers(c));
            req.setAttribute("suppliers", CatalogDAO.suppliers(c));
            req.setAttribute("products", CatalogDAO.products(c, null, false));
            req.setAttribute("parts", CatalogDAO.parts(c, null));
            req.setAttribute("editSupplier", supplierId > 0 ? CatalogDAO.supplier(c, supplierId) : null);
            req.setAttribute("supplierItems", supplierId > 0
                    ? CatalogDAO.supplierItemKeys(c, supplierId) : List.of());
            return null;
        });
        req.setAttribute("editSellerId", intParam(req, "sellerId"));
        render(req, resp, "admin/partners");
    }

    // ============================ POST: tai khoan ============================

    private void createUser(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/admin/users", () -> {
            userService.create(enumParam(req, "role", Role.class), param(req, "username"),
                    req.getParameter("password"), param(req, "fullName"), param(req, "email"),
                    param(req, "phone"), param(req, "address"));
            return "Đã tạo tài khoản " + param(req, "username");
        });
    }

    private void updateUser(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/admin/users", () -> {
            userService.update(intParam(req, "id"), param(req, "fullName"), param(req, "email"),
                    param(req, "phone"), param(req, "address"), req.getParameter("password"));
            return "Đã cập nhật tài khoản";
        });
    }

    /** Kich hoat / vo hieu hoa tai khoan (khong tu khoa chinh minh). */
    private void toggleUserActive(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User me = currentUser(req);
        perform(req, resp, "/admin/users", () -> {
            boolean activate = boolParam(req, "active");
            userService.setActive(intParam(req, "id"), activate, me.id());
            return activate ? "Đã kích hoạt tài khoản" : "Đã vô hiệu hóa tài khoản";
        });
    }

    private void deleteUser(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User me = currentUser(req);
        perform(req, resp, "/admin/users", () -> {
            userService.delete(intParam(req, "id"), me.id());
            return "Đã xóa tài khoản";
        });
    }

    // ============================ POST: san pham ============================

    /** Them moi (id = 0) hoac cap nhat san pham + chinh sach bao hanh (warranty_months). */
    private void saveProduct(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/admin/products", () -> {
            BigDecimal price = moneyParam(req, "price");
            catalogService.saveProduct(intParam(req, "id"), optInt(req, "sellerId"), param(req, "sku"),
                    param(req, "name"), price, intParam(req, "warrantyMonths"), intParam(req, "reorderLevel"));
            return "Đã lưu sản phẩm " + param(req, "sku");
        });
    }

    /** Dang / ngung kinh doanh san pham. */
    private void toggleProductActive(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        perform(req, resp, "/admin/products", () -> {
            catalogService.setProductActive(intParam(req, "id"), boolParam(req, "active"));
            return "Đã cập nhật trạng thái kinh doanh";
        });
    }

    private void deleteProduct(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/admin/products", () -> {
            catalogService.deleteProduct(intParam(req, "id"));
            return "Đã xóa sản phẩm";
        });
    }

    // ============================ POST: nguoi ban / nha cung cap ============================

    private void saveSeller(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/admin/partners", () -> {
            catalogService.saveSeller(intParam(req, "id"), param(req, "name"), param(req, "phone"),
                    param(req, "email"));
            return "Đã lưu người bán";
        });
    }

    private void deleteSeller(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/admin/partners", () -> {
            catalogService.deleteSeller(intParam(req, "id"));
            return "Đã xóa người bán";
        });
    }

    /** Luu nha cung cap kem danh sach san pham / linh kien ho cung cap. */
    private void saveSupplier(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        perform(req, resp, "/admin/partners", () -> {
            catalogService.saveSupplier(intParam(req, "id"), param(req, "name"), param(req, "phone"),
                    param(req, "email"), param(req, "address"), intValues(req, "productIds"),
                    intValues(req, "partIds"));
            return "Đã lưu nhà cung cấp";
        });
    }

    private void deleteSupplier(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        perform(req, resp, "/admin/partners", () -> {
            catalogService.deleteSupplier(intParam(req, "id"));
            return "Đã xóa nhà cung cấp";
        });
    }
}
