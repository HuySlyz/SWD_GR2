package com.warranty.service;

import com.warranty.dao.DBContext;
import com.warranty.dao.UserDAO;
import com.warranty.model.Entities.User;
import com.warranty.model.Role;
import com.warranty.util.PasswordUtil;
import java.sql.SQLException;

/** Dang nhap (sequence "1. Login") va quan ly tai khoan (use case Employee / Customers Management). */
public class UserService {

    private static final int MIN_PASSWORD = 6;

    /** Tim user theo email, so khop mat khau bam, chan tai khoan bi vo hieu hoa. */
    public User login(String email, String password) throws SQLException, BusinessException {
        if (Tx.blank(email) || Tx.blank(password)) {
            throw new BusinessException("Vui lòng nhập email và mật khẩu");
        }
        User user = Tx.read(c -> {
            User u = UserDAO.findByEmail(c, email.trim());
            boolean ok = u != null && PasswordUtil.verify(password, UserDAO.passwordHash(c, u.id()));
            return ok ? u : null;
        });
        if (user == null) {
            throw new BusinessException("Sai email hoặc mật khẩu");
        }
        if (!user.active()) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa, vui lòng liên hệ quản trị viên");
        }
        return user;
    }

    public int create(Role role, String username, String password, String fullName, String email, String phone,
            String address) throws SQLException, BusinessException {
        if (role == null) {
            throw new BusinessException("Chọn vai trò cho tài khoản");
        }
        String un = Tx.require(username, "Tên đăng nhập là bắt buộc");
        if (!un.matches("[A-Za-z0-9_.]{3,50}")) {
            throw new BusinessException("Tên đăng nhập 3-50 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới");
        }
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new BusinessException("Mật khẩu tối thiểu " + MIN_PASSWORD + " ký tự");
        }
        String name = Tx.require(fullName, "Họ tên là bắt buộc");
        Tx.checkEmail(email);
        Tx.checkPhone(phone, role == Role.CUSTOMER);
        return Tx.write(c -> {
            if (UserDAO.existsUsernameOrEmail(c, un, email.trim(), 0)) {
                throw new BusinessException("Tên đăng nhập hoặc email đã tồn tại");
            }
            int id = UserDAO.insert(c, role, un, PasswordUtil.hash(password), name, email.trim(), Tx.trimOrNull(phone));
            UserDAO.insertSubtype(c, id, role, Tx.trimOrNull(address));
            return id;
        });
    }

    public void update(int id, String fullName, String email, String phone, String address, String newPassword)
            throws SQLException, BusinessException {
        String name = Tx.require(fullName, "Họ tên là bắt buộc");
        Tx.checkEmail(email);
        Tx.write(c -> {
            User u = UserDAO.findById(c, id);
            if (u == null) {
                throw new BusinessException("Tài khoản không tồn tại");
            }
            Tx.checkPhone(phone, u.role() == Role.CUSTOMER);
            if (UserDAO.existsUsernameOrEmail(c, u.username(), email.trim(), id)) {
                throw new BusinessException("Email đã được dùng cho tài khoản khác");
            }
            UserDAO.update(c, id, name, email.trim(), Tx.trimOrNull(phone));
            if (u.role() == Role.CUSTOMER) {
                UserDAO.updateAddress(c, id, Tx.trimOrNull(address));
            }
            if (!Tx.blank(newPassword)) {
                if (newPassword.length() < MIN_PASSWORD) {
                    throw new BusinessException("Mật khẩu tối thiểu " + MIN_PASSWORD + " ký tự");
                }
                UserDAO.setPassword(c, id, PasswordUtil.hash(newPassword));
            }
            return null;
        });
    }

    /** Use case "Deactivate Account". */
    public void setActive(int id, boolean active, int currentUserId) throws SQLException, BusinessException {
        if (id == currentUserId && !active) {
            throw new BusinessException("Không thể tự vô hiệu hóa tài khoản đang đăng nhập");
        }
        Tx.write(c -> {
            UserDAO.setActive(c, id, active);
            return null;
        });
    }

    /** Use case "Delete Account": chi xoa duoc khi chua phat sinh nghiep vu. */
    public void delete(int id, int currentUserId) throws SQLException, BusinessException {
        if (id == currentUserId) {
            throw new BusinessException("Không thể xóa tài khoản đang đăng nhập");
        }
        try {
            Tx.write(c -> {
                User u = UserDAO.findById(c, id);
                if (u == null) {
                    throw new BusinessException("Tài khoản không tồn tại");
                }
                UserDAO.delete(c, id, u.role());
                return null;
            });
        } catch (SQLException e) {
            if (e.getErrorCode() == DBContext.FK_VIOLATION) {
                throw new BusinessException("Tài khoản đã phát sinh dữ liệu (đơn hàng, ticket...). Hãy vô hiệu hóa thay vì xóa.");
            }
            throw e;
        }
    }
}
