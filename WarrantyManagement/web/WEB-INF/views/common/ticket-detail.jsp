<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.*, com.warranty.model.Entities.Part, com.warranty.model.TicketStatus, com.warranty.model.Codes.PaymentMethod"%>
<%
    String pageTitle = "Chi tiết ticket";
    String activeMenu = "tickets";
    Ticket t = (Ticket) request.getAttribute("ticket");
    Warranty w = t.warranty();
    List<RepairLog> logs = (List<RepairLog>) request.getAttribute("logs");
    List<RepairPart> parts = (List<RepairPart>) request.getAttribute("parts");
    List<Handover> handovers = (List<Handover>) request.getAttribute("handovers");
    List<Payment> payments = (List<Payment>) request.getAttribute("payments");
    List<User> technicians = (List<User>) request.getAttribute("technicians");
    List<Part> allParts = (List<Part>) request.getAttribute("allParts");
    boolean paid = (Boolean) request.getAttribute("paid");
    TicketStatus st = t.status();
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>
<%
    boolean isTech = me.role() == Role.TECHNICIAN;
    boolean isSupport = me.role() == Role.SUPPORT;
    boolean canPayOrClose = me.role() == Role.CUSTOMER || isSupport || me.role() == Role.ADMIN;
    boolean feeDue = t.repairFee().signum() > 0 && !paid;
%>

<h2>Ticket <%= Html.esc(t.ticketCode()) %>
    <span class="badge <%= st %>"><%= st.getLabel() %></span>
    <%= t.inWarranty() ? "<span class='badge ok'>Trong bảo hành</span>" : "<span class='badge err'>Ngoài bảo hành – có phí</span>" %>
</h2>

<div class="cols">
    <div>
        <div class="card">
            <h3>Thông tin yêu cầu</h3>
            <dl class="info">
                <dt>Sản phẩm</dt><dd><%= Html.esc(w.productName()) %></dd>
                <dt>Serial</dt><dd><%= Html.esc(w.serialNumber()) %></dd>
                <dt>Bảo hành</dt><dd><%= Html.date(w.startDate()) %> → <%= Html.date(w.endDate()) %></dd>
                <dt>Khách hàng</dt><dd><%= Html.esc(w.customerName()) %> – <%= Html.esc(w.customerPhone()) %></dd>
                <dt>Mô tả lỗi</dt><dd><%= Html.esc(t.issueDescription()) %></dd>
                <dt>Kênh</dt><dd><%= t.channel().getLabel() %><%= t.createdBySupportName() != null ? " – tạo bởi " + Html.esc(t.createdBySupportName()) : "" %></dd>
                <dt>Ngày tạo</dt><dd><%= Html.date(t.createdAt()) %></dd>
                <dt>Support phân công</dt><dd><%= Html.esc(t.supportName()) %></dd>
                <dt>Kỹ thuật viên</dt><dd><%= Html.esc(t.technicianName()) %></dd>
                <dt>Phí sửa chữa</dt><dd><%= Html.money(t.repairFee()) %>
                    <% if (t.repairFee().signum() > 0) { %><span class="badge <%= paid ? "PAID" : "PENDING" %>"><%= paid ? "Đã thanh toán" : "Chưa thanh toán" %></span><% } %></dd>
                <dt>Ngày đóng</dt><dd><%= Html.date(t.closedAt()) %></dd>
            </dl>
        </div>

        <div class="card">
            <h3>Nhật ký sửa chữa</h3>
            <% if (logs.isEmpty() && handovers.isEmpty()) { %><p class="muted">Chưa có.</p><% } %>
            <ul class="timeline">
                <% for (RepairLog l : logs) { %>
                <li><b><%= Html.date(l.createdAt()) %></b> – <%= Html.esc(l.technicianName()) %><br>
                    Chẩn đoán: <%= Html.esc(l.diagnosis()) %>
                    <% if (l.repairResult() != null) { %><br>Kết quả: <%= Html.esc(l.repairResult()) %><% } %>
                    <% if (l.shiftNote() != null) { %><br><span class="muted">Ghi chú ca: <%= Html.esc(l.shiftNote()) %></span><% } %></li>
                <% } %>
                <% for (Handover h : handovers) { %>
                <li><b><%= Html.date(h.handedOverAt()) %></b> – Bàn giao: <%= Html.esc(h.fromName()) %> → <%= Html.esc(h.toName()) %>
                    <% if (h.note() != null) { %><br><span class="muted"><%= Html.esc(h.note()) %></span><% } %></li>
                <% } %>
            </ul>
            <% if (!parts.isEmpty()) { %>
            <h3 style="margin-top:12px">Linh kiện sử dụng</h3>
            <table>
                <tr><th>Linh kiện</th><th class="num">SL</th><th>Kho giao</th></tr>
                <% for (RepairPart p : parts) { %>
                <tr><td><%= Html.esc(p.partCode()) %> – <%= Html.esc(p.partName()) %></td><td class="num"><%= p.quantity() %></td>
                    <td><%= p.delivered() ? Html.esc(p.issuedByName()) + " (" + Html.date(p.issuedAt()) + ")" : "<span class='badge PENDING'>Chờ kho giao</span>" %></td></tr>
                <% } %>
            </table>
            <% } %>
            <% if (!payments.isEmpty()) { %>
            <h3 style="margin-top:12px">Thanh toán</h3>
            <table>
                <tr><th>Thời gian</th><th>Phương thức</th><th class="num">Số tiền</th><th>Kết quả</th></tr>
                <% for (Payment p : payments) { %>
                <tr><td><%= Html.date(p.paidAt()) %></td><td><%= p.method().getLabel() %></td><td class="num"><%= Html.money(p.amount()) %></td>
                    <td><span class="badge <%= p.status() %>"><%= p.status().getLabel() %></span> <span class="muted"><%= Html.esc(p.transactionRef()) %></span></td></tr>
                <% } %>
            </table>
            <% } %>
        </div>
    </div>

    <div>
        <%-- ============ Flow 1: Support duyet & phan cong ============ --%>
        <% if (isSupport && st == TicketStatus.NEW) { %>
        <form class="card" method="post" action="<%= ctx %>/tickets">
            <h3>Duyệt &amp; phân công kỹ thuật viên</h3>
            <input type="hidden" name="action" value="assign"><input type="hidden" name="id" value="<%= t.id() %>">
            <select name="technicianId" required>
                <option value="">-- Chọn kỹ thuật viên --</option>
                <% for (User u : technicians) { %><option value="<%= u.id() %>"><%= Html.esc(u.fullName()) %></option><% } %>
            </select>
            <div class="actions"><button class="btn">Phân công</button></div>
        </form>
        <% } %>

        <%-- ============ Flow 2: Ky thuat vien ============ --%>
        <% if (isTech && st == TicketStatus.ASSIGNED) { %>
        <form class="card" method="post" action="<%= ctx %>/tickets">
            <h3>Nhận việc &amp; chẩn đoán</h3>
            <input type="hidden" name="action" value="accept"><input type="hidden" name="id" value="<%= t.id() %>">
            <textarea name="diagnosis" required placeholder="Kết quả kiểm tra, nguyên nhân lỗi..."></textarea>
            <div class="actions"><button class="btn">Nhận việc (Đang sửa chữa)</button></div>
        </form>
        <% } %>

        <% if (isTech && st == TicketStatus.IN_REPAIR) { %>
        <form class="card" method="post" action="<%= ctx %>/tickets">
            <h3>Yêu cầu linh kiện thay thế</h3>
            <input type="hidden" name="action" value="requestPart"><input type="hidden" name="id" value="<%= t.id() %>">
            <div class="grid">
                <div><label>Linh kiện</label>
                    <select name="partId" required>
                        <% for (Part p : allParts) { %>
                            <option value="<%= p.id() %>"><%= Html.esc(p.partCode()) %> – <%= Html.esc(p.name()) %> (tồn <%= p.stockQty() %>)</option>
                        <% } %>
                    </select></div>
                <div><label>Số lượng</label><input type="number" name="quantity" min="1" value="1" required></div>
            </div>
            <div class="actions"><button class="btn orange">Gửi yêu cầu kho</button></div>
        </form>

        <form class="card" method="post" action="<%= ctx %>/tickets">
            <h3>Hoàn tất sửa chữa</h3>
            <input type="hidden" name="action" value="complete"><input type="hidden" name="id" value="<%= t.id() %>">
            <label>Kết quả sửa chữa *</label>
            <textarea name="result" required></textarea>
            <label>Ghi chú bàn giao ca</label>
            <input name="shiftNote">
            <% if (!t.inWarranty()) { %>
                <label>Phí sửa chữa (VNĐ) *</label>
                <input name="repairFee" value="0" required>
            <% } %>
            <div class="actions"><button class="btn green">Hoàn tất &amp; báo khách</button></div>
        </form>
        <% } %>

        <% if (isTech && (st == TicketStatus.ASSIGNED || st == TicketStatus.IN_REPAIR)) { %>
        <form class="card" method="post" action="<%= ctx %>/tickets">
            <h3>Bàn giao cho kỹ thuật viên khác</h3>
            <input type="hidden" name="action" value="handover"><input type="hidden" name="id" value="<%= t.id() %>">
            <select name="toTechnicianId" required>
                <% for (User u : technicians) { if (u.id() == me.id()) continue; %>
                    <option value="<%= u.id() %>"><%= Html.esc(u.fullName()) %></option>
                <% } %>
            </select>
            <label>Ghi chú bàn giao</label>
            <input name="note" placeholder="Tình trạng hiện tại, việc cần làm tiếp...">
            <div class="actions"><button class="btn gray">Bàn giao</button></div>
        </form>
        <% } %>

        <%-- ============ Thanh toan phi & xac nhan hoan thanh ============ --%>
        <% if (canPayOrClose && st == TicketStatus.COMPLETED && feeDue) { %>
        <form class="card" method="post" action="<%= ctx %>/tickets">
            <h3>Thanh toán phí sửa chữa – <%= Html.money(t.repairFee()) %></h3>
            <input type="hidden" name="action" value="pay"><input type="hidden" name="id" value="<%= t.id() %>">
            <select name="method">
                <% for (PaymentMethod m : PaymentMethod.values()) { %><option value="<%= m %>"><%= m.getLabel() %></option><% } %>
            </select>
            <label class="inline"><input type="checkbox" name="simulateFail"> Giả lập cổng thanh toán từ chối</label>
            <div class="actions"><button class="btn green">Thanh toán</button></div>
        </form>
        <% } %>

        <% if (canPayOrClose && st == TicketStatus.COMPLETED && !feeDue) { %>
        <form class="card" method="post" action="<%= ctx %>/tickets" onsubmit="return confirm('Xác nhận đã nhận lại thiết bị và đóng ticket?')">
            <h3>Xác nhận hoàn thành</h3>
            <p class="muted">Thiết bị đã sửa xong. Xác nhận khi khách đã nhận lại sản phẩm.</p>
            <input type="hidden" name="action" value="close"><input type="hidden" name="id" value="<%= t.id() %>">
            <div class="actions"><button class="btn green">Xác nhận &amp; đóng ticket</button></div>
        </form>
        <% } %>
    </div>
</div>
<a class="btn gray" href="<%= ctx %>/tickets">← Danh sách ticket</a>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
