package com.warranty.util;

import com.warranty.model.Entities.User;
import com.warranty.model.Role;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Sequence "2. Authentication": moi request phai co phien dang nhap hop le,
 * va role phai duoc phep truy cap khu vuc tuong ung. Phien het han -> ve trang dang nhap.
 * (Web MVC dung HttpSession thay cho access/refresh token cua kien truc API trong so do.)
 */
public class AuthFilter implements Filter {

    public static final String SESSION_USER = "currentUser";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(req, resp);
            return;
        }
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SESSION_USER);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        Set<Role> allowed = allowedRoles(path);
        if (allowed != null && !allowed.contains(user.role())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng này");
            return;
        }
        chain.doFilter(req, resp);
    }

    private static boolean isPublic(String path) {
        return path.equals("/") || path.equals("/index.jsp") || path.equals("/login") || path.equals("/logout")
                || path.equals("/lookup") || path.startsWith("/css/");
    }

    /** null = moi nguoi dung da dang nhap. */
    private static Set<Role> allowedRoles(String path) {
        if (path.startsWith("/admin/")) {
            return EnumSet.of(Role.ADMIN);
        }
        if (path.startsWith("/support/")) {
            return EnumSet.of(Role.SUPPORT);
        }
        if (path.startsWith("/inventory/")) {
            return EnumSet.of(Role.INVENTORY);
        }
        if (path.startsWith("/customer/")) {
            return EnumSet.of(Role.CUSTOMER);
        }
        if (path.startsWith("/tickets")) {
            return EnumSet.of(Role.SUPPORT, Role.TECHNICIAN, Role.CUSTOMER, Role.ADMIN);
        }
        return null;
    }
}
