<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String pageTitle = "Đăng nhập";
    String activeMenu = "login";
    String loginError = (String) request.getAttribute("loginError");
    String email = (String) request.getAttribute("email");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<div class="card login">
    <h2>Đăng nhập</h2>
    <% if (loginError != null) { %><div class="alert err"><%= Html.esc(loginError) %></div><% } %>
    <form method="post" action="<%= ctx %>/login">
        <label>Email</label>
        <input type="email" name="email" value="<%= Html.esc(email) %>" required autofocus>
        <label>Mật khẩu</label>
        <input type="password" name="password" required>
        <div class="actions"><button class="btn">Đăng nhập</button></div>
    </form>
    <p class="muted" style="margin-top:14px">Chưa có tài khoản? Liên hệ cửa hàng để được tạo tài khoản.
        Bạn vẫn có thể <a href="<%= ctx %>/lookup">tra cứu bảo hành</a> không cần đăng nhập.</p>
</div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
