# Sales & Warranty Management System

Java Web MVC (Servlet + JSP + JDBC), NetBeans Ant project, Apache Tomcat 10.1, SQL Server 2022. Không dùng Maven.
Thiết kế theo sơ đồ draw.io: Context diagram, Use case, ERD / **physicalv2**, Flow 1–4, sequence Login / Authentication / Customer / Tech.

## Cấu trúc MVC
```
src/java/com/warranty/
  model/       Entities (record theo bảng physicalv2), Role, TicketStatus, Codes (các enum trạng thái)
  dao/         DBContext, Sql (tiện ích JDBC), UserDAO, CatalogDAO, OrderDAO, TicketDAO,
               PurchaseOrderDAO, NotificationDAO, ReportDAO
  service/     UserService, CatalogService, OrderService (Flow 3), TicketService (Flow 1, 2),
               InventoryService (Flow 4), Tx (transaction)
  controller/  AuthServlet, LookupServlet, AdminServlet, OrderServlet, TicketServlet,
               InventoryServlet, CustomerServlet
  util/        AuthFilter (phân quyền theo role), EncodingFilter, PasswordUtil, Html
web/WEB-INF/views/  common/ admin/ support/ inventory/ customer/   (JSP)
database/warranty_db.sql   23 bảng theo physicalv2 + dữ liệu mẫu
```

## Vai trò & chức năng
| Vai trò | Chức năng |
|---|---|
| Admin | Báo cáo thống kê; quản lý tài khoản nhân viên/khách (tạo, sửa, vô hiệu hóa, xóa); sản phẩm + chính sách bảo hành (`warranty_months`), ngừng kinh doanh; người bán; nhà cung cấp + mặt hàng cung cấp |
| Customer Support | Flow 3: tạo đơn → thanh toán → sinh bảo hành; tạo ticket thay khách; duyệt & phân công kỹ thuật viên; thu phí sửa chữa, đóng ticket tại quầy |
| Technician | Flow 2: nhận việc + chẩn đoán (repair_log), xin linh kiện, bàn giao ca (ticket_handovers), hoàn tất + phí sửa |
| Inventory Staff | Flow 4: báo cáo tồn thấp, lập PO gửi NCC, nhận hàng cộng kho; giao / từ chối linh kiện cho kỹ thuật; CRUD linh kiện; kiểm kê điều chỉnh tồn |
| Customer | Xem sản phẩm & bảo hành, gửi yêu cầu bảo hành, theo dõi, thanh toán phí, xác nhận hoàn thành, nhận thông báo |
| Khách vãng lai | Tra cứu bảo hành theo serial (`/lookup`) |

## Trạng thái
- Ticket: `NEW → ASSIGNED → IN_REPAIR → COMPLETED → CLOSED`
- Đơn hàng: `PENDING → PAID | CANCELLED`; thanh toán: `PAID | FAILED`
- Bảo hành: `ACTIVE | EXPIRED | VOID`; PO: `SENT → RECEIVED`

## Điểm điều chỉnh so với sơ đồ (do web MVC, không dùng Maven)
- **bcrypt → PBKDF2-HmacSHA256** (có sẵn trong JDK) cho `password_hash`.
- **Access/refresh token → HttpSession** + `AuthFilter` kiểm tra đăng nhập & role cho mỗi request.
- **Email → bảng `notifications`** (thông báo trong hệ thống cho khách hàng).
- **Payment gateway → giả lập** (ô "Giả lập giao dịch bị từ chối" để thử nhánh thất bại).
- Serial bảo hành sinh tự động khi thanh toán: `SKU-yyMMddNNN-n`.
- Đánh giá (rating) ở sequence Customer không có cột trong physicalv2 nên chưa lưu.

## Cài đặt & chạy
1. Bật SQL Server (PowerShell **Run as administrator**): `Start-Service MSSQLSERVER`
2. Tạo CSDL (xóa và tạo lại toàn bộ dữ liệu mẫu):
   `sqlcmd -S localhost -E -f 65001 -i database\warranty_db.sql`
3. Driver `mssql-jdbc-12.8.1.jre11.jar` đã có trong `web/WEB-INF/lib/`.
4. NetBeans: **File → Open Project** → `WarrantyManagement`. Nếu báo thiếu server:
   chuột phải project → **Resolve Problems** → chọn Apache Tomcat 10.1.
5. **Run** → http://localhost:8080/WarrantyManagement/

## Tài khoản mẫu (mật khẩu chung: `123456`)
| Email | Vai trò |
|---|---|
| admin@shop.vn | Admin |
| support@shop.vn | Customer Support |
| tech1@shop.vn, tech2@shop.vn | Technician |
| kho@shop.vn | Inventory Staff |
| an@gmail.com, binh@gmail.com, cuong@gmail.com | Customer |

Serial mẫu: `SN-VGA-0001` (còn hạn, đang sửa), `SN-CPU-0001` / `SN-SSD-0001` (còn hạn),
`SN-RAM-0001` / `SN-PSU-0001` (hết hạn → sửa có phí).
