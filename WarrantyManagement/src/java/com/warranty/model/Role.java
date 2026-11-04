package com.warranty.model;

/** Vai tro nguoi dung (bang roles.role_name). */
public enum Role {
    ADMIN("Quản trị viên", "/admin/dashboard"),
    SUPPORT("Customer Support", "/tickets"),
    TECHNICIAN("Kỹ thuật viên", "/tickets"),
    INVENTORY("Nhân viên kho", "/inventory/stock"),
    CUSTOMER("Khách hàng", "/customer/warranties");

    private final String label;
    private final String homePath;

    Role(String label, String homePath) {
        this.label = label;
        this.homePath = homePath;
    }

    public String getLabel() {
        return label;
    }

    /** Trang dashboard theo role sau khi dang nhap. */
    public String getHomePath() {
        return homePath;
    }

    public boolean isStaff() {
        return this != CUSTOMER;
    }

    /** Bang chuyen biet hoa ung voi role. */
    public String subtypeTable() {
        switch (this) {
            case ADMIN: return "admins";
            case SUPPORT: return "support_staff";
            case TECHNICIAN: return "technicians";
            case INVENTORY: return "inventory_staff";
            default: return "customers";
        }
    }
}
