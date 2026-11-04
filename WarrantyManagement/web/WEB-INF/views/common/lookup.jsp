<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, java.time.LocalDate, com.warranty.model.Entities.*, com.warranty.model.Codes.WarrantyStatus"%>
<%
    String pageTitle = "Tra cứu bảo hành";
    String activeMenu = "lookup";
    String serial = (String) request.getAttribute("serial");
    Warranty w = (Warranty) request.getAttribute("warranty");
    String lookupError = (String) request.getAttribute("lookupError");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Tra cứu bảo hành</h2>
<form class="card search" method="get" action="<%= ctx %>/lookup">
    <input name="serial" value="<%= Html.esc(serial) %>" placeholder="Nhập số serial / mã bảo hành, ví dụ SN-VGA-0001" autofocus>
    <button class="btn">Tra cứu</button>
</form>

<% if (lookupError != null) { %><div class="alert err"><%= Html.esc(lookupError) %></div><% } %>

<% if (w != null) {
       LocalDate today = LocalDate.now();
       WarrantyStatus st = w.effectiveStatus(today);
       List<Ticket> history = (List<Ticket>) request.getAttribute("history");
       boolean isOwner = me != null && me.role() == Role.CUSTOMER && me.id() == w.customerId();
       boolean canCreate = me != null && (isOwner || me.role() == Role.SUPPORT);
%>
<div class="card">
    <h3>Thông tin bảo hành <span class="badge <%= st %>"><%= st.getLabel() %></span></h3>
    <dl class="info">
        <dt>Serial</dt><dd><%= Html.esc(w.serialNumber()) %></dd>
        <dt>Sản phẩm</dt><dd><%= Html.esc(w.productName()) %> <span class="muted">(<%= Html.esc(w.sku()) %>)</span></dd>
        <dt>Đơn hàng</dt><dd><%= Html.esc(w.orderCode()) %></dd>
        <% if (me != null && (me.role().isStaff() || isOwner)) { %>
        <dt>Khách hàng</dt><dd><%= Html.esc(w.customerName()) %> – <%= Html.esc(w.customerPhone()) %></dd>
        <% } %>
        <dt>Bắt đầu</dt><dd><%= Html.date(w.startDate()) %></dd>
        <dt>Hết hạn</dt><dd><%= Html.date(w.endDate()) %>
            <% if (st == WarrantyStatus.ACTIVE) { %><span class="muted">(còn <%= java.time.temporal.ChronoUnit.DAYS.between(today, w.endDate()) %> ngày)</span><% } %></dd>
    </dl>
    <% if (canCreate) { %>
    <div class="actions">
        <a class="btn green" href="<%= ctx %>/tickets?action=new&serial=<%= Html.url(w.serialNumber()) %>">
            <%= st == WarrantyStatus.ACTIVE ? "Tạo yêu cầu bảo hành" : "Yêu cầu sửa chữa có phí" %></a>
    </div>
    <% } else if (me == null) { %>
        <p class="muted">Đăng nhập để gửi yêu cầu bảo hành.</p>
    <% } %>
</div>

<div class="card">
    <h3>Lịch sử bảo hành</h3>
    <% if (history.isEmpty()) { %><p class="muted">Chưa có lần bảo hành nào.</p><% } else { %>
    <div class="table-wrap"><table>
        <tr><th>Mã ticket</th><th>Ngày tạo</th><th>Mô tả lỗi</th><th>Trạng thái</th></tr>
        <% for (Ticket t : history) { %>
        <tr>
            <td><%= Html.esc(t.ticketCode()) %></td>
            <td><%= Html.date(t.createdAt()) %></td>
            <td><%= Html.esc(t.issueDescription()) %></td>
            <td><span class="badge <%= t.status() %>"><%= t.status().getLabel() %></span></td>
        </tr>
        <% } %>
    </table></div>
    <% } %>
</div>
<% } %>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
