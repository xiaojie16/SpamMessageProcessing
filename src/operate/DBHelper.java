package operate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author by kissx on 2016/11/11.
 * 数据库帮组类
 * 这里使用了 ---- 单例模式
 */
public class DBHelper {

    private Connection conn;
    private static DBHelper dbHelper;

    private DBHelper(String dbName){
        String url = "jdbc:mysql://127.0.0.1/" + dbName +"?useUnicode=true&characterEncoding=utf-8&useSSL=false";
        String userName = "root";
        String passWord = "111111";
        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url,userName,passWord);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 返回单例 DBHelper
     */
    public static DBHelper getDBHelper(String dbName){
        if (dbHelper != null)
            dbHelper.close();

        return dbHelper = new DBHelper(dbName);
    }

    /**
     * @return 返回一个连接
     */
    public Connection getConn() {
        return conn;
    }

    /**
     * 关闭资源
     */
    public void close(){
        if (conn != null)
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

}
