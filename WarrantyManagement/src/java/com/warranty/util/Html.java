package com.warranty.util;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Ham tien ich dung trong JSP: chong XSS, dinh dang ngay va tien VND. */
public final class Html {

    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter VN_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Html() {
    }

    public static String esc(Object o) {
        if (o == null) {
            return "";
        }
        String s = o.toString();
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String url(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static String date(LocalDate d) {
        return d == null ? "" : d.format(VN_DATE);
    }

    public static String date(LocalDateTime d) {
        return d == null ? "" : d.format(VN_DATE_TIME);
    }

    public static String money(BigDecimal v) {
        NumberFormat f = NumberFormat.getInstance(new Locale("vi", "VN"));
        f.setMaximumFractionDigits(0);
        return f.format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    /** So tien dang nhap lieu (khong phan cach), vd 1290000. */
    public static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    public static String selected(boolean b) {
        return b ? "selected" : "";
    }

    public static String checked(boolean b) {
        return b ? "checked" : "";
    }
}
