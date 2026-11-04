<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.dao.ReportDAO.*, com.warranty.model.TicketStatus, com.warranty.model.Entities.StockItem"%>
<%
    String pageTitle = "Tổng quan";
    String activeMenu = "dashboard";
    Summary s = (Summary) request.getAttribute("summary");
    List<TopProduct> top = (List<TopProduct>) request.getAttribute("topProducts");
    List<TechLoad> load = (List<TechLoad>) request.getAttribute("techLoad");
    List<StockItem> low = (List<StockItem>) request.getAttribute("lowStock");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Báo cáo &amp; thống kê</h2>
<div class="stats">
    <div class="stat"><div class="k">Doanh thu bán hàng</div><div class="v"><%= Html.money(s.salesRevenue()) %></div></div>
    <div class="stat"><div class="k">Thu phí sửa chữa</div><div class="v"><%= Html.money(s.repairRevenue()) %></div></div>
    <div class="stat"><div class="k">Khách hàng</div><div class="v"><%= s.customers() %></div></div>
    <div class="stat"><div class="k">Bảo hành còn hiệu lực</div><div class="v"><%= s.activeWarranties() %></div></div>
    <div class="stat"><div class="k">Mặt hàng dưới mức tồn</div><div class="v"><%= s.lowStockItems() %></div></div>
    <div class="stat"><div class="k">Yêu cầu linh kiện chờ giao</div><div class="v"><%= s.pendingPartRequests() %></div></div>
    <div class="stat"><div class="k">PO chờ nhận hàng</div><div class="v"><%= s.openPurchaseOrders() %></div></div>
</div>

<div class="cols">
    <div class="card">
        <h3>Ticket theo trạng thái</h3>
        <table>
            <% for (TicketStatus ts : TicketStatus.values()) { %>
            <tr><td><span class="badge <%= ts %>"><%= ts.getLabel() %></span></td>
                <td class="num"><a href="<%= ctx %>/tickets?status=<%= ts %>"><%= s.ticketsByStatus().get(ts) %></a></td></tr>
            <% } %>
        </table>
    </div>
    <div class="card">
        <h3>Khối lượng kỹ thuật viên</h3>
        <table>
            <tr><th>Kỹ thuật viên</th><th class="num">Đang xử lý</th><th class="num">Đã xong</th></tr>
            <% for (TechLoad l : load) { %>
            <tr><td><%= Html.esc(l.technician()) %></td><td class="num"><%= l.open() %></td><td class="num"><%= l.completed() %></td></tr>
            <% } %>
        </table>
    </div>
    <div class="card">
        <h3>Sản phẩm bán chạy</h3>
        <table>
            <tr><th>Sản phẩm</th><th class="num">SL</th><th class="num">Doanh thu</th></tr>
            <% for (TopProduct p : top) { %>
            <tr><td><%= Html.esc(p.name()) %></td><td class="num"><%= p.quantity() %></td><td class="num"><%= Html.money(p.revenue()) %></td></tr>
            <% } %>
        </table>
    </div>
    <div class="card">
        <h3>Hàng dưới mức tồn tối thiểu</h3>
        <% if (low.isEmpty()) { %><p class="muted">Không có.</p><% } else { %>
        <table>
            <tr><th>Mặt hàng</th><th class="num">Tồn</th><th class="num">Tối thiểu</th></tr>
            <% for (StockItem i : low) { %>
            <tr><td><%= "PART".equals(i.type()) ? "Linh kiện" : "Sản phẩm" %>: <%= Html.esc(i.name()) %></td>
                <td class="num"><%= i.stockQty() %></td><td class="num"><%= i.reorderLevel() %></td></tr>
            <% } %>
        </table>
        <% } %>
    </div>
</div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
