<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.RepairPart"%>
<%
    String pageTitle = "Yêu cầu linh kiện";
    String activeMenu = "requests";
    List<RepairPart> requests = (List<RepairPart>) request.getAttribute("requests");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Yêu cầu linh kiện từ kỹ thuật viên</h2>
<p class="muted">Linh kiện đã được giữ (trừ tồn kho) khi kỹ thuật viên gửi yêu cầu. Giao cho kỹ thuật viên hoặc từ chối để hoàn lại tồn.</p>
<div class="table-wrap"><table>
    <tr><th>Ticket</th><th>Kỹ thuật viên</th><th>Linh kiện</th><th class="num">SL</th><th>Yêu cầu lúc</th><th>Trạng thái</th><th></th></tr>
    <% if (requests.isEmpty()) { %><tr><td colspan="7" class="muted">Không có yêu cầu.</td></tr><% } %>
    <% for (RepairPart r : requests) { %>
    <tr class="<%= r.delivered() ? "muted-row" : "" %>">
        <td><%= Html.esc(r.ticketCode()) %></td>
        <td><%= Html.esc(r.technicianName()) %></td>
        <td><%= Html.esc(r.partCode()) %> – <%= Html.esc(r.partName()) %></td>
        <td class="num"><%= r.quantity() %></td>
        <td><%= Html.date(r.requestedAt()) %></td>
        <td><%= r.delivered() ? "Đã giao bởi " + Html.esc(r.issuedByName()) + " – " + Html.date(r.issuedAt())
                : "<span class='badge PENDING'>Chờ giao</span>" %></td>
        <td>
            <% if (!r.delivered()) { %>
            <form class="inline" method="post" action="<%= ctx %>/inventory/requests">
                <input type="hidden" name="repairLogId" value="<%= r.repairLogId() %>"><input type="hidden" name="partId" value="<%= r.partId() %>">
                <button class="btn sm green" name="action" value="deliver">Giao cho KTV</button>
                <button class="btn sm red" name="action" value="reject" onclick="return confirm('Từ chối yêu cầu này?')">Từ chối</button>
            </form>
            <% } %>
        </td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
