<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%
    String pageTitle = "Tài khoản";
    String activeMenu = "users";
    List<com.warranty.model.Entities.User> users = (List<com.warranty.model.Entities.User>) request.getAttribute("users");
    com.warranty.model.Entities.User e = (com.warranty.model.Entities.User) request.getAttribute("editing");
    com.warranty.model.Role roleFilter = (com.warranty.model.Role) request.getAttribute("role");
    String keyword = (String) request.getAttribute("keyword");
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Quản lý tài khoản (nhân viên &amp; khách hàng)</h2>
<form class="card" method="post" action="<%= ctx %>/admin/users">
    <h3><%= e == null ? "Tạo tài khoản" : "Sửa tài khoản " + Html.esc(e.username()) %></h3>
    <input type="hidden" name="id" value="<%= e == null ? 0 : e.id() %>">
    <div class="grid">
        <div><label>Vai trò *</label>
            <select name="role" <%= e != null ? "disabled" : "required" %>>
                <% for (Role r : Role.values()) { %>
                    <option value="<%= r %>" <%= Html.selected(e != null && e.role() == r) %>><%= r.getLabel() %></option>
                <% } %>
            </select></div>
        <div><label>Tên đăng nhập *</label>
            <input name="username" value="<%= e == null ? "" : Html.esc(e.username()) %>" <%= e != null ? "disabled" : "required" %>></div>
        <div><label><%= e == null ? "Mật khẩu *" : "Mật khẩu mới (bỏ trống nếu giữ nguyên)" %></label>
            <input type="password" name="password" <%= e == null ? "required minlength='6'" : "" %>></div>
        <div><label>Họ tên *</label><input name="fullName" value="<%= e == null ? "" : Html.esc(e.fullName()) %>" required></div>
        <div><label>Email (đăng nhập) *</label><input type="email" name="email" value="<%= e == null ? "" : Html.esc(e.email()) %>" required></div>
        <div><label>Số điện thoại</label><input name="phone" value="<%= e == null ? "" : Html.esc(e.phone()) %>"></div>
        <div><label>Địa chỉ (khách hàng)</label><input name="address" value="<%= e == null ? "" : Html.esc(e.address()) %>"></div>
    </div>
    <div class="actions">
        <button class="btn green">Lưu</button>
        <% if (e != null) { %><a class="btn gray" href="<%= ctx %>/admin/users">Hủy</a><% } %>
    </div>
</form>

<form class="search" method="get" action="<%= ctx %>/admin/users">
    <input name="q" value="<%= Html.esc(keyword) %>" placeholder="Tên / tên đăng nhập / email / SĐT">
    <select name="role">
        <option value="">-- Tất cả vai trò --</option>
        <% for (Role r : Role.values()) { %><option value="<%= r %>" <%= Html.selected(r == roleFilter) %>><%= r.getLabel() %></option><% } %>
    </select>
    <button class="btn">Lọc</button>
</form>
<div class="table-wrap"><table>
    <tr><th>Tên đăng nhập</th><th>Họ tên</th><th>Vai trò</th><th>Email</th><th>SĐT</th><th>Trạng thái</th><th></th></tr>
    <% for (com.warranty.model.Entities.User u : users) { %>
    <tr class="<%= u.active() ? "" : "muted-row" %>">
        <td><%= Html.esc(u.username()) %></td>
        <td><%= Html.esc(u.fullName()) %></td>
        <td><%= u.role().getLabel() %></td>
        <td><%= Html.esc(u.email()) %></td>
        <td><%= Html.esc(u.phone()) %></td>
        <td><span class="badge <%= u.active() ? "ok" : "err" %>"><%= u.active() ? "Hoạt động" : "Vô hiệu" %></span></td>
        <td>
            <a class="btn sm" href="<%= ctx %>/admin/users?id=<%= u.id() %>">Sửa</a>
            <form class="inline" method="post" action="<%= ctx %>/admin/users">
                <input type="hidden" name="action" value="toggle"><input type="hidden" name="id" value="<%= u.id() %>">
                <input type="hidden" name="active" value="<%= !u.active() %>">
                <button class="btn sm <%= u.active() ? "orange" : "green" %>"><%= u.active() ? "Vô hiệu hóa" : "Kích hoạt" %></button>
            </form>
            <form class="inline" method="post" action="<%= ctx %>/admin/users" onsubmit="return confirm('Xóa vĩnh viễn tài khoản này?')">
                <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%= u.id() %>">
                <button class="btn sm red">Xóa</button>
            </form>
        </td>
    </tr>
    <% } %>
</table></div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
