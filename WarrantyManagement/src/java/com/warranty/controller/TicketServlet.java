package com.warranty.controller;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.OrderDAO;
import com.warranty.dao.TicketDAO;
import com.warranty.dao.UserDAO;
import com.warranty.model.Codes.Channel;
import com.warranty.model.Codes.PaymentMethod;
import com.warranty.model.Entities.Ticket;
import com.warranty.model.Entities.User;
import com.warranty.model.Role;
import com.warranty.model.TicketStatus;
import com.warranty.service.BusinessException;
import com.warranty.service.OrderService;
import com.warranty.service.TicketService;
import com.warranty.service.Tx;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Ticket bao hanh cho 3 vai tro:
 *  - Customer: gui yeu cau, theo doi, thanh toan phi, xac nhan hoan thanh
 *  - Customer support / Admin: tao thay khach, duyet & phan cong ky thuat, dong ticket tai quay
 *  - Technician: ticket duoc giao -> nhan viec, xin linh kien, hoan tat, ban giao
 */
@WebServlet("/tickets")
public class TicketServlet extends BaseServlet {

    private final TicketService ticketService = new TicketService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User me = currentUser(req);
        String action = param(req, "action");
        try {
            if ("new".equals(action)) {
                if (me.role() == Role.TECHNICIAN) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                if (me.role() == Role.CUSTOMER) {
                    req.setAttribute("myWarranties", Tx.read(c -> OrderDAO.warrantiesOfCustomer(c, me.id())));
                }
                req.setAttribute("serial", param(req, "serial"));
                render(req, resp, "common/ticket-new");
            } else if ("view".equals(action)) {
                showDetail(req, resp, me, intParam(req, "id"));
            } else {
                TicketStatus status = enumParam(req, "status", TicketStatus.class);
                Integer techFilter = me.role() == Role.TECHNICIAN ? Integer.valueOf(me.id()) : null;
                Integer customerFilter = me.role() == Role.CUSTOMER ? Integer.valueOf(me.id()) : null;
                req.setAttribute("tickets", Tx.read(c ->
                        TicketDAO.search(c, status, techFilter, customerFilter, param(req, "q"))));
                req.setAttribute("status", status);
                req.setAttribute("keyword", param(req, "q"));
                render(req, resp, "common/tickets");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, User me, int id)
            throws SQLException, ServletException, IOException {
        Ticket t = Tx.read(c -> TicketDAO.find(c, id));
        if (t == null || !canView(me, t)) {
            flashError(req, "Ticket không tồn tại hoặc bạn không có quyền xem");
            redirect(req, resp, "/tickets");
            return;
        }
        Tx.read(c -> {
            req.setAttribute("logs", TicketDAO.logs(c, id));
            req.setAttribute("parts", TicketDAO.partsOfTicket(c, id));
            req.setAttribute("handovers", TicketDAO.handovers(c, id));
            req.setAttribute("payments", OrderDAO.paymentsOfTicket(c, id));
            req.setAttribute("paid", TicketService.isPaid(c, id));
            if (me.role() != Role.CUSTOMER) {
                req.setAttribute("technicians", UserDAO.activeByRole(c, Role.TECHNICIAN));
            }
            if (me.role() == Role.TECHNICIAN) {
                req.setAttribute("allParts", CatalogDAO.parts(c, null));
            }
            return null;
        });
        req.setAttribute("ticket", t);
        render(req, resp, "common/ticket-detail");
    }

    private static boolean canView(User me, Ticket t) {
        switch (me.role()) {
            case CUSTOMER:
                return t.warranty().customerId() == me.id();
            case TECHNICIAN:
                return t.technicianId() != null && t.technicianId() == me.id();
            default:
                return true;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User me = currentUser(req);
        String action = param(req, "action");
        int id = intParam(req, "id");

        if ("create".equals(action)) {
            perform(req, resp, me.role() == Role.CUSTOMER ? "/tickets" : "/tickets?status=NEW", () -> {
                requireRole(me, Role.CUSTOMER, Role.SUPPORT);
                boolean paid = boolParam(req, "acceptPaidRepair");
                String code = me.role() == Role.CUSTOMER
                        ? ticketService.submitByCustomer(me.id(), param(req, "serial"), param(req, "issue"), paid)
                        : ticketService.submitBySupport(me.id(), param(req, "serial"), param(req, "issue"),
                                enumParam(req, "channel", Channel.class), paid);
                return "Đã tạo ticket " + code + " (trạng thái: Mới)";
            });
            return;
        }

        // Sau khi ban giao, ky thuat vien cu khong con quyen xem ticket -> ve danh sach
        String back = "handover".equals(action) ? "/tickets" : "/tickets?action=view&id=" + id;
        perform(req, resp, back, () -> {
            switch (action == null ? "" : action) {
                case "assign":
                    requireRole(me, Role.SUPPORT, Role.ADMIN);
                    ticketService.assign(id, supportIdOf(me), intParam(req, "technicianId"));
                    return "Đã phân công kỹ thuật viên";
                case "accept":
                    requireRole(me, Role.TECHNICIAN);
                    ticketService.accept(id, me.id(), param(req, "diagnosis"));
                    return "Đã nhận việc – ticket chuyển sang Đang sửa chữa";
                case "requestPart":
                    requireRole(me, Role.TECHNICIAN);
                    return ticketService.requestPart(id, me.id(), intParam(req, "partId"), intParam(req, "quantity"));
                case "complete":
                    requireRole(me, Role.TECHNICIAN);
                    ticketService.complete(id, me.id(), param(req, "result"), param(req, "shiftNote"),
                            moneyParam(req, "repairFee"));
                    return "Đã hoàn tất sửa chữa và thông báo khách hàng";
                case "handover":
                    requireRole(me, Role.TECHNICIAN);
                    ticketService.handover(id, me.id(), intParam(req, "toTechnicianId"), param(req, "note"));
                    return "Đã bàn giao ticket cho kỹ thuật viên khác";
                case "pay": {
                    requireRole(me, Role.CUSTOMER, Role.SUPPORT, Role.ADMIN);
                    OrderService.PaymentResult r = ticketService.payRepairFee(id, me,
                            enumParam(req, "method", PaymentMethod.class), boolParam(req, "simulateFail"));
                    if (!r.success()) {
                        throw new BusinessException(r.message());
                    }
                    return r.message();
                }
                case "close":
                    requireRole(me, Role.CUSTOMER, Role.SUPPORT, Role.ADMIN);
                    ticketService.close(id, me);
                    return "Đã đóng ticket";
                default:
                    throw new BusinessException("Thao tác không hợp lệ");
            }
        });
    }

    private static void requireRole(User me, Role... roles) throws BusinessException {
        for (Role r : roles) {
            if (me.role() == r) {
                return;
            }
        }
        throw new BusinessException("Bạn không có quyền thực hiện thao tác này");
    }

    /** Admin khong nam trong support_staff -> khong ghi support_id. */
    private static int supportIdOf(User me) throws BusinessException {
        if (me.role() != Role.SUPPORT) {
            throw new BusinessException("Chỉ Customer Support mới được phân công ticket");
        }
        return me.id();
    }
}
