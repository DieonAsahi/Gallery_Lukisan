//package object;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//
///**
// *
// * @author athaw
// */
//public class config {
//        public static Connection configDB() throws SQLException {
//        try {
//            String url = "jdbc:mysql://localhost:3306/uas_pemkom";
//            String user = "root";
//            String password = "";
//
//            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
//            return DriverManager.getConnection(url, user, password);
//        } catch (SQLException e) {
//            System.out.println("Connection failed: " + e.getMessage());
//            throw e;
//        }
//    }
//}
//
//
//
package object;

import java.sql.Connection;
import java.sql.DriverManager;

public class config {
    private static Connection mysqlconfig;

    public static Connection configDB() throws Exception {
        if (mysqlconfig == null || mysqlconfig.isClosed()) {
            String url = "jdbc:mysql://localhost:3306/uas_pemkom"; // ganti sesuai DB
            String user = "root"; // ganti sesuai user DB
            String password = ""; // ganti sesuai password DB
            Class.forName("com.mysql.cj.jdbc.Driver");
            mysqlconfig = DriverManager.getConnection(url, user, password);
        }
        return mysqlconfig;
    }
}
