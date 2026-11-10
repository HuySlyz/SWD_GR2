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

/**
 * Sequence "1. Login": nhap email + mat khau -> chuyen toi dashboard theo role.
 * doGet / doPost chi dieu huong; xu ly nam trong cac ham rieng ben duoi.
 */
@WebServlet({"/login", "/logout"})
public class AuthServlet extends BaseServlet {

    private static final String LOGIN_VIEW = "common/login";

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/logout".equals(req.getServletPath())) {
            logout(req, resp);
        } else {
            showLoginPage(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleLogin(req, resp);
    }

    /** Hien form dang nhap; da dang nhap roi thi ve trang chu theo role. */
    private void showLoginPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User u = currentUser(req);
        if (u != null) {
            redirectToHome(req, resp, u);
            return;
        }
        render(req, resp, LOGIN_VIEW);
    }

    /** Xac thuc email + mat khau qua UserService; dung -> tao phien, sai -> hien loi tren form. */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            User u = userService.login(param(req, "email"), req.getParameter("password"));
            createUserSession(req, u);
            redirectToHome(req, resp, u);
        } catch (BusinessException e) {
            showLoginError(req, resp, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Huy phien cu (chong session fixation) roi luu user vao phien moi. */
    private void createUserSession(HttpServletRequest req, User u) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        req.getSession(true).setAttribute(AuthFilter.SESSION_USER, u);
    }

    /** Chuyen toi trang chu cua role (Admin -> dashboard, Support/Tech -> tickets, ...). */
    private void redirectToHome(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        redirect(req, resp, u.role().getHomePath());
    }

    /** Hien lai form dang nhap kem thong bao loi va email da nhap. */
    private void showLoginError(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        req.setAttribute("loginError", message);
        req.setAttribute("email", param(req, "email"));
        render(req, resp, LOGIN_VIEW);
    }

    /** Dang xuat: huy phien va ve trang dang nhap. */
    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate();
        }
        redirect(req, resp, "/login");
    }
}
