package com.warranty.dao;

import com.warranty.model.Codes.Channel;
import com.warranty.model.Entities.Handover;
import com.warranty.model.Entities.RepairLog;
import com.warranty.model.Entities.RepairPart;
import com.warranty.model.Entities.Ticket;
import com.warranty.model.TicketStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** tickets, repair_logs, ticket_handovers, repair_parts. */
public final class TicketDAO {

    private static final String SELECT = "SELECT t.*, cs.full_name AS created_by_name, su.full_name AS support_name,"
            + " te.full_name AS technician_name, " + OrderDAO.WARRANTY_COLUMNS
            + " FROM tickets t JOIN warranties w ON w.id = t.warranty_id" + OrderDAO.WARRANTY_JOINS
            + " LEFT JOIN users cs ON cs.id = t.created_by_support_id"
            + " LEFT JOIN users su ON su.id = t.support_id"
            + " LEFT JOIN users te ON te.id = t.technician_id";

    private static final String PART_SELECT = "SELECT rp.*, pa.part_code, pa.name AS part_name, ib.full_name AS issued_name,"
            + " rl.ticket_id, t.ticket_code, te.full_name AS technician_name, rl.created_at AS requested_at"
            + " FROM repair_parts rp JOIN parts pa ON pa.id = rp.part_id JOIN repair_logs rl ON rl.id = rp.repair_log_id"
            + " JOIN tickets t ON t.id = rl.ticket_id JOIN users te ON te.id = rl.technician_id"
            + " LEFT JOIN users ib ON ib.id = rp.issued_by";

    private TicketDAO() {
    }

    // ============================ TICKET ============================
    public static int insert(Connection c, String code, int warrantyId, Integer createdBySupportId, Channel channel,
            String issue) throws SQLException {
        return Sql.insert(c, "INSERT INTO tickets(ticket_code, warranty_id, created_by_support_id, channel,"
                + " issue_description, status) VALUES (?, ?, ?, ?, ?, ?)",
                code, warrantyId, createdBySupportId, channel, issue, TicketStatus.NEW);
    }

    public static Ticket find(Connection c, int id) throws SQLException {
        return Sql.one(c, SELECT + " WHERE t.id = ?", TicketDAO::map, id);
    }

    public static Ticket findForUpdate(Connection c, int id) throws SQLException {
        return Sql.one(c, SELECT.replace("FROM tickets t", "FROM tickets t WITH (UPDLOCK)") + " WHERE t.id = ?",
                TicketDAO::map, id);
    }

    /** Loc ticket. technicianId/customerId = null: khong loc. */
    public static List<Ticket> search(Connection c, TicketStatus status, Integer technicianId, Integer customerId,
            String keyword) throws SQLException {
        String kw = Sql.like(keyword);
        return Sql.list(c, SELECT + " WHERE (? IS NULL OR t.status = ?)"
                + " AND (? IS NULL OR t.technician_id = ?) AND (? IS NULL OR o.customer_id = ?)"
                + " AND (t.ticket_code LIKE ? OR w.serial_number LIKE ? OR cu.full_name LIKE ? OR cu.phone LIKE ?)"
                + " ORDER BY CASE t.status WHEN 'CLOSED' THEN 1 ELSE 0 END, t.created_at DESC", TicketDAO::map,
                status, status, technicianId, technicianId, customerId, customerId, kw, kw, kw, kw);
    }

    public static List<Ticket> ofWarranty(Connection c, int warrantyId) throws SQLException {
        return Sql.list(c, SELECT + " WHERE t.warranty_id = ? ORDER BY t.created_at DESC", TicketDAO::map, warrantyId);
    }

    public static boolean hasOpenTicket(Connection c, int warrantyId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM tickets WHERE warranty_id = ? AND status <> ?",
                warrantyId, TicketStatus.CLOSED) > 0;
    }

    public static void assign(Connection c, int ticketId, int supportId, int technicianId) throws SQLException {
        Sql.exec(c, "UPDATE tickets SET support_id = ?, technician_id = ?, status = ? WHERE id = ?",
                supportId, technicianId, TicketStatus.ASSIGNED, ticketId);
    }

    public static void setStatus(Connection c, int ticketId, TicketStatus status) throws SQLException {
        Sql.exec(c, "UPDATE tickets SET status = ? WHERE id = ?", status, ticketId);
    }

    public static void setRepairFee(Connection c, int ticketId, BigDecimal fee) throws SQLException {
        Sql.exec(c, "UPDATE tickets SET repair_fee = ? WHERE id = ?", fee, ticketId);
    }

    public static void setTechnician(Connection c, int ticketId, int technicianId) throws SQLException {
        Sql.exec(c, "UPDATE tickets SET technician_id = ? WHERE id = ?", technicianId, ticketId);
    }

    public static void close(Connection c, int ticketId) throws SQLException {
        Sql.exec(c, "UPDATE tickets SET status = ?, closed_at = ? WHERE id = ?",
                TicketStatus.CLOSED, LocalDateTime.now(), ticketId);
    }

    // ============================ REPAIR LOG ============================
    public static int insertLog(Connection c, int ticketId, int technicianId, String diagnosis) throws SQLException {
        return Sql.insert(c, "INSERT INTO repair_logs(ticket_id, technician_id, diagnosis) VALUES (?, ?, ?)",
                ticketId, technicianId, diagnosis);
    }

    public static void finishLog(Connection c, int logId, String result, String shiftNote) throws SQLException {
        Sql.exec(c, "UPDATE repair_logs SET repair_result = ?, shift_note = ? WHERE id = ?", result, shiftNote, logId);
    }

    public static void setShiftNote(Connection c, int logId, String shiftNote) throws SQLException {
        Sql.exec(c, "UPDATE repair_logs SET shift_note = ? WHERE id = ?", shiftNote, logId);
    }

    public static List<RepairLog> logs(Connection c, int ticketId) throws SQLException {
        return Sql.list(c, "SELECT rl.*, u.full_name FROM repair_logs rl JOIN users u ON u.id = rl.technician_id"
                + " WHERE rl.ticket_id = ? ORDER BY rl.created_at", rs -> new RepairLog(rs.getInt("id"),
                rs.getInt("ticket_id"), rs.getInt("technician_id"), rs.getString("full_name"),
                rs.getString("diagnosis"), rs.getString("repair_result"), rs.getString("shift_note"),
                Sql.dateTime(rs, "created_at")), ticketId);
    }

    /** Repair log moi nhat cua ticket (lan sua hien tai). */
    public static Integer latestLogId(Connection c, int ticketId) throws SQLException {
        return Sql.one(c, "SELECT TOP 1 id FROM repair_logs WHERE ticket_id = ? ORDER BY created_at DESC, id DESC",
                rs -> rs.getInt(1), ticketId);
    }

    // ============================ HANDOVER ============================
    public static void insertHandover(Connection c, int ticketId, int fromTech, int toTech, String note)
            throws SQLException {
        Sql.exec(c, "INSERT INTO ticket_handovers(ticket_id, from_technician_id, to_technician_id, note)"
                + " VALUES (?, ?, ?, ?)", ticketId, fromTech, toTech, note);
    }

    public static List<Handover> handovers(Connection c, int ticketId) throws SQLException {
        return Sql.list(c, "SELECT h.*, f.full_name AS from_name, t.full_name AS to_name FROM ticket_handovers h"
                + " JOIN users f ON f.id = h.from_technician_id JOIN users t ON t.id = h.to_technician_id"
                + " WHERE h.ticket_id = ? ORDER BY h.handed_over_at", rs -> new Handover(rs.getInt("id"),
                rs.getInt("ticket_id"), rs.getString("from_name"), rs.getString("to_name"), rs.getString("note"),
                Sql.dateTime(rs, "handed_over_at")), ticketId);
    }

    // ============================ REPAIR PARTS ============================
    public static RepairPart repairPart(Connection c, int logId, int partId) throws SQLException {
        return Sql.one(c, PART_SELECT + " WHERE rp.repair_log_id = ? AND rp.part_id = ?", TicketDAO::mapPart,
                logId, partId);
    }

    public static void insertRepairPart(Connection c, int logId, int partId, int qty) throws SQLException {
        Sql.exec(c, "INSERT INTO repair_parts(repair_log_id, part_id, quantity) VALUES (?, ?, ?)", logId, partId, qty);
    }

    public static void addRepairPartQty(Connection c, int logId, int partId, int qty) throws SQLException {
        Sql.exec(c, "UPDATE repair_parts SET quantity = quantity + ? WHERE repair_log_id = ? AND part_id = ?",
                qty, logId, partId);
    }

    public static void issueRepairPart(Connection c, int logId, int partId, int staffId) throws SQLException {
        Sql.exec(c, "UPDATE repair_parts SET issued_by = ?, issued_at = ? WHERE repair_log_id = ? AND part_id = ?",
                staffId, LocalDateTime.now(), logId, partId);
    }

    public static void deleteRepairPart(Connection c, int logId, int partId) throws SQLException {
        Sql.exec(c, "DELETE FROM repair_parts WHERE repair_log_id = ? AND part_id = ?", logId, partId);
    }

    public static List<RepairPart> partsOfTicket(Connection c, int ticketId) throws SQLException {
        return Sql.list(c, PART_SELECT + " WHERE rl.ticket_id = ? ORDER BY rl.created_at", TicketDAO::mapPart, ticketId);
    }

    /** pendingOnly = linh kien da giu kho nhung chua giao cho ky thuat. */
    public static List<RepairPart> partRequests(Connection c, boolean pendingOnly) throws SQLException {
        return Sql.list(c, PART_SELECT + (pendingOnly ? " WHERE rp.issued_by IS NULL" : "")
                + " ORDER BY CASE WHEN rp.issued_by IS NULL THEN 0 ELSE 1 END, rl.created_at DESC", TicketDAO::mapPart);
    }

    // ============================ MAPPER ============================
    private static Ticket map(ResultSet rs) throws SQLException {
        return new Ticket(rs.getInt("id"), rs.getString("ticket_code"), OrderDAO.mapWarranty(rs),
                Sql.intOrNull(rs, "created_by_support_id"), rs.getString("created_by_name"),
                Sql.intOrNull(rs, "support_id"), rs.getString("support_name"), Sql.intOrNull(rs, "technician_id"),
                rs.getString("technician_name"), Channel.valueOf(rs.getString("channel")),
                rs.getString("issue_description"), TicketStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("repair_fee"), Sql.dateTime(rs, "created_at"), Sql.dateTime(rs, "closed_at"));
    }

    private static RepairPart mapPart(ResultSet rs) throws SQLException {
        return new RepairPart(rs.getInt("repair_log_id"), rs.getInt("part_id"), rs.getString("part_code"),
                rs.getString("part_name"), rs.getInt("quantity"), Sql.intOrNull(rs, "issued_by"),
                rs.getString("issued_name"), Sql.dateTime(rs, "issued_at"), rs.getInt("ticket_id"),
                rs.getString("ticket_code"), rs.getString("technician_name"), Sql.dateTime(rs, "requested_at"));
    }
}
