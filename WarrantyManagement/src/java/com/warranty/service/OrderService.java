package com.warranty.service;

import com.warranty.dao.CatalogDAO;
import com.warranty.dao.NotificationDAO;
import com.warranty.dao.OrderDAO;
import com.warranty.dao.Sql;
import com.warranty.dao.UserDAO;
import com.warranty.model.Codes.OrderStatus;
import com.warranty.model.Codes.PaymentMethod;
import com.warranty.model.Codes.PaymentStatus;
import com.warranty.model.Entities.Order;
import com.warranty.model.Entities.OrderItem;
import com.warranty.model.Entities.Product;
import com.warranty.model.Entities.User;
import com.warranty.model.Role;
import com.warranty.util.Html;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Flow 3 – Customer support tao don hang, thanh toan va kich hoat bao hanh.
 *  1-4. Tao order + order_items (PENDING) sau khi kiem tra ton kho
 *  5-11. Thanh toan thanh cong: payment PAID, order PAID, tru kho, tao warranties, gui bien nhan
 *  12-14. Thanh toan that bai: payment FAILED, order CANCELLED
 */
public class OrderService {

    /** Ket qua thanh toan (that bai van commit ban ghi payment FAILED). */
    public record PaymentResult(boolean success, String message) {
    }

    /** lines: productId -> so luong. Tra ve id don hang. */
    public int createOrder(int supportId, int customerId, Map<Integer, Integer> lines)
            throws SQLException, BusinessException {
        if (lines.isEmpty()) {
            throw new BusinessException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }
        return Tx.write(c -> {
            User cu = UserDAO.findById(c, customerId);
            if (cu == null || cu.role() != Role.CUSTOMER || !cu.active()) {
                throw new BusinessException("Khách hàng không hợp lệ");
            }
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> e : lines.entrySet()) {
                Product p = checkStock(CatalogDAO.product(c, e.getKey()), e.getValue());
                total = total.add(p.price().multiply(BigDecimal.valueOf(e.getValue())));
            }
            String code = Sql.nextCode(c, "orders", "order_code", "DH");
            int orderId = OrderDAO.insertOrder(c, code, customerId, supportId, total);
            for (Map.Entry<Integer, Integer> e : lines.entrySet()) {
                Product p = CatalogDAO.product(c, e.getKey());
                OrderDAO.insertItem(c, orderId, p.id(), e.getValue(), p.price());
            }
            return orderId;
        });
    }

    /** Gui yeu cau thanh toan den cong thanh toan (gia lap). simulateFail = cong tu choi giao dich. */
    public PaymentResult pay(int orderId, PaymentMethod method, boolean simulateFail)
            throws SQLException, BusinessException {
        if (method == null) {
            throw new BusinessException("Chọn phương thức thanh toán");
        }
        return Tx.write(c -> {
            Order o = OrderDAO.findForUpdate(c, orderId);
            if (o == null) {
                throw new BusinessException("Đơn hàng không tồn tại");
            }
            if (o.status() != OrderStatus.PENDING) {
                throw new BusinessException("Đơn hàng đã " + o.status().getLabel().toLowerCase());
            }
            if (simulateFail) {
                // Buoc 12-14: thanh toan bi tu choi -> ghi nhan FAILED, huy don
                OrderDAO.insertPayment(c, orderId, null, o.totalAmount(), method, PaymentStatus.FAILED, null);
                OrderDAO.setStatus(c, orderId, OrderStatus.CANCELLED);
                return new PaymentResult(false, "Thanh toán bị từ chối – đơn " + o.orderCode()
                        + " đã hủy. Vui lòng tạo đơn mới để thử lại.");
            }
            // Buoc 6-7: cong xac nhan -> payment PAID, order PAID, tru kho
            for (OrderItem i : OrderDAO.items(c, orderId)) {
                checkStock(CatalogDAO.productForUpdate(c, i.productId()), i.quantity());
                CatalogDAO.addProductStock(c, i.productId(), -i.quantity());
            }
            String ref = OrderDAO.newTransactionRef();
            OrderDAO.insertPayment(c, orderId, null, o.totalAmount(), method, PaymentStatus.PAID, ref);
            OrderDAO.setStatus(c, orderId, OrderStatus.PAID);

            // Buoc 8: moi don vi san pham co 1 serial + 1 bao hanh (end_date = start + warranty_months)
            LocalDate today = LocalDate.now();
            StringBuilder receipt = new StringBuilder("Đơn " + o.orderCode() + " – " + Html.money(o.totalAmount())
                    + " (" + method.getLabel() + ", mã GD " + ref + "). Bảo hành:");
            for (OrderItem i : OrderDAO.items(c, orderId)) {
                Product p = CatalogDAO.product(c, i.productId());
                for (int n = 1; n <= i.quantity(); n++) {
                    String serial = p.sku() + "-" + o.orderCode().substring(2).replace("-", "") + "-" + n;
                    LocalDate end = today.plusMonths(p.warrantyMonths());
                    OrderDAO.insertWarranty(c, orderId, p.id(), serial, today, end);
                    receipt.append(" ").append(serial).append(" đến ").append(Html.date(end)).append(";");
                }
            }
            // Buoc 9-10: gui bien nhan + thong tin bao hanh cho khach
            NotificationDAO.send(c, o.customerId(), null, "Biên nhận đơn hàng " + o.orderCode(), receipt.toString());
            return new PaymentResult(true, "Thanh toán thành công – mã giao dịch " + ref
                    + ". Đã kích hoạt bảo hành và gửi biên nhận cho khách.");
        });
    }

    public void cancel(int orderId) throws SQLException, BusinessException {
        Tx.write(c -> {
            Order o = OrderDAO.findForUpdate(c, orderId);
            if (o == null || o.status() != OrderStatus.PENDING) {
                throw new BusinessException("Chỉ hủy được đơn đang chờ thanh toán");
            }
            OrderDAO.setStatus(c, orderId, OrderStatus.CANCELLED);
            return null;
        });
    }

    /** Flow 3 buoc 2-3: kiem tra stock_qty. */
    private static Product checkStock(Product p, int qty) throws BusinessException {
        if (p == null || !p.active()) {
            throw new BusinessException("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        }
        if (qty <= 0) {
            throw new BusinessException("Số lượng phải lớn hơn 0");
        }
        if (p.stockQty() < qty) {
            throw new BusinessException("Không đủ hàng: " + p.name() + " còn " + p.stockQty());
        }
        return p;
    }
}
