<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.StockItem"%>
<%
    String pageTitle = "Tồn kho";
    String activeMenu = "stock";
    List<StockItem> items = (List<StockItem>) request.getAttribute("items");
    boolean lowOnly = (Boolean) request.getAttribute("lowOnly");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Báo cáo tồn kho</h2>
<div class="actions" style="margin-bottom:12px">
    <a class="btn <%= lowOnly ? "" : "gray" %>" href="<%= ctx %>/inventory/stock">Dưới mức tồn tối thiểu</a>
    <a class="btn <%= lowOnly ? "gray" : "" %>" href="<%= ctx %>/inventory/stock?view=all">Tất cả / kiểm kê</a>
    <a class="btn green" href="<%= ctx %>/inventory/po?action=new">+ Tạo phiếu nhập (PO)</a>
</div>
<% if (lowOnly) { %><div class="alert info">Các mặt hàng có tồn kho ≤ mức tối thiểu (reorder level) cần lập phiếu nhập từ nhà cung cấp.</div><% } %>

<div class="table-wrap"><table>
    <tr><th>Loại</th><th>Mã</th><th>Tên</th><th class="num">Tồn</th><th class="num">Tối thiểu</th><th>Kiểm kê (số thực đếm)</th></tr>
    <% if (items.isEmpty()) { %><tr><td colspan="6" class="muted">Không có mặt hàng nào dưới mức tồn.</td></tr><% } %>
    <% for (StockItem i : items) { %>
    <tr>
        <td><%= "PART".equals(i.type()) ? "Linh kiện" : "Sản phẩm" %></td>
        <td><%= Html.esc(i.code()) %></td>
        <td><%= Html.esc(i.name()) %></td>
        <td class="num"><%= i.stockQty() %><%= i.lowStock() ? " <span class='badge err'>thấp</span>" : "" %></td>
        <td class="num"><%= i.reorderLevel() %></td>
        <td>
            <form class="inline" method="post" action="<%= ctx %>/inventory/stock">
                <input type="hidden" name="type" value="<%= i.type() %>"><input type="hidden" name="id" value="<%= i.id() %>">
                <input type="hidden" name="view" value="<%= lowOnly ? "" : "all" %>">
                <input type="number" name="countedQty" min="0" value="<%= i.stockQty() %>" style="width:90px">
                <button class="btn sm gray">Điều chỉnh</button>
            </form>
        </td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
