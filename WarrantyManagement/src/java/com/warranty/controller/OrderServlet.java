package com.warranty.controller;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.OrderDAO;
import com.warranty.dao.UserDAO;
import com.warranty.model.Codes.OrderStatus;
import com.warranty.model.Codes.PaymentMethod;
import com.warranty.model.Entities.Order;
import com.warranty.model.Role;
import com.warranty.service.BusinessException;
import com.warranty.service.OrderService;
import com.warranty.service.Tx;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flow 3 – Customer support: tao don hang, thanh toan, kich hoat bao hanh.
 * doGet / doPost chi dieu huong theo action; xu ly nam trong cac ham rieng ben duoi.
 */
@WebServlet("/support/orders")
public class OrderServlet extends BaseServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        try {
            if ("new".equals(action)) {
                showCreateOrderForm(req, resp);
            } else if ("view".equals(action)) {
                showOrderDetail(req, resp);
            } else {
                showOrderList(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        if ("create".equals(action)) {
            createOrder(req, resp);
        } else if ("cancel".equals(action)) {
            cancelOrder(req, resp);
        } else {
            payOrder(req, resp);
        }
    }

    // ============================ GET ============================

    /** Form tao don: danh sach khach hang dang hoat dong + san pham dang kinh doanh. */
    private void showCreateOrderForm(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        Tx.read(c -> {
            req.setAttribute("customers", UserDAO.activeByRole(c, Role.CUSTOMER));
            req.setAttribute("products", CatalogDAO.products(c, null, true));
            return null;
        });
        render(req, resp, "support/order-new");
    }

    /** Chi tiet don: san pham, thanh toan, bao hanh da sinh. */
    private void showOrderDetail(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        int id = intParam(req, "id");
        Order o = Tx.read(c -> {
            Order order = OrderDAO.find(c, id);
            if (order != null) {
                req.setAttribute("items", OrderDAO.items(c, id));
                req.setAttribute("payments", OrderDAO.paymentsOfOrder(c, id));
                req.setAttribute("warranties", OrderDAO.warrantiesOfOrder(c, id));
            }
            return order;
        });
        if (o == null) {
            flashError(req, "Đơn hàng không tồn tại");
            redirect(req, resp, "/support/orders");
            return;
        }
        req.setAttribute("order", o);
        render(req, resp, "support/order-detail");
    }

    /** Danh sach don hang, loc theo trang thai va tu khoa. */
    private void showOrderList(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        OrderStatus status = enumParam(req, "status", OrderStatus.class);
        req.setAttribute("orders", Tx.read(c -> OrderDAO.search(c, status, param(req, "q"), null)));
        req.setAttribute("status", status);
        req.setAttribute("keyword", param(req, "q"));
        render(req, resp, "support/orders");
    }

    // ============================ POST ============================

    /** Flow 3 buoc 1-4: tao don PENDING; thanh cong -> trang chi tiet, loi -> quay lai form. */
    private void createOrder(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int orderId = orderService.createOrder(currentUser(req).id(), intParam(req, "customerId"),
                    readOrderLines(req));
            req.getSession().setAttribute("flash", "Đã tạo đơn hàng (chờ thanh toán)");
            redirect(req, resp, "/support/orders?action=view&id=" + orderId);
        } catch (BusinessException e) {
            flashError(req, e.getMessage());
            redirect(req, resp, "/support/orders?action=new");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Huy don dang cho thanh toan. */
    private void cancelOrder(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id = intParam(req, "id");
        perform(req, resp, orderDetailPath(id), () -> {
            orderService.cancel(id);
            return "Đã hủy đơn hàng";
        });
    }

    /** Flow 3 buoc 5-14: thanh toan qua cong gia lap; that bai -> bao loi (don da bi huy). */
    private void payOrder(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id = intParam(req, "id");
        perform(req, resp, orderDetailPath(id), () -> {
            OrderService.PaymentResult r = orderService.pay(id, enumParam(req, "method", PaymentMethod.class),
                    boolParam(req, "simulateFail"));
            if (!r.success()) {
                throw new BusinessException(r.message());
            }
            return r.message();
        });
    }

    private static String orderDetailPath(int id) {
        return "/support/orders?action=view&id=" + id;
    }

    /** Doc cac dong productId[] / qty[]; gop san pham trung. */
    private Map<Integer, Integer> readOrderLines(HttpServletRequest req) throws BusinessException {
        String[] ids = req.getParameterValues("productId");
        String[] qtys = req.getParameterValues("qty");
        Map<Integer, Integer> out = new LinkedHashMap<>();
        if (ids == null || qtys == null) {
            return out;
        }
        for (int i = 0; i < ids.length && i < qtys.length; i++) {
            if (ids[i].isBlank()) {
                continue;
            }
            try {
                int pid = Integer.parseInt(ids[i]);
                int q = Integer.parseInt(qtys[i].trim());
                out.merge(pid, q, Integer::sum);
            } catch (NumberFormatException e) {
                throw new BusinessException("Số lượng không hợp lệ");
            }
        }
        return out;
    }
}
