package com.warranty.service;

import com.warranty.dao.DBContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

/** Mo ket noi / transaction cho tang service va controller. */
public final class Tx {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException, BusinessException;
    }

    @FunctionalInterface
    public interface ReadWork<T> {
        T run(Connection c) throws SQLException;
    }

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{9,10}$");

    private Tx() {
    }

    /** Doc du lieu (auto-commit). */
    public static <T> T read(ReadWork<T> work) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            return work.run(c);
        }
    }

    /** Ghi du lieu trong 1 transaction: loi nghiep vu hoac SQL deu rollback. */
    public static <T> T write(Work<T> work) throws SQLException, BusinessException {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | BusinessException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // ---------------- kiem tra du lieu dung chung ----------------
    static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    static String require(String value, String message) throws BusinessException {
        if (blank(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    static String trimOrNull(String s) {
        return blank(s) ? null : s.trim();
    }

    static void checkEmail(String email) throws BusinessException {
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw new BusinessException("Email không hợp lệ");
        }
    }

    static void checkPhone(String phone, boolean required) throws BusinessException {
        if (blank(phone)) {
            if (required) {
                throw new BusinessException("Số điện thoại là bắt buộc");
            }
            return;
        }
        if (!PHONE.matcher(phone.trim()).matches()) {
            throw new BusinessException("Số điện thoại không hợp lệ (bắt đầu bằng 0, 10-11 số)");
        }
    }
}
