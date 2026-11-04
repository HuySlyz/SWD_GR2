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

/** Khach hang: san pham da mua + bao hanh, don hang, thong bao. */
@WebServlet({"/customer/warranties", "/customer/notifications"})
public class CustomerServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User me = currentUser(req);
        try {
            if ("/customer/notifications".equals(req.getServletPath())) {
                req.setAttribute("notifications", Tx.read(c -> NotificationDAO.ofCustomer(c, me.id())));
                render(req, resp, "customer/notifications");
                Tx.read(c -> {
                    NotificationDAO.markAllRead(c, me.id());
                    return null;
                });
                return;
            }
            Tx.read(c -> {
                OrderDAO.expireWarranties(c);
                req.setAttribute("warranties", OrderDAO.warrantiesOfCustomer(c, me.id()));
                req.setAttribute("orders", OrderDAO.search(c, null, null, me.id()));
                return null;
            });
            render(req, resp, "customer/warranties");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
