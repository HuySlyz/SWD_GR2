package com.warranty.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Ham tien ich JDBC dung chung cho cac DAO (giam lap code PreparedStatement/ResultSet). */
public final class Sql {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private Sql() {
    }

    public static <T> List<T> list(Connection c, String sql, RowMapper<T> mapper, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            List<T> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
            }
            return out;
        }
    }

    public static <T> T one(Connection c, String sql, RowMapper<T> mapper, Object... args) throws SQLException {
        List<T> l = list(c, sql, mapper, args);
        return l.isEmpty() ? null : l.get(0);
    }

    public static int exec(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    /** INSERT va tra ve khoa IDENTITY vua sinh. */
    public static int insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static int count(Connection c, String sql, Object... args) throws SQLException {
        Integer n = one(c, sql, rs -> rs.getInt(1), args);
        return n == null ? 0 : n;
    }

    public static BigDecimal sum(Connection c, String sql, Object... args) throws SQLException {
        BigDecimal v = one(c, sql, rs -> rs.getBigDecimal(1), args);
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Sinh ma nghiep vu dang PREFIX + yyMMdd + "-" + so thu tu trong ngay, vd TK261103-002.
     * Goi trong transaction de tranh trung.
     */
    public static String nextCode(Connection c, String table, String column, String prefix) throws SQLException {
        String day = prefix + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        int n = count(c, "SELECT COUNT(*) FROM " + table + " WITH (UPDLOCK, HOLDLOCK) WHERE " + column + " LIKE ?",
                day + "%");
        return day + String.format("%03d", n + 1);
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            int idx = i + 1;
            if (a == null) {
                ps.setNull(idx, Types.VARCHAR);
            } else if (a instanceof LocalDate) {
                ps.setDate(idx, Date.valueOf((LocalDate) a));
            } else if (a instanceof LocalDateTime) {
                ps.setTimestamp(idx, Timestamp.valueOf((LocalDateTime) a));
            } else if (a instanceof Enum<?>) {
                ps.setString(idx, ((Enum<?>) a).name());
            } else if (a instanceof Boolean) {
                ps.setBoolean(idx, (Boolean) a);
            } else {
                ps.setObject(idx, a);
            }
        }
    }

    // --------- doc cot co the NULL ---------
    public static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static LocalDate date(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    public static LocalDateTime dateTime(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    public static <E extends Enum<E>> E enumOf(Class<E> type, ResultSet rs, String col) throws SQLException {
        String v = rs.getString(col);
        return v == null ? null : Enum.valueOf(type, v);
    }

    /** Chuoi LIKE an toan cho tu khoa tim kiem. */
    public static String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim()) + "%";
    }
}
