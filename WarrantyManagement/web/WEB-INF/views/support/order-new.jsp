<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.Product"%>
<%
    String pageTitle = "Tạo đơn hàng";
    String activeMenu = "orders";
    List<com.warranty.model.Entities.User> customers = (List<com.warranty.model.Entities.User>) request.getAttribute("customers");
    List<Product> products = (List<Product>) request.getAttribute("products");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Tạo đơn hàng</h2>
<form class="card" method="post" action="<%= ctx %>/support/orders">
    <input type="hidden" name="action" value="create">
    <label>Khách hàng *</label>
    <select name="customerId" required>
        <option value="">-- Chọn khách hàng --</option>
        <% for (com.warranty.model.Entities.User c : customers) { %>
            <option value="<%= c.id() %>"><%= Html.esc(c.fullName()) %> – <%= Html.esc(c.phone()) %></option>
        <% } %>
    </select>
    <p class="muted">Khách mới: nhờ Admin tạo tài khoản khách hàng trước.</p>

    <h3 style="margin-top:14px">Sản phẩm</h3>
    <table>
        <tr><th>Sản phẩm</th><th style="width:120px">Số lượng</th></tr>
        <% for (int i = 0; i < 5; i++) { %>
        <tr>
            <td><select name="productId">
                <option value="">--</option>
                <% for (Product p : products) { %>
                    <option value="<%= p.id() %>" <%= p.stockQty() == 0 ? "disabled" : "" %>>
                        <%= Html.esc(p.sku()) %> – <%= Html.esc(p.name()) %> – <%= Html.money(p.price()) %> (tồn <%= p.stockQty() %>, BH <%= p.warrantyMonths() %>th)</option>
                <% } %>
            </select></td>
            <td><input type="number" name="qty" min="1" value="1"></td>
        </tr>
        <% } %>
    </table>
    <p class="muted">Hệ thống kiểm tra tồn kho, tạo đơn ở trạng thái <b>Chờ thanh toán</b>; bảo hành được kích hoạt sau khi thanh toán thành công.</p>
    <div class="actions">
        <button class="btn green">Tạo đơn</button>
        <a class="btn gray" href="<%= ctx %>/support/orders">Hủy</a>
    </div>
</form>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
