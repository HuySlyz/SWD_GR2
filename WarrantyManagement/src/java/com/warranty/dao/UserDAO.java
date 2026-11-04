package com.warranty.dao;

import com.warranty.model.Entities.User;
import com.warranty.model.Role;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** users, roles va cac bang chuyen biet hoa (customers, staff, ...). */
public final class UserDAO {

    private static final String SELECT = "SELECT u.id, r.role_name, u.username, u.full_name, u.email, u.phone,"
            + " u.is_active, u.created_at, cu.address"
            + " FROM users u JOIN roles r ON r.id = u.role_id"
            + " LEFT JOIN customers cu ON cu.user_id = u.id";

    private UserDAO() {
    }

    public static User findById(Connection c, int id) throws SQLException {
        return Sql.one(c, SELECT + " WHERE u.id = ?", UserDAO::map, id);
    }

    public static User findByEmail(Connection c, String email) throws SQLException {
        return Sql.one(c, SELECT + " WHERE u.email = ?", UserDAO::map, email);
    }

    public static String passwordHash(Connection c, int userId) throws SQLException {
        return Sql.one(c, "SELECT password_hash FROM users WHERE id = ?", rs -> rs.getString(1), userId);
    }

    /** role = null: tat ca. */
    public static List<User> search(Connection c, Role role, String keyword) throws SQLException {
        String kw = Sql.like(keyword);
        return Sql.list(c, SELECT + " WHERE (? IS NULL OR r.role_name = ?)"
                + " AND (u.full_name LIKE ? OR u.username LIKE ? OR u.email LIKE ? OR u.phone LIKE ?)"
                + " ORDER BY r.id, u.full_name", UserDAO::map,
                role, role, kw, kw, kw, kw);
    }

    /** Nguoi dung dang hoat dong theo role (dropdown chon ky thuat vien, khach hang...). */
    public static List<User> activeByRole(Connection c, Role role) throws SQLException {
        return Sql.list(c, SELECT + " WHERE r.role_name = ? AND u.is_active = 1 ORDER BY u.full_name",
                UserDAO::map, role);
    }

    public static boolean existsUsernameOrEmail(Connection c, String username, String email, int exceptId)
            throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM users WHERE (username = ? OR email = ?) AND id <> ?",
                username, email, exceptId) > 0;
    }

    public static int insert(Connection c, Role role, String username, String passwordHash, String fullName,
            String email, String phone) throws SQLException {
        return Sql.insert(c, "INSERT INTO users(role_id, username, password_hash, full_name, email, phone)"
                + " SELECT id, ?, ?, ?, ?, ? FROM roles WHERE role_name = ?",
                username, passwordHash, fullName, email, phone, role);
    }

    /** Tao ban ghi chuyen biet hoa: customers hoac staff + bang con. */
    public static void insertSubtype(Connection c, int userId, Role role, String address) throws SQLException {
        if (role == Role.CUSTOMER) {
            Sql.exec(c, "INSERT INTO customers(user_id, address) VALUES (?, ?)", userId, address);
        } else {
            Sql.exec(c, "INSERT INTO staff(user_id) VALUES (?)", userId);
            Sql.exec(c, "INSERT INTO " + role.subtypeTable() + "(user_id) VALUES (?)", userId);
        }
    }

    public static void update(Connection c, int id, String fullName, String email, String phone) throws SQLException {
        Sql.exec(c, "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?", fullName, email, phone, id);
    }

    public static void updateAddress(Connection c, int customerId, String address) throws SQLException {
        Sql.exec(c, "UPDATE customers SET address = ? WHERE user_id = ?", address, customerId);
    }

    public static void setPassword(Connection c, int id, String passwordHash) throws SQLException {
        Sql.exec(c, "UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    public static void setActive(Connection c, int id, boolean active) throws SQLException {
        Sql.exec(c, "UPDATE users SET is_active = ? WHERE id = ?", active, id);
    }

    /** Xoa han tai khoan (chi thanh cong khi chua phat sinh du lieu). */
    public static void delete(Connection c, int id, Role role) throws SQLException {
        if (role == Role.CUSTOMER) {
            Sql.exec(c, "DELETE FROM customers WHERE user_id = ?", id);
        } else {
            Sql.exec(c, "DELETE FROM " + role.subtypeTable() + " WHERE user_id = ?", id);
            Sql.exec(c, "DELETE FROM staff WHERE user_id = ?", id);
        }
        Sql.exec(c, "DELETE FROM users WHERE id = ?", id);
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), Role.valueOf(rs.getString("role_name")), rs.getString("username"),
                rs.getString("full_name"), rs.getString("email"), rs.getString("phone"),
                rs.getBoolean("is_active"), Sql.dateTime(rs, "created_at"), rs.getString("address"));
    }
}
