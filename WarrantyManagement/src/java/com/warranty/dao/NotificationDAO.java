package com.warranty.dao;

import com.warranty.model.Entities.Notification;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** notifications: thay the kenh Email trong so do (luu thong bao trong he thong cho khach hang). */
public final class NotificationDAO {

    private NotificationDAO() {
    }

    public static void send(Connection c, int customerId, Integer ticketId, String title, String content)
            throws SQLException {
        Sql.exec(c, "INSERT INTO notifications(customer_id, ticket_id, title, content) VALUES (?, ?, ?, ?)",
                customerId, ticketId, title, content);
    }

    public static List<Notification> ofCustomer(Connection c, int customerId) throws SQLException {
        return Sql.list(c, "SELECT * FROM notifications WHERE customer_id = ? ORDER BY sent_at DESC, id DESC",
                rs -> new Notification(rs.getInt("id"), rs.getInt("customer_id"), Sql.intOrNull(rs, "ticket_id"),
                        rs.getString("title"), rs.getString("content"), rs.getBoolean("is_read"),
                        Sql.dateTime(rs, "sent_at")), customerId);
    }

    public static int unreadCount(Connection c, int customerId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM notifications WHERE customer_id = ? AND is_read = 0", customerId);
    }

    public static void markAllRead(Connection c, int customerId) throws SQLException {
        Sql.exec(c, "UPDATE notifications SET is_read = 1 WHERE customer_id = ? AND is_read = 0", customerId);
    }
}
