package com.warranty.controller;

import com.warranty.dao.TicketDAO;
import com.warranty.model.Entities.Warranty;
import com.warranty.service.BusinessException;
import com.warranty.service.TicketService;
import com.warranty.service.Tx;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Tra cuu bao hanh theo serial / ma bao hanh (context diagram: "warranty lookup"). Cong khai.
 * doGet chi dieu huong; xu ly nam trong cac ham rieng ben duoi.
 */
@WebServlet("/lookup")
public class LookupServlet extends BaseServlet {

    private final TicketService ticketService = new TicketService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        showLookupPage(req, resp);
    }

    /** Hien trang tra cuu; neu co serial thi tra cuu va hien ket qua. */
    private void showLookupPage(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String serial = param(req, "serial");
        req.setAttribute("serial", serial);
        if (serial != null && !serial.isEmpty()) {
            lookupWarranty(req, serial);
        }
        render(req, resp, "common/lookup");
    }

    /** Tim bao hanh theo serial kem lich su ticket; khong thay -> dat thong bao loi. */
    private void lookupWarranty(HttpServletRequest req, String serial) throws ServletException {
        try {
            Warranty w = ticketService.lookup(serial);
            req.setAttribute("warranty", w);
            req.setAttribute("history", Tx.read(c -> TicketDAO.ofWarranty(c, w.id())));
        } catch (BusinessException e) {
            req.setAttribute("lookupError", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
