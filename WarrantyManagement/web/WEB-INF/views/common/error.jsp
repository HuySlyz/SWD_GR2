<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<%
    String pageTitle = "Lỗi";
    String activeMenu = "";
    Integer code = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    String message = (String) request.getAttribute("jakarta.servlet.error.message");
    Throwable root = exception;
    while (root != null && root.getCause() != null) root = root.getCause();
    String title = code != null && code == 403 ? "Không có quyền truy cập"
            : code != null && code == 404 ? "Không tìm thấy trang" : "Đã xảy ra lỗi";
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>
<div class="card">
    <h2><%= title %></h2>
    <p><%= Html.esc(root != null ? root.getMessage() : message) %></p>
    <% if (root != null) { %>
    <p class="muted">Kiểm tra: dịch vụ SQL Server (MSSQLSERVER) đang chạy, đã chạy database/warranty_db.sql,
        thông tin kết nối trong DBContext.java, và mssql-jdbc.jar nằm trong web/WEB-INF/lib.</p>
    <% } %>
    <a class="btn gray" href="<%= ctx %>/login">Về trang chính</a>
</div>
<%@include file="/WEB-INF/views/common/_footer.jspf"%>
