<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.Supplier, com.warranty.model.Entities.StockItem"%>
<%
    String pageTitle = "Tạo phiếu nhập";
    String activeMenu = "po";
    List<Supplier> suppliers = (List<Supplier>) request.getAttribute("suppliers");
    Supplier supplier = (Supplier) request.getAttribute("supplier");
    List<StockItem> items = (List<StockItem>) request.getAttribute("items");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Tạo phiếu nhập hàng</h2>
<form class="card search" method="get" action="<%= ctx %>/inventory/po">
    <input type="hidden" name="action" value="new">
    <select name="supplierId" onchange="this.form.submit()">
        <option value="">-- Bước 1: chọn nhà cung cấp --</option>
        <% for (Supplier s : suppliers) { %>
            <option value="<%= s.id() %>" <%= Html.selected(supplier != null && supplier.id() == s.id()) %>><%= Html.esc(s.name()) %></option>
        <% } %>
    </select>
    <button class="btn gray">Chọn</button>
</form>

<% if (supplier != null) { %>
<form class="card" method="post" action="<%= ctx %>/inventory/po">
    <input type="hidden" name="action" value="create"><input type="hidden" name="supplierId" value="<%= supplier.id() %>">
    <h3>Bước 2: mặt hàng do <%= Html.esc(supplier.name()) %> cung cấp</h3>
    <% if (items.isEmpty()) { %>
        <p class="muted">Nhà cung cấp chưa được gán mặt hàng nào (Admin → Người bán / NCC).</p>
    <% } else { %>
    <p class="muted">Số lượng gợi ý cho hàng dưới mức tồn = 2 × mức tối thiểu − tồn hiện tại. Để 0 nếu không nhập.</p>
    <div class="table-wrap"><table>
        <tr><th>Loại</th><th>Mặt hàng</th><th class="num">Tồn / tối thiểu</th><th style="width:110px">SL nhập</th><th style="width:150px">Đơn giá nhập</th></tr>
        <% for (StockItem i : items) {
               String key = i.type() + ":" + i.id();
               int suggest = i.lowStock() ? Math.max(i.reorderLevel() * 2 - i.stockQty(), 1) : 0; %>
        <tr>
            <td><%= "PART".equals(i.type()) ? "Linh kiện" : "Sản phẩm" %><input type="hidden" name="item" value="<%= key %>"></td>
            <td><%= Html.esc(i.code()) %> – <%= Html.esc(i.name()) %></td>
            <td class="num"><%= i.stockQty() %> / <%= i.reorderLevel() %><%= i.lowStock() ? " <span class='badge err'>thấp</span>" : "" %></td>
            <td><input type="number" min="0" name="qty_<%= key %>" value="<%= suggest %>"></td>
            <td><input name="cost_<%= key %>" value="<%= Html.plain(i.unitCost()) %>"></td>
        </tr>
        <% } %>
    </table></div>
    <div class="actions"><button class="btn green">Tạo phiếu &amp; gửi nhà cung cấp</button></div>
    <% } %>
</form>
<% } %>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
