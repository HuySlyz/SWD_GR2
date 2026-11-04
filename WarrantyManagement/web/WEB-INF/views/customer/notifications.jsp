<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.Notification"%>
<%
    String pageTitle = "Thông báo";
    String activeMenu = "notifications";
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Thông báo</h2>
<% if (notifications.isEmpty()) { %><p class="muted">Chưa có thông báo.</p><% } %>
<% for (Notification n : notifications) { %>
<div class="card" style="<%= n.read() ? "" : "border-left:4px solid #1d6fd1" %>">
    <h3><%= Html.esc(n.title()) %> <% if (!n.read()) { %><span class="badge NEW">Mới</span><% } %></h3>
    <p style="margin:0 0 6px"><%= Html.esc(n.content()) %></p>
    <span class="muted"><%= Html.date(n.sentAt()) %></span>
    <% if (n.ticketId() != null) { %> · <a href="<%= ctx %>/tickets?action=view&id=<%= n.ticketId() %>">Xem ticket</a><% } %>
</div>
<% } %>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
