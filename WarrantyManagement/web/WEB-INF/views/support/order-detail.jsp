<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.*, com.warranty.model.Codes.*"%>
<%
    String pageTitle = "Chi tiết đơn hàng";
    String activeMenu = "orders";
    Order o = (Order) request.getAttribute("order");
    List<OrderItem> items = (List<OrderItem>) request.getAttribute("items");
    List<Payment> payments = (List<Payment>) request.getAttribute("payments");
    List<Warranty> warranties = (List<Warranty>) request.getAttribute("warranties");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Đơn hàng <%= Html.esc(o.orderCode()) %> <span class="badge <%= o.status() %>"><%= o.status().getLabel() %></span></h2>
<div class="cols">
    <div class="card">
        <dl class="info">
            <dt>Khách hàng</dt><dd><%= Html.esc(o.customerName()) %> – <%= Html.esc(o.customerPhone()) %></dd>
            <dt>Ngày tạo</dt><dd><%= Html.date(o.orderDate()) %></dd>
            <dt>Nhân viên</dt><dd><%= Html.esc(o.supportName()) %></dd>
        </dl>
        <table style="margin-top:12px">
            <tr><th>Sản phẩm</th><th class="num">SL</th><th class="num">Đơn giá</th><th class="num">Thành tiền</th></tr>
            <% for (OrderItem i : items) { %>
            <tr><td><%= Html.esc(i.sku()) %> – <%= Html.esc(i.productName()) %></td><td class="num"><%= i.quantity() %></td>
                <td class="num"><%= Html.money(i.unitPrice()) %></td><td class="num"><%= Html.money(i.lineTotal()) %></td></tr>
            <% } %>
            <tr><th colspan="3" class="num">Tổng cộng</th><th class="num"><%= Html.money(o.totalAmount()) %></th></tr>
        </table>
    </div>

    <div>
        <% if (o.status() == OrderStatus.PENDING) { %>
        <form class="card" method="post" action="<%= ctx %>/support/orders">
            <h3>Thanh toán (cổng thanh toán)</h3>
            <input type="hidden" name="action" value="pay"><input type="hidden" name="id" value="<%= o.id() %>">
            <label>Phương thức</label>
            <select name="method">
                <% for (PaymentMethod m : PaymentMethod.values()) { %><option value="<%= m %>"><%= m.getLabel() %></option><% } %>
            </select>
            <label class="inline"><input type="checkbox" name="simulateFail"> Giả lập giao dịch bị từ chối</label>
            <div class="actions">
                <button class="btn green">Thanh toán <%= Html.money(o.totalAmount()) %></button>
            </div>
        </form>
        <form class="card" method="post" action="<%= ctx %>/support/orders" onsubmit="return confirm('Hủy đơn hàng?')">
            <input type="hidden" name="action" value="cancel"><input type="hidden" name="id" value="<%= o.id() %>">
            <button class="btn red">Hủy đơn</button>
        </form>
        <% } %>

        <% if (!payments.isEmpty()) { %>
        <div class="card">
            <h3>Giao dịch thanh toán</h3>
            <table>
                <tr><th>Thời gian</th><th>Phương thức</th><th>Kết quả</th><th>Mã GD</th></tr>
                <% for (Payment p : payments) { %>
                <tr><td><%= Html.date(p.paidAt()) %></td><td><%= p.method().getLabel() %></td>
                    <td><span class="badge <%= p.status() %>"><%= p.status().getLabel() %></span></td><td><%= Html.esc(p.transactionRef()) %></td></tr>
                <% } %>
            </table>
        </div>
        <% } %>

        <% if (!warranties.isEmpty()) { %>
        <div class="card">
            <h3>Bảo hành đã kích hoạt</h3>
            <table>
                <tr><th>Serial</th><th>Sản phẩm</th><th>Hết hạn</th></tr>
                <% for (Warranty w : warranties) { %>
                <tr><td><a href="<%= ctx %>/lookup?serial=<%= Html.url(w.serialNumber()) %>"><%= Html.esc(w.serialNumber()) %></a></td>
                    <td><%= Html.esc(w.productName()) %></td><td><%= Html.date(w.endDate()) %></td></tr>
                <% } %>
            </table>
        </div>
        <% } %>
    </div>
</div>
<a class="btn gray" href="<%= ctx %>/support/orders">← Danh sách đơn hàng</a>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
