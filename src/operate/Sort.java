package operate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * @author by kissx on 2016/12/10.
 */
public class Sort {

    //用于 贝努力模型
    private static double[] p1;
    private static double[] p2;

    //用于 个性化
    private static double[] prioriP;

    /**
     * 多项式模型
     * @param testList 测试集
     * @param falseList 判错的短信集
     * @param wordsList 判错的短信集的分词结果
     * @param i 利用哪一个训练集
     * @param rate 训练集 非占所有的比率
     * @return 返回垃圾短信的条数
     */
    public static int countSpamByNB(List<String> testList,List<String> falseList,List<String> wordsList,int i,double rate){
        int count = 0;
        boolean isSpam = "1".equals(testList.get(0).split("\t")[1]);
        DBHelper dbHelper = DBHelper.getDBHelper("多项式模型");
        try {
            for (String message : testList) {
                    double p1 = 0;
                    double p2 = 0;
                    List<String> wordList = ChineseSegmentation.result(message.split("\t")[2]);
                    /*
                     *3.27      1
                     * 修改这里是为了避免对短短信处理时，只因一词的出现使其误报为垃圾短信
                     */
                    int count_keyword = 0;

                    for (String word : wordList) {
                        String sql = "SELECT p1,p2 FROM 结果_" + i + " WHERE word = \'" + word + "\'";
                        ResultSet ret = dbHelper.getConn().prepareStatement(sql).executeQuery();
                        while (ret.next()) {
                            p1 += Math.log(Double.parseDouble(ret.getString(1)));
                            p2 += Math.log(Double.parseDouble(ret.getString(2)));
                            //此处添加一个变量用来计该短信在词库中找到了几个词项；
                            //3.27     1
                            count_keyword++;
                        }
                    }
                    p1 = p1 + Math.log(rate);
                    p2 = p2 + Math.log(1 - rate);

                    //3.27   1
                    //TODO　12.13 为了提高垃圾短信正确率 这里将分词得来的词较少的直接判为 非垃圾短信 效果不明显
                    if (count_keyword <= 0) {
                        p1 = 1;
                        p2 = 0;             //TODO 对于分词得到为空直接判为 改为 p1 = 1 垃圾短信 12-11 结果：与下面效果一样 估计在非垃圾短信中也有 分词为空的
                    }
                    //TODO 下面的等式加了 == （即改为 p1<=p2） 12-11 效果不明显
                    if (p1 < p2) {
                        count++;
                        if (!isSpam){
                            falseList.add(message);
                            wordsList.add(wordList.toString());
                        }
                    } else {
                        if (isSpam){
                            falseList.add(message);
                            wordsList.add(wordList.toString());
                        }
                    }
                }
        }catch (Exception e){
            e.printStackTrace();
        }
        return count;
    }

    /**
     * 初始化 贝努力模型
     * @param ctList
     */
    public static void initBnl(List<String> ctList){
        p1 = new double[ctList.size()];
        p2 = new double[ctList.size()];
        int times = 0;
        DBHelper dbHelper = DBHelper.getDBHelper("贝努力模型");
        Connection conn = dbHelper.getConn();
        for(String word : ctList){
            String sql = "select p1,p2 from 结果_1 where word = '" + word + "'";
            try {
                PreparedStatement statement = conn.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()){
                    p1[times] = resultSet.getDouble(1);
                    p2[times] = resultSet.getDouble(2);
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }
            ++times;
        }
    }

    /**
     * 贝努力模型
     *  判断一条短信是不是垃圾短信，若返回 true 则表名是
     * @param message 短信
     * @param ctList 特征词集合
     * @param wordList 分词结果
     * @return TRUE 表名为垃圾短信
     */
    public static boolean BnlSort(String message,List<String> ctList,List<String> wordList){
        boolean result = false;
        double p1_sum = Math.log(0.9);
        double p2_sum = Math.log(0.1);

        try {
            wordList.addAll(ChineseSegmentation.result(message.split("\t")[2]));
            Set<String> wordSet = new HashSet<>(wordList);
            int count = 0;
            for (String word : ctList){
                if (wordSet.contains(word)){
                    p1_sum += Math.log(p1[count]);
                    p2_sum += Math.log(p2[count]);
                }else{
                    p1_sum += Math.log(1 - p1[count]);
                    p2_sum += Math.log(1 - p2[count]);
                }
                ++count;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        if (p1_sum < p2_sum)
            result = true;
        return result;
    }


    /**
     * @param k knn 中 k 值(TODO 由于疏忽组数等于 k = 10)
     * @return 判断n条短信是不是垃圾短信，若返回 true 则表名是
     */
    public static boolean[] KnnSort(int k,List<String> messageList){
        boolean[] results = new boolean[messageList.size()];
        double[][] tables = new double[messageList.size()][k];
        for(int i = 1;i <= k;++i){
            double[] lengthArr = distance(i,messageList);
            for(int j = 0;j < lengthArr.length;++j)
                tables[j][i-1] = lengthArr[j];
        }
        for(int i = 0;i < tables.length;++i){
            double max = tables[i][0];
            int index = 0;
            for(int j = 1;j < k;++j){
                if (tables[i][j] > max){
                    max = tables[i][j];
                    index = j;
                }
            }
            results[i] = (index > 8);
        }
        return results;
    }

    /**
     * 用于 knn 模型
     * @param a 与 i 组的距离
     */
    private static double[] distance(int a,List<String> messageList){
        double[] lengthArr = new double[messageList.size()];
        double aLength = 0;
        Map<String,Integer> map = new HashMap<>();
        File file = new File("");
        try(BufferedReader bfReader = new BufferedReader(new FileReader(file.getAbsolutePath() + "\\数据\\Knn\\" + a + "_词集.txt"))){
            String temp;
            while ((temp = bfReader.readLine()) != null){
                String word = temp.split("\t")[0];
                Integer count = Integer.parseInt(temp.split("\t")[1]);
                map.put(word,count);
                aLength += (count * count);
            }
            aLength = Math.sqrt(aLength);
        }catch (Exception e){
            e.printStackTrace();
        }
        int i = 0;
        for(String message : messageList){
            Map<String,Integer> wordMap = new HashMap<>();
            double bLength = 0;
            int numerator = 0;      //分子
            try {
                List<String> wordList = ChineseSegmentation.result(message.split("\t")[2]);
                for(String word : wordList){
                    Integer count = wordMap.get(word);
                    if(count != null){
                        wordMap.put(word,++count);
                    }else
                        wordMap.put(word,1);
                }
                Set<String> keys = wordMap.keySet();
                for(String key : keys){
                    int aCount = map.get(key) == null ? 0 : map.get(key);
                    int bCount = wordMap.get(key);
                    numerator += (aCount * bCount);
                    bLength += (bCount * bCount);
                }
                bLength = Math.sqrt(bLength);
                lengthArr[i] = numerator / (aLength * bLength);
                ++i;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return lengthArr;
    }

    /**
     * 使用个性化前先调用此方法，来获取先验概率
     * @param paths 测试集所在的路径
     */
    public static void initPrioriP(String... paths){
        prioriP = new double[paths.length];     //先验概率
        int[] numOfMessage = new int[paths.length];
        int times = 0;
        for(String path : paths){
            int count = 0;
            try(BufferedReader bfReader = new BufferedReader(new FileReader(path))){
                while (bfReader.readLine() != null)
                    ++count;
                numOfMessage[times] = count;
            }catch (Exception e){
                throw new RuntimeException(e);
            }
            ++times;
        }
        int sum = 0;
        for(int num : numOfMessage)
            sum += num;
        for(int i = 0;i < paths.length;++i)
            prioriP[i] = (double)numOfMessage[i] / sum;
    }

    /**
     * 执行前应该先调用 initPrioriP ，判断一条短信属于哪一类别
     * @param message 一条短信
     * @param wordList 将短信分词结果添加到指定 List
     * @return 类别代号
     */
    public static int personalization(String message,List<String> wordList){
        int mark = 0;
        int sNumber = prioriP.length;
        double[] pArray = new double[sNumber];
        DBHelper dbHelper = DBHelper.getDBHelper("科研之个性化");
        Connection conn = dbHelper.getConn();
        String sql = "select ";
        for(int i = 1;i <= sNumber;++i){
            if (i != sNumber)
                sql += "p"+i+",";
            else
                sql += "p"+i+" from 结果 ";
        }
        try {
            List<String> words = ChineseSegmentation.result(message);
            wordList.addAll(words);
            for(String word : words){
                String temp = sql + "where word like '" + word + "'";
                PreparedStatement pStatement = conn.prepareStatement(temp);
                ResultSet resultSet = pStatement.executeQuery();
                while (resultSet.next()){
                    for (int i = 0;i < sNumber;++i)
                        pArray[i] += Math.log(Double.parseDouble(resultSet.getString(i + 1)));
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        for(int i = 0;i < sNumber;++i)
            pArray[i] += Math.log(prioriP[i]);
        double max = pArray[0];
        for (int i = 1;i < pArray.length;++i){
            if(pArray[i] > max) {
                max = pArray[i];
                mark = i;
            }
        }
        return ++mark;
    }

}
