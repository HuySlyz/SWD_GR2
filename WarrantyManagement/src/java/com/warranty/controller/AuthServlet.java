package com.warranty.controller;

import com.warranty.model.Entities.User;
import com.warranty.service.BusinessException;
import com.warranty.service.UserService;
import com.warranty.util.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/** Sequence "1. Login": nhap email + mat khau -> chuyen toi dashboard theo role. */
@WebServlet({"/login", "/logout"})
public class AuthServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/logout".equals(req.getServletPath())) {
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.invalidate();
            }
            redirect(req, resp, "/login");
            return;
        }
        User u = currentUser(req);
        if (u != null) {
            redirect(req, resp, u.role().getHomePath());
            return;
        }
        render(req, resp, "common/login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            User u = userService.login(param(req, "email"), req.getParameter("password"));
            HttpSession old = req.getSession(false);
            if (old != null) {
                old.invalidate(); // chong session fixation
            }
            req.getSession(true).setAttribute(AuthFilter.SESSION_USER, u);
            redirect(req, resp, u.role().getHomePath());
        } catch (BusinessException e) {
            req.setAttribute("loginError", e.getMessage());
            req.setAttribute("email", param(req, "email"));
            render(req, resp, "common/login");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
