<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.*, com.warranty.model.TicketStatus"%>
<%
    String pageTitle = "Ticket";
    String activeMenu = "tickets";
    List<Ticket> tickets = (List<Ticket>) request.getAttribute("tickets");
    TicketStatus status = (TicketStatus) request.getAttribute("status");
    String keyword = (String) request.getAttribute("keyword");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>
<%
    String heading = me.role() == Role.TECHNICIAN ? "Ticket được giao cho tôi"
            : me.role() == Role.CUSTOMER ? "Yêu cầu bảo hành của tôi" : "Danh sách ticket";
%>
<h2><%= heading %></h2>
<% if (me.role() == Role.SUPPORT) { %>
    <div class="alert info">Ticket <b>Mới</b> cần được duyệt và phân công kỹ thuật viên.</div>
<% } %>
<form class="search" method="get" action="<%= ctx %>/tickets">
    <input name="q" value="<%= Html.esc(keyword) %>" placeholder="Mã ticket / serial / tên hoặc SĐT khách">
    <select name="status">
        <option value="">-- Tất cả trạng thái --</option>
        <% for (TicketStatus s : TicketStatus.values()) { %>
            <option value="<%= s %>" <%= Html.selected(s == status) %>><%= s.getLabel() %></option>
        <% } %>
    </select>
    <button class="btn">Lọc</button>
    <% if (me.role() == Role.SUPPORT || me.role() == Role.CUSTOMER) { %>
        <a class="btn green" href="<%= ctx %>/tickets?action=new">+ Tạo ticket</a>
    <% } %>
</form>

<div class="table-wrap"><table>
    <tr><th>Mã ticket</th><th>Ngày tạo</th><th>Sản phẩm / Serial</th><th>Khách hàng</th><th>Bảo hành</th>
        <th>Kỹ thuật viên</th><th>Trạng thái</th><th class="num">Phí</th></tr>
    <% if (tickets.isEmpty()) { %><tr><td colspan="8" class="muted">Không có ticket nào.</td></tr><% } %>
    <% for (Ticket t : tickets) { %>
    <tr class="<%= t.status() == TicketStatus.CLOSED ? "muted-row" : "" %>">
        <td><a href="<%= ctx %>/tickets?action=view&id=<%= t.id() %>"><%= Html.esc(t.ticketCode()) %></a></td>
        <td><%= Html.date(t.createdAt()) %><br><span class="muted"><%= t.channel().getLabel() %></span></td>
        <td><%= Html.esc(t.warranty().productName()) %><br><span class="muted"><%= Html.esc(t.warranty().serialNumber()) %></span></td>
        <td><%= Html.esc(t.warranty().customerName()) %><br><span class="muted"><%= Html.esc(t.warranty().customerPhone()) %></span></td>
        <td><%= t.inWarranty() ? "<span class='badge ok'>Trong BH</span>" : "<span class='badge err'>Có phí</span>" %></td>
        <td><%= Html.esc(t.technicianName()) %></td>
        <td><span class="badge <%= t.status() %>"><%= t.status().getLabel() %></span></td>
        <td class="num"><%= t.repairFee().signum() > 0 ? Html.money(t.repairFee()) : "" %></td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
