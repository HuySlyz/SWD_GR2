<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, com.warranty.model.Entities.*, com.warranty.model.Entities.Part"%>
<%
    String pageTitle = "Người bán & nhà cung cấp";
    String activeMenu = "partners";
    List<Seller> sellers = (List<Seller>) request.getAttribute("sellers");
    List<Supplier> suppliers = (List<Supplier>) request.getAttribute("suppliers");
    List<Product> products = (List<Product>) request.getAttribute("products");
    List<Part> parts = (List<Part>) request.getAttribute("parts");
    Supplier es = (Supplier) request.getAttribute("editSupplier");
    List<String> supplied = (List<String>) request.getAttribute("supplierItems");
    int editSellerId = (Integer) request.getAttribute("editSellerId");
    Seller esl = null;
    for (Seller s : sellers) { if (s.id() == editSellerId) esl = s; }
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Người bán &amp; nhà cung cấp</h2>
<div class="cols">
    <div>
        <form class="card" method="post" action="<%= ctx %>/admin/partners">
            <h3><%= esl == null ? "Thêm người bán" : "Sửa người bán" %></h3>
            <input type="hidden" name="action" value="saveSeller"><input type="hidden" name="id" value="<%= esl == null ? 0 : esl.id() %>">
            <label>Tên *</label><input name="name" value="<%= esl == null ? "" : Html.esc(esl.name()) %>" required>
            <div class="grid">
                <div><label>SĐT</label><input name="phone" value="<%= esl == null ? "" : Html.esc(esl.phone()) %>"></div>
                <div><label>Email</label><input name="email" value="<%= esl == null ? "" : Html.esc(esl.email()) %>"></div>
            </div>
            <div class="actions"><button class="btn green">Lưu</button>
                <% if (esl != null) { %><a class="btn gray" href="<%= ctx %>/admin/partners">Hủy</a><% } %></div>
        </form>
        <div class="card">
            <h3>Người bán</h3>
            <table>
                <tr><th>Tên</th><th>Liên hệ</th><th></th></tr>
                <% for (Seller s : sellers) { %>
                <tr><td><%= Html.esc(s.name()) %></td><td><%= Html.esc(s.phone()) %><br><span class="muted"><%= Html.esc(s.email()) %></span></td>
                    <td><a class="btn sm" href="<%= ctx %>/admin/partners?sellerId=<%= s.id() %>">Sửa</a>
                        <form class="inline" method="post" action="<%= ctx %>/admin/partners" onsubmit="return confirm('Xóa người bán?')">
                            <input type="hidden" name="action" value="deleteSeller"><input type="hidden" name="id" value="<%= s.id() %>">
                            <button class="btn sm red">Xóa</button></form></td></tr>
                <% } %>
            </table>
        </div>
    </div>

    <div>
        <form class="card" method="post" action="<%= ctx %>/admin/partners">
            <h3><%= es == null ? "Thêm nhà cung cấp" : "Sửa nhà cung cấp" %></h3>
            <input type="hidden" name="action" value="saveSupplier"><input type="hidden" name="id" value="<%= es == null ? 0 : es.id() %>">
            <label>Tên *</label><input name="name" value="<%= es == null ? "" : Html.esc(es.name()) %>" required>
            <div class="grid">
                <div><label>SĐT</label><input name="phone" value="<%= es == null ? "" : Html.esc(es.phone()) %>"></div>
                <div><label>Email</label><input name="email" value="<%= es == null ? "" : Html.esc(es.email()) %>"></div>
            </div>
            <label>Địa chỉ</label><input name="address" value="<%= es == null ? "" : Html.esc(es.address()) %>">
            <label>Mặt hàng cung cấp (supplier_products / supplier_parts)</label>
            <div class="check-list">
                <% for (Product p : products) { %>
                    <label class="inline"><input type="checkbox" name="productIds" value="<%= p.id() %>" <%= Html.checked(supplied.contains("PRODUCT:" + p.id())) %>>
                        SP: <%= Html.esc(p.name()) %></label><br>
                <% } %>
                <% for (Part p : parts) { %>
                    <label class="inline"><input type="checkbox" name="partIds" value="<%= p.id() %>" <%= Html.checked(supplied.contains("PART:" + p.id())) %>>
                        LK: <%= Html.esc(p.name()) %></label><br>
                <% } %>
            </div>
            <div class="actions"><button class="btn green">Lưu</button>
                <% if (es != null) { %><a class="btn gray" href="<%= ctx %>/admin/partners">Hủy</a><% } %></div>
        </form>
        <div class="card">
            <h3>Nhà cung cấp</h3>
            <table>
                <tr><th>Tên</th><th>Liên hệ</th><th></th></tr>
                <% for (Supplier s : suppliers) { %>
                <tr><td><%= Html.esc(s.name()) %><br><span class="muted"><%= Html.esc(s.address()) %></span></td>
                    <td><%= Html.esc(s.phone()) %><br><span class="muted"><%= Html.esc(s.email()) %></span></td>
                    <td><a class="btn sm" href="<%= ctx %>/admin/partners?supplierId=<%= s.id() %>">Sửa</a>
                        <form class="inline" method="post" action="<%= ctx %>/admin/partners" onsubmit="return confirm('Xóa nhà cung cấp?')">
                            <input type="hidden" name="action" value="deleteSupplier"><input type="hidden" name="id" value="<%= s.id() %>">
                            <button class="btn sm red">Xóa</button></form></td></tr>
                <% } %>
            </table>
        </div>
    </div>
</div>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
