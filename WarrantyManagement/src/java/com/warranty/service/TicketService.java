package com.warranty.service;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.NotificationDAO;
import com.warranty.dao.OrderDAO;
import com.warranty.dao.Sql;
import com.warranty.dao.TicketDAO;
import com.warranty.dao.UserDAO;
import com.warranty.model.Codes.Channel;
import com.warranty.model.Codes.PaymentMethod;
import com.warranty.model.Codes.PaymentStatus;
import com.warranty.model.Entities.Part;
import com.warranty.model.Entities.RepairPart;
import com.warranty.model.Entities.Ticket;
import com.warranty.model.Entities.User;
import com.warranty.model.Entities.Warranty;
import com.warranty.model.Role;
import com.warranty.model.TicketStatus;
import com.warranty.util.Html;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Flow 1 – Tao ticket bao hanh, Support duyet & phan cong ky thuat vien.
 * Flow 2 – Ky thuat vien chan doan, dung linh kien, ban giao ca, hoan tat.
 * Sequence Customer – khach theo doi va xac nhan hoan thanh (CLOSED).
 */
public class TicketService {

    // ============================ FLOW 1 ============================

    /** Khach hang tu gui yeu cau qua web (serial phai thuoc tai khoan cua khach). */
    public String submitByCustomer(int customerId, String serial, String issue, boolean acceptPaidRepair)
            throws SQLException, BusinessException {
        return Tx.write(c -> {
            Warranty w = findWarranty(c, serial);
            if (w.customerId() != customerId) {
                throw new BusinessException("Serial này không thuộc sản phẩm bạn đã mua");
            }
            return create(c, w, null, Channel.WEB, issue, acceptPaidRepair);
        });
    }

    /** Customer support tao ticket thay khach (dien thoai / tai cua hang). */
    public String submitBySupport(int supportId, String serial, String issue, Channel channel,
            boolean acceptPaidRepair) throws SQLException, BusinessException {
        return Tx.write(c -> create(c, findWarranty(c, serial), supportId,
                channel == null ? Channel.STORE : channel, issue, acceptPaidRepair));
    }

    /** Buoc 2-4: tim bao hanh theo serial (tra cuu bao hanh). */
    public Warranty lookup(String serial) throws SQLException, BusinessException {
        return Tx.write(c -> findWarranty(c, serial));
    }

    private static Warranty findWarranty(Connection c, String serial) throws SQLException, BusinessException {
        String s = Tx.require(serial, "Vui lòng nhập số serial / mã bảo hành");
        OrderDAO.expireWarranties(c);
        Warranty w = OrderDAO.warrantyBySerial(c, s);
        if (w == null) {
            throw new BusinessException("Không tìm thấy bảo hành cho serial " + s);
        }
        return w;
    }

    private static String create(Connection c, Warranty w, Integer supportId, Channel channel, String issue,
            boolean acceptPaidRepair) throws SQLException, BusinessException {
        String desc = Tx.require(issue, "Vui lòng mô tả lỗi của sản phẩm");
        if (TicketDAO.hasOpenTicket(c, w.id())) {
            throw new BusinessException("Sản phẩm này đang có ticket chưa đóng");
        }
        boolean valid = w.isValidOn(LocalDate.now());
        // Buoc 13: bao hanh het han / khong hop le -> chi nhan khi dong y sua chua co phi
        if (!valid && !acceptPaidRepair) {
            throw new BusinessException("Bảo hành đã hết hạn (" + Html.date(w.endDate())
                    + "). Cần đồng ý sửa chữa có phí (repair fee) để tạo ticket.");
        }
        String code = Sql.nextCode(c, "tickets", "ticket_code", "TK");
        int id = TicketDAO.insert(c, code, w.id(), supportId, channel, desc);
        // Buoc 6-7: gui xac nhan ticket cho khach
        NotificationDAO.send(c, w.customerId(), id, "Đã tiếp nhận yêu cầu " + code,
                "Sản phẩm " + w.productName() + " (" + w.serialNumber() + ") – trạng thái: Mới. "
                + (valid ? "Trong thời hạn bảo hành." : "Ngoài bảo hành – sửa chữa có phí."));
        return code;
    }

    /** Buoc 9-12: Support duyet ticket NEW, phan cong ky thuat vien -> ASSIGNED. */
    public void assign(int ticketId, int supportId, int technicianId) throws SQLException, BusinessException {
        Tx.write(c -> {
            Ticket t = lock(c, ticketId);
            requireStatus(t, TicketStatus.NEW);
            User tech = activeTechnician(c, technicianId);
            TicketDAO.assign(c, ticketId, supportId, technicianId);
            NotificationDAO.send(c, t.warranty().customerId(), ticketId, "Ticket " + t.ticketCode() + " đã được phân công",
                    "Kỹ thuật viên " + tech.fullName() + " sẽ xử lý yêu cầu của bạn.");
            return null;
        });
    }

    // ============================ FLOW 2 ============================

    /** Buoc 1-5: ky thuat nhan viec, gui chan doan -> INSERT repair_log, IN_REPAIR. */
    public void accept(int ticketId, int technicianId, String diagnosis) throws SQLException, BusinessException {
        String d = Tx.require(diagnosis, "Nhập kết quả chẩn đoán");
        Tx.write(c -> {
            Ticket t = ownedBy(lock(c, ticketId), technicianId);
            requireStatus(t, TicketStatus.ASSIGNED);
            TicketDAO.insertLog(c, ticketId, technicianId, d);
            TicketDAO.setStatus(c, ticketId, TicketStatus.IN_REPAIR);
            NotificationDAO.send(c, t.warranty().customerId(), ticketId, "Ticket " + t.ticketCode() + " đang được xử lý",
                    "Kỹ thuật viên đã nhận việc. Chẩn đoán: " + d);
            return null;
        });
    }

    /**
     * Buoc 6-11: yeu cau linh kien thay the.
     * Con hang -> INSERT repair_parts + tru stock_qty (giu hang, cho kho giao).
     * Het hang -> bao loi de kho lap purchase order (Flow 4).
     */
    public String requestPart(int ticketId, int technicianId, int partId, int qty)
            throws SQLException, BusinessException {
        if (qty <= 0) {
            throw new BusinessException("Số lượng linh kiện phải lớn hơn 0");
        }
        return Tx.write(c -> {
            Ticket t = ownedBy(lock(c, ticketId), technicianId);
            requireStatus(t, TicketStatus.IN_REPAIR);
            Part p = CatalogDAO.partForUpdate(c, partId);
            if (p == null) {
                throw new BusinessException("Linh kiện không tồn tại");
            }
            if (p.stockQty() < qty) {
                throw new BusinessException("Hết hàng: " + p.name() + " chỉ còn " + p.stockQty()
                        + ". Đã báo kho – linh kiện nằm trong danh sách cần nhập (purchase order).");
            }
            int logId = TicketDAO.latestLogId(c, ticketId);
            RepairPart existing = TicketDAO.repairPart(c, logId, partId);
            if (existing == null) {
                TicketDAO.insertRepairPart(c, logId, partId, qty);
            } else if (!existing.delivered()) {
                TicketDAO.addRepairPartQty(c, logId, partId, qty);
            } else {
                throw new BusinessException("Linh kiện này đã được kho giao cho lần sửa hiện tại");
            }
            CatalogDAO.addPartStock(c, partId, -qty);
            return "Đã giữ " + qty + " × " + p.name() + " – chờ kho giao linh kiện";
        });
    }

    /** Buoc 12-14: hoan tat sua chua + ghi chu ban giao ca -> COMPLETED, bao khach den nhan. */
    public void complete(int ticketId, int technicianId, String result, String shiftNote, BigDecimal fee)
            throws SQLException, BusinessException {
        String r = Tx.require(result, "Nhập kết quả sửa chữa");
        Tx.write(c -> {
            Ticket t = ownedBy(lock(c, ticketId), technicianId);
            requireStatus(t, TicketStatus.IN_REPAIR);
            boolean pending = TicketDAO.partsOfTicket(c, ticketId).stream().anyMatch(p -> !p.delivered());
            if (pending) {
                throw new BusinessException("Còn linh kiện chưa được kho giao – không thể hoàn tất");
            }
            BigDecimal repairFee = BigDecimal.ZERO;
            if (!t.inWarranty()) {
                if (fee == null || fee.signum() < 0) {
                    throw new BusinessException("Sản phẩm ngoài bảo hành – nhập phí sửa chữa hợp lệ");
                }
                repairFee = fee;
            }
            TicketDAO.finishLog(c, TicketDAO.latestLogId(c, ticketId), r, Tx.trimOrNull(shiftNote));
            TicketDAO.setRepairFee(c, ticketId, repairFee);
            TicketDAO.setStatus(c, ticketId, TicketStatus.COMPLETED);
            NotificationDAO.send(c, t.warranty().customerId(), ticketId, "Thiết bị đã sửa xong – " + t.ticketCode(),
                    "Kết quả: " + r + ". " + (repairFee.signum() > 0
                            ? "Phí sửa chữa: " + Html.money(repairFee) + ". " : "Miễn phí bảo hành. ")
                    + "Mời quý khách đến nhận thiết bị.");
            return null;
        });
    }

    /** Ban giao ticket cho ky thuat vien khac (ticket_handovers). */
    public void handover(int ticketId, int fromTechnicianId, int toTechnicianId, String note)
            throws SQLException, BusinessException {
        if (fromTechnicianId == toTechnicianId) {
            throw new BusinessException("Chọn kỹ thuật viên khác để bàn giao");
        }
        Tx.write(c -> {
            Ticket t = ownedBy(lock(c, ticketId), fromTechnicianId);
            if (t.status() != TicketStatus.ASSIGNED && t.status() != TicketStatus.IN_REPAIR) {
                throw new BusinessException("Chỉ bàn giao ticket đang được xử lý");
            }
            activeTechnician(c, toTechnicianId);
            TicketDAO.insertHandover(c, ticketId, fromTechnicianId, toTechnicianId, Tx.trimOrNull(note));
            TicketDAO.setTechnician(c, ticketId, toTechnicianId);
            Integer logId = TicketDAO.latestLogId(c, ticketId);
            if (logId != null && !Tx.blank(note)) {
                TicketDAO.setShiftNote(c, logId, note.trim());
            }
            return null;
        });
    }

    // ============================ THANH TOAN & DONG TICKET ============================

    /** Thanh toan phi sua chua (payments.ticket_id) qua cong thanh toan gia lap. */
    public OrderService.PaymentResult payRepairFee(int ticketId, User actor, PaymentMethod method,
            boolean simulateFail) throws SQLException, BusinessException {
        if (method == null) {
            throw new BusinessException("Chọn phương thức thanh toán");
        }
        return Tx.write(c -> {
            Ticket t = lock(c, ticketId);
            checkCustomer(t, actor);
            requireStatus(t, TicketStatus.COMPLETED);
            if (t.repairFee().signum() <= 0 || isPaid(c, ticketId)) {
                throw new BusinessException("Ticket không có phí cần thanh toán");
            }
            if (simulateFail) {
                OrderDAO.insertPayment(c, null, ticketId, t.repairFee(), method, PaymentStatus.FAILED, null);
                return new OrderService.PaymentResult(false, "Thanh toán bị từ chối – vui lòng thử lại");
            }
            String ref = OrderDAO.newTransactionRef();
            OrderDAO.insertPayment(c, null, ticketId, t.repairFee(), method, PaymentStatus.PAID, ref);
            NotificationDAO.send(c, t.warranty().customerId(), ticketId, "Đã thanh toán phí sửa chữa " + t.ticketCode(),
                    Html.money(t.repairFee()) + " – " + method.getLabel() + ", mã GD " + ref);
            return new OrderService.PaymentResult(true, "Đã thanh toán phí sửa chữa – mã giao dịch " + ref);
        });
    }

    /** Khach xac nhan hoan thanh (hoac support dong khi khach nhan may tai quay) -> CLOSED. */
    public void close(int ticketId, User actor) throws SQLException, BusinessException {
        Tx.write(c -> {
            Ticket t = lock(c, ticketId);
            checkCustomer(t, actor);
            requireStatus(t, TicketStatus.COMPLETED);
            if (t.repairFee().signum() > 0 && !isPaid(c, ticketId)) {
                throw new BusinessException("Cần thanh toán phí sửa chữa " + Html.money(t.repairFee())
                        + " trước khi đóng ticket");
            }
            TicketDAO.close(c, ticketId);
            NotificationDAO.send(c, t.warranty().customerId(), ticketId, "Ticket " + t.ticketCode() + " đã đóng",
                    "Cảm ơn quý khách đã sử dụng dịch vụ bảo hành.");
            return null;
        });
    }

    public static boolean isPaid(Connection c, int ticketId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM payments WHERE ticket_id = ? AND status = ?",
                ticketId, PaymentStatus.PAID) > 0;
    }

    // ============================ KIEM TRA ============================
    private static Ticket lock(Connection c, int ticketId) throws SQLException, BusinessException {
        Ticket t = TicketDAO.findForUpdate(c, ticketId);
        if (t == null) {
            throw new BusinessException("Ticket không tồn tại");
        }
        return t;
    }

    private static void requireStatus(Ticket t, TicketStatus expected) throws BusinessException {
        if (t.status() != expected) {
            throw new BusinessException("Ticket đang ở trạng thái \"" + t.status().getLabel()
                    + "\", cần \"" + expected.getLabel() + "\"");
        }
    }

    private static Ticket ownedBy(Ticket t, int technicianId) throws BusinessException {
        if (t.technicianId() == null || t.technicianId() != technicianId) {
            throw new BusinessException("Ticket không được giao cho bạn");
        }
        return t;
    }

    private static void checkCustomer(Ticket t, User actor) throws BusinessException {
        if (actor.role() == Role.CUSTOMER && actor.id() != t.warranty().customerId()) {
            throw new BusinessException("Ticket không thuộc tài khoản của bạn");
        }
    }

    private static User activeTechnician(Connection c, int id) throws SQLException, BusinessException {
        User u = UserDAO.findById(c, id);
        if (u == null || u.role() != Role.TECHNICIAN || !u.active()) {
            throw new BusinessException("Kỹ thuật viên không hợp lệ");
        }
        return u;
    }
}
