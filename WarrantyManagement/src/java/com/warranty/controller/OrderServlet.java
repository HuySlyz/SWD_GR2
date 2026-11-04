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

/** Flow 3 – Customer support: tao don hang, thanh toan, kich hoat bao hanh. */
@WebServlet("/support/orders")
public class OrderServlet extends BaseServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        try {
            if ("new".equals(action)) {
                Tx.read(c -> {
                    req.setAttribute("customers", UserDAO.activeByRole(c, Role.CUSTOMER));
                    req.setAttribute("products", CatalogDAO.products(c, null, true));
                    return null;
                });
                render(req, resp, "support/order-new");
            } else if ("view".equals(action)) {
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
            } else {
                OrderStatus status = enumParam(req, "status", OrderStatus.class);
                req.setAttribute("orders", Tx.read(c -> OrderDAO.search(c, status, param(req, "q"), null)));
                req.setAttribute("status", status);
                req.setAttribute("keyword", param(req, "q"));
                render(req, resp, "support/orders");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        int id = intParam(req, "id");
        if ("create".equals(action)) {
            try {
                int orderId = orderService.createOrder(currentUser(req).id(), intParam(req, "customerId"), lines(req));
                req.getSession().setAttribute("flash", "Đã tạo đơn hàng (chờ thanh toán)");
                redirect(req, resp, "/support/orders?action=view&id=" + orderId);
            } catch (BusinessException e) {
                flashError(req, e.getMessage());
                redirect(req, resp, "/support/orders?action=new");
            } catch (SQLException e) {
                throw new ServletException(e);
            }
            return;
        }
        perform(req, resp, "/support/orders?action=view&id=" + id, () -> {
            if ("cancel".equals(action)) {
                orderService.cancel(id);
                return "Đã hủy đơn hàng";
            }
            OrderService.PaymentResult r = orderService.pay(id, enumParam(req, "method", PaymentMethod.class),
                    boolParam(req, "simulateFail"));
            if (!r.success()) {
                throw new BusinessException(r.message());
            }
            return r.message();
        });
    }

    /** Doc cac dong productId[] / qty[]; gop san pham trung. */
    private Map<Integer, Integer> lines(HttpServletRequest req) throws BusinessException {
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
