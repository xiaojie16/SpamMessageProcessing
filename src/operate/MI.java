package operate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * @author by kissx on 2016/11/19.
 * 特征选择------服务于贝努力模型
 */
public class MI {

    /**
     * 获取排名前 size 的特征词
     * @param size 选取特征词的数目
     * @param ctList 将特征词保存在一集合中
     * @param fileName 将特征词保存在某文件中
     */
    public static void getCT(int size, List<String> ctList,String fileName){
        String sql = "select word from 统计_1 order by mi desc limit " + size;

        DBHelper dbHelper = DBHelper.getDBHelper("贝努力模型");
        Connection conn = dbHelper.getConn();
        try (BufferedWriter bfWriter = new BufferedWriter(new FileWriter(fileName))){
            PreparedStatement statement = conn.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                String word = resultSet.getString(1);
                bfWriter.write(word + "\n");
                ctList.add(word);
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 重载 getCT
     */
    public static void getCT(int size, List<String> ctList){
        String sql = "select word from 统计_1 order by mi desc limit " + size;

        DBHelper dbHelper = DBHelper.getDBHelper("贝努力模型");
        Connection conn = dbHelper.getConn();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                String word = resultSet.getString(1);
                ctList.add(word);
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 重载 getCT
     */
    public static void getCT(int size,String fileName){
        String sql = "select word from 统计_1 order by mi desc limit " + size;

        DBHelper dbHelper = DBHelper.getDBHelper("贝努力模型");
        Connection conn = dbHelper.getConn();
        try (BufferedWriter bfWriter = new BufferedWriter(new FileWriter(fileName))){
            PreparedStatement statement = conn.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                String word = resultSet.getString(1);
                bfWriter.write(word + "\n");
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
//        List<String> ctList = new ArrayList<>();
//        getCT(100,ctList);
//        for(String word : ctList)
//            System.out.println(word);
    }

}
