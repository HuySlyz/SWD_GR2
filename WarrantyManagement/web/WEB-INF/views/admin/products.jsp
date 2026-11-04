<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.Product, com.warranty.model.Entities.Seller"%>
<%
    String pageTitle = "Sản phẩm & chính sách bảo hành";
    String activeMenu = "products";
    List<Product> products = (List<Product>) request.getAttribute("products");
    List<Seller> sellers = (List<Seller>) request.getAttribute("sellers");
    Product e = (Product) request.getAttribute("editing");
    String keyword = (String) request.getAttribute("keyword");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Sản phẩm &amp; chính sách bảo hành</h2>
<form class="card" method="post" action="<%= ctx %>/admin/products">
    <h3><%= e == null ? "Thêm sản phẩm" : "Sửa sản phẩm " + Html.esc(e.sku()) %></h3>
    <input type="hidden" name="id" value="<%= e == null ? 0 : e.id() %>">
    <div class="grid">
        <div><label>SKU *</label><input name="sku" value="<%= e == null ? "" : Html.esc(e.sku()) %>" required></div>
        <div><label>Tên sản phẩm *</label><input name="name" value="<%= e == null ? "" : Html.esc(e.name()) %>" required></div>
        <div><label>Người bán</label>
            <select name="sellerId">
                <option value="">-- Không --</option>
                <% for (Seller s : sellers) { %>
                    <option value="<%= s.id() %>" <%= Html.selected(e != null && e.sellerId() != null && e.sellerId() == s.id()) %>><%= Html.esc(s.name()) %></option>
                <% } %>
            </select></div>
        <div><label>Giá bán (VNĐ) *</label><input name="price" value="<%= e == null ? "" : Html.plain(e.price()) %>" required></div>
        <div><label>Chính sách bảo hành (tháng) *</label>
            <input type="number" name="warrantyMonths" min="0" max="120" value="<%= e == null ? 12 : e.warrantyMonths() %>" required></div>
        <div><label>Mức tồn tối thiểu</label>
            <input type="number" name="reorderLevel" min="0" value="<%= e == null ? 5 : e.reorderLevel() %>"></div>
    </div>
    <p class="muted">Tồn kho chỉ thay đổi qua bán hàng, nhập hàng (PO) và kiểm kê của kho.</p>
    <div class="actions">
        <button class="btn green">Lưu</button>
        <% if (e != null) { %><a class="btn gray" href="<%= ctx %>/admin/products">Hủy</a><% } %>
    </div>
</form>

<form class="search" method="get" action="<%= ctx %>/admin/products">
    <input name="q" value="<%= Html.esc(keyword) %>" placeholder="SKU / tên sản phẩm">
    <button class="btn">Tìm</button>
</form>
<div class="table-wrap"><table>
    <tr><th>SKU</th><th>Tên</th><th>Người bán</th><th class="num">Giá</th><th class="num">Bảo hành</th><th class="num">Tồn</th><th>Trạng thái</th><th></th></tr>
    <% for (Product p : products) { %>
    <tr class="<%= p.active() ? "" : "muted-row" %>">
        <td><%= Html.esc(p.sku()) %></td>
        <td><%= Html.esc(p.name()) %></td>
        <td><%= Html.esc(p.sellerName()) %></td>
        <td class="num"><%= Html.money(p.price()) %></td>
        <td class="num"><%= p.warrantyMonths() %> tháng</td>
        <td class="num"><%= p.stockQty() %><%= p.lowStock() ? " <span class='badge err'>thấp</span>" : "" %></td>
        <td><span class="badge <%= p.active() ? "ok" : "err" %>"><%= p.active() ? "Đang bán" : "Ngừng bán" %></span></td>
        <td>
            <a class="btn sm" href="<%= ctx %>/admin/products?id=<%= p.id() %>">Sửa</a>
            <form class="inline" method="post" action="<%= ctx %>/admin/products">
                <input type="hidden" name="action" value="toggle"><input type="hidden" name="id" value="<%= p.id() %>">
                <input type="hidden" name="active" value="<%= !p.active() %>">
                <button class="btn sm <%= p.active() ? "orange" : "green" %>"><%= p.active() ? "Ngừng bán" : "Bán lại" %></button>
            </form>
            <form class="inline" method="post" action="<%= ctx %>/admin/products" onsubmit="return confirm('Xóa sản phẩm?')">
                <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%= p.id() %>">
                <button class="btn sm red">Xóa</button>
            </form>
        </td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
