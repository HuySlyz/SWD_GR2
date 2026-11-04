<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.PurchaseOrder, com.warranty.model.Entities.PoLine, com.warranty.model.Codes.PoStatus"%>
<%
    String pageTitle = "Chi tiết phiếu nhập";
    String activeMenu = "po";
    PurchaseOrder po = (PurchaseOrder) request.getAttribute("po");
    List<PoLine> lines = (List<PoLine>) request.getAttribute("lines");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Phiếu nhập <%= Html.esc(po.poCode()) %> <span class="badge <%= po.status() %>"><%= po.status().getLabel() %></span></h2>
<div class="card">
    <dl class="info">
        <dt>Nhà cung cấp</dt><dd><%= Html.esc(po.supplierName()) %></dd>
        <dt>Người lập</dt><dd><%= Html.esc(po.staffName()) %> – <%= Html.date(po.orderDate()) %></dd>
        <dt>Nhận hàng</dt><dd><%= po.receivedAt() == null ? "Chưa nhận" : Html.esc(po.receivedByName()) + " – " + Html.date(po.receivedAt()) %></dd>
    </dl>
    <table style="margin-top:12px">
        <tr><th>Loại</th><th>Mặt hàng</th><th class="num">SL</th><th class="num">Đơn giá</th><th class="num">Thành tiền</th></tr>
        <% for (PoLine l : lines) { %>
        <tr><td><%= "PART".equals(l.type()) ? "Linh kiện" : "Sản phẩm" %></td><td><%= Html.esc(l.code()) %> – <%= Html.esc(l.name()) %></td>
            <td class="num"><%= l.quantity() %></td><td class="num"><%= Html.money(l.unitCost()) %></td><td class="num"><%= Html.money(l.lineTotal()) %></td></tr>
        <% } %>
        <tr><th colspan="4" class="num">Tổng cộng</th><th class="num"><%= Html.money(po.totalAmount()) %></th></tr>
    </table>
    <% if (po.status() == PoStatus.SENT) { %>
    <form method="post" action="<%= ctx %>/inventory/po" onsubmit="return confirm('Xác nhận đã nhận đủ hàng? Tồn kho sẽ được cộng.')">
        <input type="hidden" name="action" value="receive"><input type="hidden" name="id" value="<%= po.id() %>">
        <div class="actions"><button class="btn green">Xác nhận nhận hàng &amp; cập nhật tồn kho</button></div>
    </form>
    <% } %>
</div>
<a class="btn gray" href="<%= ctx %>/inventory/po">← Danh sách phiếu nhập</a>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
