<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.Part"%>
<%
    String pageTitle = "Linh kiện";
    String activeMenu = "parts";
    List<Part> parts = (List<Part>) request.getAttribute("parts");
    Part e = (Part) request.getAttribute("editing");
    String keyword = (String) request.getAttribute("keyword");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Linh kiện thay thế (component)</h2>
<form class="card" method="post" action="<%= ctx %>/inventory/parts">
    <h3><%= e == null ? "Thêm linh kiện" : "Sửa linh kiện " + Html.esc(e.partCode()) %></h3>
    <input type="hidden" name="id" value="<%= e == null ? 0 : e.id() %>">
    <div class="grid">
        <div><label>Mã linh kiện *</label><input name="partCode" value="<%= e == null ? "" : Html.esc(e.partCode()) %>" required></div>
        <div><label>Tên *</label><input name="name" value="<%= e == null ? "" : Html.esc(e.name()) %>" required></div>
        <div><label>Mức tồn tối thiểu</label><input type="number" min="0" name="reorderLevel" value="<%= e == null ? 5 : e.reorderLevel() %>"></div>
        <div><label>Đơn giá nhập (VNĐ)</label><input name="unitCost" value="<%= e == null ? "0" : Html.plain(e.unitCost()) %>"></div>
    </div>
    <p class="muted">Số lượng tồn tăng khi nhận hàng từ phiếu nhập, giảm khi cấp cho kỹ thuật viên; điều chỉnh qua kiểm kê.</p>
    <div class="actions"><button class="btn green">Lưu</button>
        <% if (e != null) { %><a class="btn gray" href="<%= ctx %>/inventory/parts">Hủy</a><% } %></div>
</form>

<form class="search" method="get" action="<%= ctx %>/inventory/parts">
    <input name="q" value="<%= Html.esc(keyword) %>" placeholder="Mã / tên linh kiện"><button class="btn">Tìm</button>
</form>
<div class="table-wrap"><table>
    <tr><th>Mã</th><th>Tên</th><th class="num">Tồn</th><th class="num">Tối thiểu</th><th class="num">Đơn giá</th><th></th></tr>
    <% for (Part p : parts) { %>
    <tr>
        <td><%= Html.esc(p.partCode()) %></td>
        <td><%= Html.esc(p.name()) %></td>
        <td class="num"><%= p.stockQty() %><%= p.lowStock() ? " <span class='badge err'>thấp</span>" : "" %></td>
        <td class="num"><%= p.reorderLevel() %></td>
        <td class="num"><%= Html.money(p.unitCost()) %></td>
        <td><a class="btn sm" href="<%= ctx %>/inventory/parts?id=<%= p.id() %>">Sửa</a>
            <form class="inline" method="post" action="<%= ctx %>/inventory/parts" onsubmit="return confirm('Xóa linh kiện?')">
                <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%= p.id() %>">
                <button class="btn sm red">Xóa</button></form></td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
