package com.warranty.controller;

import com.warranty.dao.NotificationDAO;
import com.warranty.model.Entities.User;
import com.warranty.model.Role;
import com.warranty.service.BusinessException;
import com.warranty.service.Tx;
import com.warranty.util.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Ham dung chung cho cac controller. */
public abstract class BaseServlet extends HttpServlet {

    /** Mot thao tac ghi; tra ve thong bao thanh cong (null = khong thong bao). */
    @FunctionalInterface
    protected interface Action {
        String run() throws SQLException, BusinessException;
    }

    protected User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute(AuthFilter.SESSION_USER);
    }

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        User u = currentUser(req);
        if (u != null && u.role() == Role.CUSTOMER) {
            try {
                req.setAttribute("unreadCount", Tx.read(c -> NotificationDAO.unreadCount(c, u.id())));
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    /**
     * Thuc hien thao tac POST theo mo hinh Post/Redirect/Get:
     * thanh cong -> flash xanh, loi nghiep vu -> flash do, roi chuyen ve {@code back}.
     */
    protected void perform(HttpServletRequest req, HttpServletResponse resp, String back, Action action)
            throws ServletException, IOException {
        try {
            String msg = action.run();
            if (msg != null) {
                req.getSession().setAttribute("flash", msg);
            }
        } catch (BusinessException e) {
            req.getSession().setAttribute("error", e.getMessage());
        } catch (NumberFormatException | DateTimeParseException e) {
            req.getSession().setAttribute("error", "Dữ liệu nhập không hợp lệ");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + back);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected void flashError(HttpServletRequest req, String message) {
        req.getSession().setAttribute("error", message);
    }

    // ---------------- doc tham so ----------------
    protected String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? null : v.trim();
    }

    protected int intParam(HttpServletRequest req, String name) {
        try {
            return Integer.parseInt(param(req, name));
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    protected Integer optInt(HttpServletRequest req, String name) {
        int v = intParam(req, name);
        return v == 0 ? null : v;
    }

    protected boolean boolParam(HttpServletRequest req, String name) {
        String v = param(req, name);
        return "on".equals(v) || "true".equals(v) || "1".equals(v);
    }

    /** Tien VND: chap nhan "1.290.000" hoac "1290000". null neu sai dinh dang. */
    protected BigDecimal moneyParam(HttpServletRequest req, String name) {
        String v = param(req, name);
        if (v == null || v.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(v.replace(".", "").replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected <E extends Enum<E>> E enumParam(HttpServletRequest req, String name, Class<E> type) {
        String v = param(req, name);
        if (v == null || v.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected List<Integer> intValues(HttpServletRequest req, String name) {
        List<Integer> out = new ArrayList<>();
        String[] vs = req.getParameterValues(name);
        if (vs != null) {
            for (String v : vs) {
                try {
                    out.add(Integer.parseInt(v));
                } catch (NumberFormatException ignored) {
                    // bo qua gia tri khong hop le
                }
            }
        }
        return out;
    }
}
