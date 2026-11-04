<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, java.time.LocalDate, com.warranty.model.Entities.*, com.warranty.model.Codes.WarrantyStatus"%>
<%
    String pageTitle = "Sản phẩm của tôi";
    String activeMenu = "warranties";
    List<Warranty> warranties = (List<Warranty>) request.getAttribute("warranties");
    List<Order> orders = (List<Order>) request.getAttribute("orders");
    LocalDate today = LocalDate.now();
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Sản phẩm đã mua &amp; bảo hành</h2>
<div class="table-wrap"><table>
    <tr><th>Sản phẩm</th><th>Serial</th><th>Đơn hàng</th><th>Bảo hành</th><th>Trạng thái</th><th></th></tr>
    <% if (warranties.isEmpty()) { %><tr><td colspan="6" class="muted">Chưa có sản phẩm.</td></tr><% } %>
    <% for (Warranty w : warranties) { WarrantyStatus st = w.effectiveStatus(today); %>
    <tr>
        <td><%= Html.esc(w.productName()) %></td>
        <td><%= Html.esc(w.serialNumber()) %></td>
        <td><%= Html.esc(w.orderCode()) %></td>
        <td><%= Html.date(w.startDate()) %> → <%= Html.date(w.endDate()) %></td>
        <td><span class="badge <%= st %>"><%= st.getLabel() %></span></td>
        <td><a class="btn sm" href="<%= ctx %>/lookup?serial=<%= Html.url(w.serialNumber()) %>">Chi tiết</a>
            <a class="btn sm green" href="<%= ctx %>/tickets?action=new&serial=<%= Html.url(w.serialNumber()) %>">Yêu cầu BH</a></td>
    </tr>
    <% } %>
</table></div>

<h2 style="margin-top:22px">Đơn hàng</h2>
<div class="table-wrap"><table>
    <tr><th>Mã đơn</th><th>Ngày</th><th class="num">Tổng tiền</th><th>Trạng thái</th></tr>
    <% for (Order o : orders) { %>
    <tr><td><%= Html.esc(o.orderCode()) %></td><td><%= Html.date(o.orderDate()) %></td>
        <td class="num"><%= Html.money(o.totalAmount()) %></td><td><span class="badge <%= o.status() %>"><%= o.status().getLabel() %></span></td></tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
