<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.List, java.time.LocalDate, com.warranty.model.Entities.Warranty, com.warranty.model.Codes.Channel"%>
<%
    String pageTitle = "Tạo ticket bảo hành";
    String serial = (String) request.getAttribute("serial");
    List<Warranty> myWarranties = (List<Warranty>) request.getAttribute("myWarranties");
    String activeMenu = myWarranties != null ? "tickets" : "ticket-new";
    LocalDate today = LocalDate.now();
%>
<%@include file="/WEB-INF/views/common/_header.jspf"%>

<h2>Gửi yêu cầu bảo hành</h2>
<form class="card" method="post" action="<%= ctx %>/tickets">
    <input type="hidden" name="action" value="create">
    <div class="grid">
        <div>
            <% if (myWarranties != null) { %>
                <label>Sản phẩm của tôi *</label>
                <select name="serial" required>
                    <option value="">-- Chọn sản phẩm --</option>
                    <% for (Warranty w : myWarranties) { boolean valid = w.isValidOn(today); %>
                        <option value="<%= Html.esc(w.serialNumber()) %>" <%= Html.selected(w.serialNumber().equals(serial)) %>>
                            <%= Html.esc(w.productName()) %> – <%= Html.esc(w.serialNumber()) %>
                            (<%= valid ? "BH đến " + Html.date(w.endDate()) : "hết hạn" %>)</option>
                    <% } %>
                </select>
            <% } else { %>
                <label>Số serial / mã bảo hành *</label>
                <input name="serial" value="<%= Html.esc(serial) %>" required>
                <label>Kênh tiếp nhận</label>
                <select name="channel">
                    <% for (Channel ch : Channel.values()) { %>
                        <option value="<%= ch %>" <%= Html.selected(ch == Channel.STORE) %>><%= ch.getLabel() %></option>
                    <% } %>
                </select>
            <% } %>
        </div>
        <div>
            <label>Mô tả lỗi *</label>
            <textarea name="issue" required placeholder="VD: không lên hình, treo máy, kêu rè, quạt không quay..."></textarea>
        </div>
    </div>
    <label class="inline"><input type="checkbox" name="acceptPaidRepair">
        Nếu sản phẩm hết hạn / không hợp lệ bảo hành, tôi đồng ý <b>sửa chữa có phí</b></label>
    <p class="muted">Hệ thống kiểm tra tình trạng và ngày hết hạn bảo hành theo serial. Còn hạn: miễn phí. Hết hạn: chỉ tạo
        ticket khi đồng ý sửa có phí, phí do kỹ thuật viên báo sau khi sửa.</p>
    <div class="actions">
        <button class="btn green">Gửi yêu cầu</button>
        <a class="btn gray" href="<%= ctx %>/tickets">Hủy</a>
    </div>
</form>

<%@include file="/WEB-INF/views/common/_footer.jspf"%>
