package com.warranty.model;

/**
 * Trang thai ticket theo sequence diagram:
 * NEW (khach/support tao) -> ASSIGNED (support phan cong) -> IN_REPAIR (ky thuat nhan viec, chan doan)
 * -> COMPLETED (sua xong, cho khach nhan) -> CLOSED (khach xac nhan hoan thanh).
 */
public enum TicketStatus {
    NEW("Mới"),
    ASSIGNED("Đã phân công"),
    IN_REPAIR("Đang sửa chữa"),
    COMPLETED("Đã sửa xong"),
    CLOSED("Đã đóng");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
