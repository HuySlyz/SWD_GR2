package com.warranty.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Ket noi SQL Server (instance mac dinh MSSQLSERVER, cong 1433).
 * Tai khoan warranty_user duoc tao boi database/warranty_db.sql.
 */
public final class DBContext {

    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=warranty_db;"
            + "encrypt=true;trustServerCertificate=true";
    private static final String USER = "warranty_user";
    private static final String PASSWORD = "Warranty@123";

    /** Ma loi SQL Server khi vi pham khoa ngoai. */
    public static final int FK_VIOLATION = 547;
    /** Ma loi SQL Server khi trung khoa UNIQUE. */
    public static final int UNIQUE_VIOLATION = 2627;
    public static final int UNIQUE_INDEX_VIOLATION = 2601;

    private DBContext() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tìm thấy SQL Server JDBC driver trong web/WEB-INF/lib", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
