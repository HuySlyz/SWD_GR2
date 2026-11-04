<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.Order, com.warranty.model.Codes.OrderStatus"%>
<%
    String pageTitle = "Đơn hàng";
    String activeMenu = "orders";
    List<Order> orders = (List<Order>) request.getAttribute("orders");
    OrderStatus status = (OrderStatus) request.getAttribute("status");
    String keyword = (String) request.getAttribute("keyword");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Đơn hàng</h2>
<form class="search" method="get" action="<%= ctx %>/support/orders">
    <input name="q" value="<%= Html.esc(keyword) %>" placeholder="Mã đơn / tên hoặc SĐT khách">
    <select name="status">
        <option value="">-- Tất cả --</option>
        <% for (OrderStatus s : OrderStatus.values()) { %><option value="<%= s %>" <%= Html.selected(s == status) %>><%= s.getLabel() %></option><% } %>
    </select>
    <button class="btn">Lọc</button>
    <a class="btn green" href="<%= ctx %>/support/orders?action=new">+ Tạo đơn hàng</a>
</form>
<div class="table-wrap"><table>
    <tr><th>Mã đơn</th><th>Ngày</th><th>Khách hàng</th><th>Nhân viên</th><th class="num">Tổng tiền</th><th>Trạng thái</th></tr>
    <% if (orders.isEmpty()) { %><tr><td colspan="6" class="muted">Không có đơn hàng.</td></tr><% } %>
    <% for (Order o : orders) { %>
    <tr>
        <td><a href="<%= ctx %>/support/orders?action=view&id=<%= o.id() %>"><%= Html.esc(o.orderCode()) %></a></td>
        <td><%= Html.date(o.orderDate()) %></td>
        <td><%= Html.esc(o.customerName()) %><br><span class="muted"><%= Html.esc(o.customerPhone()) %></span></td>
        <td><%= Html.esc(o.supportName()) %></td>
        <td class="num"><%= Html.money(o.totalAmount()) %></td>
        <td><span class="badge <%= o.status() %>"><%= o.status().getLabel() %></span></td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
