<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.PurchaseOrder, com.warranty.model.Codes.PoStatus"%>
<%
    String pageTitle = "Phiếu nhập hàng";
    String activeMenu = "po";
    List<PurchaseOrder> orders = (List<PurchaseOrder>) request.getAttribute("orders");
    PoStatus status = (PoStatus) request.getAttribute("status");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Phiếu nhập hàng (purchase order)</h2>
<form class="search" method="get" action="<%= ctx %>/inventory/po">
    <select name="status">
        <option value="">-- Tất cả --</option>
        <% for (PoStatus s : PoStatus.values()) { %><option value="<%= s %>" <%= Html.selected(s == status) %>><%= s.getLabel() %></option><% } %>
    </select>
    <button class="btn">Lọc</button>
    <a class="btn green" href="<%= ctx %>/inventory/po?action=new">+ Tạo phiếu nhập</a>
</form>
<div class="table-wrap"><table>
    <tr><th>Mã PO</th><th>Ngày</th><th>Nhà cung cấp</th><th>Người lập</th><th class="num">Tổng</th><th>Trạng thái</th><th>Nhận hàng</th></tr>
    <% if (orders.isEmpty()) { %><tr><td colspan="7" class="muted">Không có phiếu nhập.</td></tr><% } %>
    <% for (PurchaseOrder po : orders) { %>
    <tr>
        <td><a href="<%= ctx %>/inventory/po?action=view&id=<%= po.id() %>"><%= Html.esc(po.poCode()) %></a></td>
        <td><%= Html.date(po.orderDate()) %></td>
        <td><%= Html.esc(po.supplierName()) %></td>
        <td><%= Html.esc(po.staffName()) %></td>
        <td class="num"><%= Html.money(po.totalAmount()) %></td>
        <td><span class="badge <%= po.status() %>"><%= po.status().getLabel() %></span></td>
        <td><%= po.receivedAt() == null ? "" : Html.esc(po.receivedByName()) + " – " + Html.date(po.receivedAt()) %></td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
