-- =====================================================================
--  Sales & Warranty Management System - SQL Server 2022
--  Thiet ke theo so do "physicalv2" (draw.io)
--  Chay:  sqlcmd -S localhost -E -f 65001 -i warranty_db.sql
-- =====================================================================
USE master;
GO
IF DB_ID(N'warranty_db') IS NOT NULL
BEGIN
    ALTER DATABASE warranty_db SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE warranty_db;
END
GO
CREATE DATABASE warranty_db;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'warranty_user')
    CREATE LOGIN warranty_user WITH PASSWORD = N'Warranty@123', CHECK_POLICY = OFF, DEFAULT_DATABASE = warranty_db;
GO
USE warranty_db;
GO
CREATE USER warranty_user FOR LOGIN warranty_user;
ALTER ROLE db_owner ADD MEMBER warranty_user;
GO

-- ============================ NGUOI DUNG ============================
CREATE TABLE roles (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    role_name    VARCHAR(30)   NOT NULL UNIQUE,   -- ADMIN, SUPPORT, TECHNICIAN, INVENTORY, CUSTOMER
    description  NVARCHAR(200) NULL
);

CREATE TABLE users (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    role_id        INT NOT NULL REFERENCES roles(id),
    username       VARCHAR(50)   NOT NULL UNIQUE,
    password_hash  VARCHAR(200)  NOT NULL,
    full_name      NVARCHAR(100) NOT NULL,
    email          VARCHAR(100)  NOT NULL UNIQUE,
    phone          VARCHAR(20)   NULL,
    is_active      BIT NOT NULL DEFAULT 1,
    created_at     DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

-- Chuyen biet hoa (is a)
CREATE TABLE customers (
    user_id  INT PRIMARY KEY REFERENCES users(id),
    address  NVARCHAR(255) NULL
);
CREATE TABLE staff           (user_id INT PRIMARY KEY REFERENCES users(id));
CREATE TABLE support_staff   (user_id INT PRIMARY KEY REFERENCES staff(user_id));
CREATE TABLE technicians     (user_id INT PRIMARY KEY REFERENCES staff(user_id));
CREATE TABLE inventory_staff (user_id INT PRIMARY KEY REFERENCES staff(user_id));
CREATE TABLE admins          (user_id INT PRIMARY KEY REFERENCES staff(user_id));

-- ============================ DANH MUC ============================
CREATE TABLE sellers (
    id     INT IDENTITY(1,1) PRIMARY KEY,
    name   NVARCHAR(150) NOT NULL,
    phone  VARCHAR(20)   NULL,
    email  VARCHAR(100)  NULL
);

CREATE TABLE suppliers (
    id       INT IDENTITY(1,1) PRIMARY KEY,
    name     NVARCHAR(150) NOT NULL,
    phone    VARCHAR(20)   NULL,
    email    VARCHAR(100)  NULL,
    address  NVARCHAR(255) NULL
);

CREATE TABLE products (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    seller_id        INT NULL REFERENCES sellers(id),
    sku              NVARCHAR(50)  NOT NULL UNIQUE,
    name             NVARCHAR(150) NOT NULL,
    price            DECIMAL(12,2) NOT NULL DEFAULT 0,
    warranty_months  INT NOT NULL DEFAULT 12,          -- chinh sach bao hanh
    stock_qty        INT NOT NULL DEFAULT 0,
    reorder_level    INT NOT NULL DEFAULT 5,
    is_active        BIT NOT NULL DEFAULT 1
);

CREATE TABLE parts (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    part_code      VARCHAR(50)   NOT NULL UNIQUE,
    name           NVARCHAR(150) NOT NULL,
    stock_qty      INT NOT NULL DEFAULT 0,
    reorder_level  INT NOT NULL DEFAULT 5,
    unit_cost      DECIMAL(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE supplier_products (
    supplier_id  INT NOT NULL REFERENCES suppliers(id),
    product_id   INT NOT NULL REFERENCES products(id),
    PRIMARY KEY (supplier_id, product_id)
);
CREATE TABLE supplier_parts (
    supplier_id  INT NOT NULL REFERENCES suppliers(id),
    part_id      INT NOT NULL REFERENCES parts(id),
    PRIMARY KEY (supplier_id, part_id)
);

-- ============================ BAN HANG ============================
CREATE TABLE orders (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    order_code    VARCHAR(30) NOT NULL UNIQUE,
    customer_id   INT NOT NULL REFERENCES customers(user_id),
    support_id    INT NULL REFERENCES support_staff(user_id),
    order_date    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    status        VARCHAR(20) NOT NULL,            -- PENDING, PAID, CANCELLED
    total_amount  DECIMAL(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE order_items (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    order_id    INT NOT NULL REFERENCES orders(id),
    product_id  INT NOT NULL REFERENCES products(id),
    quantity    INT NOT NULL CHECK (quantity > 0),
    unit_price  DECIMAL(12,2) NOT NULL,
    CONSTRAINT uk_order_product UNIQUE (order_id, product_id)
);

CREATE TABLE warranties (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    order_id       INT NOT NULL REFERENCES orders(id),
    product_id     INT NOT NULL REFERENCES products(id),
    serial_number  VARCHAR(60) NOT NULL UNIQUE,
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    status         VARCHAR(20) NOT NULL              -- ACTIVE, EXPIRED, VOID
);

-- ============================ BAO HANH / SUA CHUA ============================
CREATE TABLE tickets (
    id                     INT IDENTITY(1,1) PRIMARY KEY,
    ticket_code            VARCHAR(30) NOT NULL UNIQUE,
    warranty_id            INT NOT NULL REFERENCES warranties(id),
    created_by_support_id  INT NULL REFERENCES support_staff(user_id),  -- NULL = khach tu tao
    support_id             INT NULL REFERENCES support_staff(user_id),  -- nguoi duyet & phan cong
    technician_id          INT NULL REFERENCES technicians(user_id),
    channel                VARCHAR(20) NOT NULL,      -- WEB, PHONE, STORE
    issue_description      NVARCHAR(1000) NOT NULL,
    status                 VARCHAR(20) NOT NULL,      -- NEW, ASSIGNED, IN_REPAIR, COMPLETED, CLOSED
    repair_fee             DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at             DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    closed_at              DATETIME2 NULL
);

CREATE TABLE payments (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    order_id         INT NULL REFERENCES orders(id),    -- thanh toan don hang
    ticket_id        INT NULL REFERENCES tickets(id),   -- hoac thanh toan phi sua chua
    amount           DECIMAL(12,2) NOT NULL,
    method           VARCHAR(20) NOT NULL,              -- CASH, CARD, BANK_TRANSFER, E_WALLET
    status           VARCHAR(20) NOT NULL,              -- PAID, FAILED
    paid_at          DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    transaction_ref  VARCHAR(50) NULL
);

CREATE TABLE repair_logs (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    ticket_id      INT NOT NULL REFERENCES tickets(id),
    technician_id  INT NOT NULL REFERENCES technicians(user_id),
    diagnosis      NVARCHAR(1000) NOT NULL,
    repair_result  NVARCHAR(1000) NULL,
    shift_note     NVARCHAR(500)  NULL,
    created_at     DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE ticket_handovers (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    ticket_id           INT NOT NULL REFERENCES tickets(id),
    from_technician_id  INT NOT NULL REFERENCES technicians(user_id),
    to_technician_id    INT NOT NULL REFERENCES technicians(user_id),
    note                NVARCHAR(500) NULL,
    handed_over_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

-- Linh kien dung cho lan sua; issued_by NULL = da giu kho, cho kho giao
CREATE TABLE repair_parts (
    repair_log_id  INT NOT NULL REFERENCES repair_logs(id),
    part_id        INT NOT NULL REFERENCES parts(id),
    quantity       INT NOT NULL CHECK (quantity > 0),
    issued_by      INT NULL REFERENCES inventory_staff(user_id),
    issued_at      DATETIME2 NULL,
    PRIMARY KEY (repair_log_id, part_id)
);

-- ============================ NHAP HANG ============================
CREATE TABLE purchase_orders (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    po_code             VARCHAR(30) NOT NULL UNIQUE,
    supplier_id         INT NOT NULL REFERENCES suppliers(id),
    inventory_staff_id  INT NOT NULL REFERENCES inventory_staff(user_id),
    order_date          DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    status              VARCHAR(20) NOT NULL,      -- SENT, RECEIVED
    total_amount        DECIMAL(12,2) NOT NULL DEFAULT 0,
    received_by         INT NULL REFERENCES inventory_staff(user_id),
    received_at         DATETIME2 NULL
);
CREATE TABLE purchase_order_products (
    purchase_order_id  INT NOT NULL REFERENCES purchase_orders(id),
    product_id         INT NOT NULL REFERENCES products(id),
    quantity           INT NOT NULL CHECK (quantity > 0),
    unit_cost          DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (purchase_order_id, product_id)
);
CREATE TABLE purchase_order_parts (
    purchase_order_id  INT NOT NULL REFERENCES purchase_orders(id),
    part_id            INT NOT NULL REFERENCES parts(id),
    quantity           INT NOT NULL CHECK (quantity > 0),
    unit_cost          DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (purchase_order_id, part_id)
);

-- ============================ THONG BAO ============================
CREATE TABLE notifications (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    customer_id  INT NOT NULL REFERENCES customers(user_id),
    ticket_id    INT NULL REFERENCES tickets(id),
    title        NVARCHAR(200)  NOT NULL,
    content      NVARCHAR(1000) NOT NULL,
    is_read      BIT NOT NULL DEFAULT 0,
    sent_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- ============================ DU LIEU MAU ============================
-- Tat ca tai khoan mau dung mat khau: 123456
DECLARE @pw VARCHAR(200) = 'pbkdf2$65536$4Fvg2BR24DbcrSfl6M6OQw==$UcJOKs3zkqBmjUYfKIp5LzTxeaj++TwjNqgrgA4jcJg=';

INSERT INTO roles(role_name, description) VALUES
('ADMIN',      N'Quản trị hệ thống'),
('SUPPORT',    N'Chăm sóc khách hàng / bán hàng'),
('TECHNICIAN', N'Kỹ thuật viên'),
('INVENTORY',  N'Nhân viên kho'),
('CUSTOMER',   N'Khách hàng');

INSERT INTO users(role_id, username, password_hash, full_name, email, phone) VALUES
(1, 'admin',    @pw, N'Quản Trị Viên',   'admin@shop.vn',    '0900000001'),  -- 1
(2, 'support1', @pw, N'Phan Thu Hà',     'support@shop.vn',  '0900000002'),  -- 2
(3, 'tech1',    @pw, N'Phạm Minh Đức',   'tech1@shop.vn',    '0900000003'),  -- 3
(3, 'tech2',    @pw, N'Võ Thanh Tùng',   'tech2@shop.vn',    '0900000004'),  -- 4
(4, 'kho1',     @pw, N'Đỗ Văn Kho',      'kho@shop.vn',      '0900000005'),  -- 5
(5, 'an',       @pw, N'Nguyễn Văn An',   'an@gmail.com',     '0901234567'),  -- 6
(5, 'binh',     @pw, N'Trần Thị Bình',   'binh@gmail.com',   '0912345678'),  -- 7
(5, 'cuong',    @pw, N'Lê Hoàng Cường',  'cuong@gmail.com',  '0987654321');  -- 8

INSERT INTO staff(user_id) VALUES (1),(2),(3),(4),(5);
INSERT INTO admins(user_id) VALUES (1);
INSERT INTO support_staff(user_id) VALUES (2);
INSERT INTO technicians(user_id) VALUES (3),(4);
INSERT INTO inventory_staff(user_id) VALUES (5);
INSERT INTO customers(user_id, address) VALUES
(6, N'Q1, TP.HCM'), (7, N'Cầu Giấy, Hà Nội'), (8, N'Hải Châu, Đà Nẵng');

INSERT INTO sellers(name, phone, email) VALUES
(N'Cửa hàng Linh Kiện Số 1', '02838000001', 'sales@lk1.vn'),
(N'Chi nhánh Hà Nội',        '02438000002', 'hn@lk1.vn');

INSERT INTO suppliers(name, phone, email, address) VALUES
(N'Công ty Phân phối Viễn Sơn', '02839000001', 'po@vienson.vn', N'Q10, TP.HCM'),
(N'Digiworld',                  '02839000002', 'po@dgw.vn',     N'Q3, TP.HCM');

INSERT INTO products(seller_id, sku, name, price, warranty_months, stock_qty, reorder_level) VALUES
(1, 'CPU-I5-13400', N'Intel Core i5-13400',          5200000, 36, 12, 5),  -- 1
(1, 'RAM-16-D5',    N'RAM Kingston Fury 16GB DDR5',  1450000, 36, 30, 10), -- 2
(1, 'VGA-4060',     N'ASUS Dual RTX 4060 8GB',       8490000, 36, 4,  5),  -- 3
(2, 'SSD-1T-NV2',   N'SSD Kingston NV2 1TB NVMe',    1690000, 36, 20, 8),  -- 4
(2, 'PSU-650-CM',   N'Nguồn Cooler Master 650W',     1290000, 24, 3,  5),  -- 5
(1, 'MB-B760M',     N'Mainboard MSI PRO B760M-A',    3290000, 36, 9,  4);  -- 6

INSERT INTO parts(part_code, name, stock_qty, reorder_level, unit_cost) VALUES
('PT-FAN-92',    N'Quạt tản nhiệt VGA 92mm',  10, 3, 120000),
('PT-VRAM-GD6',  N'Chip nhớ GDDR6 1GB',        2, 4, 450000),
('PT-CAP-470',   N'Tụ điện 470uF 16V',        50, 20, 5000),
('PT-PASTE',     N'Keo tản nhiệt MX-4',       15, 5, 90000),
('PT-MOSFET',    N'MOSFET nguồn VRM',          0, 5, 35000),
('PT-SATA-CBL',  N'Cáp SATA',                 25, 10, 15000);

INSERT INTO supplier_products(supplier_id, product_id) VALUES (1,1),(1,3),(1,6),(2,2),(2,4),(2,5);
INSERT INTO supplier_parts(supplier_id, part_id) VALUES (1,1),(1,2),(1,5),(2,3),(2,4),(2,6);

INSERT INTO orders(order_code, customer_id, support_id, order_date, status, total_amount) VALUES
('DH250115-001', 6, 2, '2025-01-15 10:00', 'PAID', 13690000),   -- 1
('DH230310-001', 7, 2, '2023-03-10 15:30', 'PAID', 2740000),    -- 2
('DH260920-001', 6, 2, '2026-09-20 09:15', 'PAID', 1690000);    -- 3

INSERT INTO order_items(order_id, product_id, quantity, unit_price) VALUES
(1, 1, 1, 5200000), (1, 3, 1, 8490000),
(2, 2, 1, 1450000), (2, 5, 1, 1290000),
(3, 4, 1, 1690000);

INSERT INTO payments(order_id, amount, method, status, paid_at, transaction_ref) VALUES
(1, 13690000, 'CARD', 'PAID', '2025-01-15 10:05', 'TX250115100500'),
(2, 2740000,  'CASH', 'PAID', '2023-03-10 15:35', 'TX230310153500'),
(3, 1690000,  'E_WALLET', 'PAID', '2026-09-20 09:20', 'TX260920092000');

INSERT INTO warranties(order_id, product_id, serial_number, start_date, end_date, status) VALUES
(1, 1, 'SN-CPU-0001', '2025-01-15', '2028-01-15', 'ACTIVE'),   -- 1
(1, 3, 'SN-VGA-0001', '2025-01-15', '2028-01-15', 'ACTIVE'),   -- 2
(2, 2, 'SN-RAM-0001', '2023-03-10', '2026-03-10', 'EXPIRED'),  -- 3
(2, 5, 'SN-PSU-0001', '2023-03-10', '2025-03-10', 'EXPIRED'),  -- 4
(3, 4, 'SN-SSD-0001', '2026-09-20', '2029-09-20', 'ACTIVE');   -- 5

INSERT INTO tickets(ticket_code, warranty_id, created_by_support_id, support_id, technician_id,
                    channel, issue_description, status, repair_fee, created_at) VALUES
('TK261001-001', 2, NULL, 2, 3, 'WEB',   N'Màn hình bị sọc, artifact khi chơi game', 'IN_REPAIR', 0, '2026-10-01 08:30'),
('TK261102-001', 3, 2,    NULL, NULL, 'PHONE', N'Máy không nhận RAM, kêu bíp liên tục',  'NEW', 0, '2026-11-02 14:00');

INSERT INTO repair_logs(ticket_id, technician_id, diagnosis, created_at) VALUES
(1, 3, N'Lỗi chip nhớ VRAM, cần thay 1 chip GDDR6', '2026-10-02 09:00');
INSERT INTO repair_parts(repair_log_id, part_id, quantity, issued_by, issued_at) VALUES
(1, 2, 1, 5, '2026-10-02 10:00');

INSERT INTO purchase_orders(po_code, supplier_id, inventory_staff_id, order_date, status, total_amount) VALUES
('PO261025-001', 1, 5, '2026-10-25 11:00', 'SENT', 900000);
INSERT INTO purchase_order_parts(purchase_order_id, part_id, quantity, unit_cost) VALUES (1, 2, 2, 450000);

INSERT INTO notifications(customer_id, ticket_id, title, content, sent_at) VALUES
(6, 1, N'Đã tiếp nhận yêu cầu TK261001-001', N'Yêu cầu bảo hành của bạn đã được tiếp nhận.', '2026-10-01 08:30'),
(6, 1, N'Kỹ thuật viên đang xử lý TK261001-001', N'Kỹ thuật viên Phạm Minh Đức đang sửa chữa thiết bị.', '2026-10-02 09:00'),
(7, 2, N'Đã tiếp nhận yêu cầu TK261102-001', N'Sản phẩm đã hết hạn bảo hành – áp dụng sửa chữa có phí.', '2026-11-02 14:00');
GO
