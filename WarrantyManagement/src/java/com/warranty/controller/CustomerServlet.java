package com.warranty.controller;

import com.warranty.dao.NotificationDAO;
import com.warranty.dao.OrderDAO;
import com.warranty.model.Entities.User;
import com.warranty.service.Tx;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Khach hang: san pham da mua + bao hanh, don hang, thong bao.
 * doGet chi dieu huong; xu ly nam trong cac ham rieng ben duoi.
 */
@WebServlet({"/customer/warranties", "/customer/notifications"})
public class CustomerServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("/customer/notifications".equals(req.getServletPath())) {
                showNotifications(req, resp);
            } else {
                showWarranties(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Danh sach thong bao cua khach; hien xong thi danh dau da doc. */
    private void showNotifications(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User me = currentUser(req);
        req.setAttribute("notifications", Tx.read(c -> NotificationDAO.ofCustomer(c, me.id())));
        render(req, resp, "customer/notifications");
        markNotificationsRead(me.id());
    }

    private void markNotificationsRead(int customerId) throws SQLException {
        Tx.read(c -> {
            NotificationDAO.markAllRead(c, customerId);
            return null;
        });
    }

    /** San pham da mua + bao hanh (cap nhat bao hanh het han truoc) va don hang cua khach. */
    private void showWarranties(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User me = currentUser(req);
        Tx.read(c -> {
            OrderDAO.expireWarranties(c);
            req.setAttribute("warranties", OrderDAO.warrantiesOfCustomer(c, me.id()));
            req.setAttribute("orders", OrderDAO.search(c, null, null, me.id()));
            return null;
        });
        render(req, resp, "customer/warranties");
    }
}
