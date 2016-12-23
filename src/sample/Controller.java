package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import operate.ChineseSegmentation;
import operate.MI;
import operate.Sort;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Controller {

    private DecimalFormat df = new DecimalFormat("00.00");

    @FXML
    private TextField tf_sum;
    @FXML
    private TextField tf_rate;
    @FXML
    private CheckBox cb_1,cb_2,cb_3;
    @FXML
    private BarChart<String,Number> barChart;
    @FXML
    private ListView<String> lv_1,lv_2,lv_3;

    @FXML
    private TextField tf_sumSpam;
    @FXML
    private CheckBox cb_4,cb_5,cb_6,cb_7,cb_8,cb_9;
    @FXML
    private ListView<String> lv_4;
    @FXML
    private LineChart<String,Number> lineChart;


    private final static String model1 = "多项式";
    private final static String model2 = "贝努力";
    private final static String model3 = "Knn模型";

    private int _lastSpamSum;    //用于个性化判断是否重新更新 ListView
    private XYChart.Series _series = new XYChart.Series();   //用于个性化动态更新数据
    private int _count;          //用于个性化次数
    private int[] classCount = new int[7];      //用于个性化记录 各个类别的个数
    private boolean isRecount;

    @FXML
    @SuppressWarnings("unchecked")
    private void handleBtnTestAction1() throws Exception {

        int sum = Integer.parseInt(tf_sum.getText().trim());
        String strRate = tf_rate.getText().trim();
        double rate = Double.parseDouble(strRate.split("/")[0]) / (Integer.parseInt(strRate.split("/")[1]) + Double.parseDouble(strRate.split("/")[0]));
        boolean isM1 = cb_1.isSelected();       //是否测试多项式贝叶斯模型
        boolean isM2 = cb_2.isSelected();       //是否测试贝努力贝叶斯模型
        boolean isM3 = cb_3.isSelected();       //是否测试Knn模型
        List<String> messageList = getMessage((int)(sum * rate),"message");
        List<String> spamList = getMessage(sum - (int)(sum * rate),"spamMessage");

        double[][] results = new double[3][4];      //用于保存 评测结果

        //利用多项式模型
        if (isM1) {
            List<String> falseMessageList = new ArrayList<>();      //把正常短信判为垃圾短信
            List<String> wordsMessageList = new ArrayList<>();
            Sort.countSpamByNB(messageList, falseMessageList, wordsMessageList,13, 9 / 10.);
            List<String> falseSpamList = new ArrayList<>();         //把垃圾短信判为正常短信
            List<String> wordsSpamList = new ArrayList<>();
            Sort.countSpamByNB(spamList, falseSpamList, wordsSpamList, 13, 9 / 10.);
            List<String> resultList = new ArrayList<>();
            for (int i = 0;i < falseMessageList.size();++i) {
                resultList.add(falseMessageList.get(i) + "\n\t分词为：" + wordsMessageList.get(i) + "\n");
            }
            for (int i = 0;i < falseSpamList.size();++i) {
                resultList.add(falseSpamList.get(i) + "\n\t分词为：" + wordsSpamList.get(i) + "\n");
            }
            ObservableList<String> items = FXCollections.observableArrayList(resultList);
            lv_1.setItems(items);
            results[0] = getReviews(messageList.size(),spamList.size(),falseMessageList.size(),falseSpamList.size());
        }

        //利用贝努力模型
        if (isM2){
            List<String> ctList = new ArrayList<>();
            MI.getCT(300,ctList);
            Sort.initBnl(ctList);
            List<String> falseMessageList = new ArrayList<>();      //把正常短信判为垃圾短信
            List<String> wordsMessageList = new ArrayList<>();
            for(String message : messageList) {
                List<String> wordList = new ArrayList<>();
                boolean isSpam = Sort.BnlSort(message,ctList,wordList);
                if (isSpam){
                    falseMessageList.add(message);
                    wordsMessageList.add(wordList.toString());
                }
            }
            List<String> falseSpamList = new ArrayList<>();         //把垃圾短信判为正常短信
            List<String> wordsSpamList = new ArrayList<>();
            for(String spam : spamList){
                List<String> wordList = new ArrayList<>();
                boolean isSpam = Sort.BnlSort(spam,ctList,wordList);
                if (!isSpam){
                    falseSpamList.add(spam);
                    wordsSpamList.add(wordList.toString());
                }
            }
            List<String> resultList = new ArrayList<>();
            for (int i = 0;i < falseMessageList.size();++i) {
                resultList.add(falseMessageList.get(i) + "\n\t分词为：" + wordsMessageList.get(i) + "\n");
            }
            for (int i = 0;i < falseSpamList.size();++i) {
                resultList.add(falseSpamList.get(i) + "\n\t分词为：" + wordsSpamList.get(i) + "\n");
            }
            ObservableList<String> items = FXCollections.observableArrayList(resultList);
            lv_2.setItems(items);
            results[1] = getReviews(messageList.size(),spamList.size(),falseMessageList.size(),falseSpamList.size());
        }

        //利用 Knn 模型
        if (isM3){
            List<String> falseMessageList = new ArrayList<>();      //把正常短信判为垃圾短信
            List<String> wordsMessageList = new ArrayList<>();
            boolean[] messageResult = Sort.KnnSort(10,messageList);
            for(int i = 0;i < messageResult.length;++i){
                if (messageResult[i]) {
                    falseMessageList.add(messageList.get(i));
                    wordsMessageList.add(ChineseSegmentation.result(messageList.get(i).split("\t")[2]).toString());
                }
            }
            List<String> falseSpamList = new ArrayList<>();         //把垃圾短信判为正常短信
            List<String> wordsSpamList = new ArrayList<>();
            boolean[] spamResult = Sort.KnnSort(10,spamList);
            for(int i = 0;i < spamResult.length;++i){
                if (!spamResult[i]){
                    falseSpamList.add(spamList.get(i));
                    wordsSpamList.add(ChineseSegmentation.result(spamList.get(i).split("\t")[2]).toString());
                }
            }
            List<String> resultList = new ArrayList<>();
            for (int i = 0;i < falseMessageList.size();++i) {
                resultList.add(falseMessageList.get(i) + "\n\t分词为：" + wordsMessageList.get(i) + "\n");
            }
            for (int i = 0;i < falseSpamList.size();++i) {
                resultList.add(falseSpamList.get(i) + "\n\t分词为：" + wordsSpamList.get(i) + "\n");
            }
            ObservableList<String> items = FXCollections.observableArrayList(resultList);
            lv_3.setItems(items);
            results[2] = getReviews(messageList.size(),spamList.size(),falseMessageList.size(),falseSpamList.size());
        }

        //画相应的柱状图
        barChart.getData().clear();
        XYChart.Series series1 = new XYChart.Series();
        series1.setName("p1");
        series1.getData().add(new XYChart.Data(model1,results[0][0]));
        series1.getData().add(new XYChart.Data(model2,results[1][0]));
        series1.getData().add(new XYChart.Data(model3,results[2][0]));

        barChart.getData().add(series1);

        XYChart.Series series2 = new XYChart.Series();
        series2.setName("r1");
        series2.getData().add(new XYChart.Data(model1,results[0][1]));
        series2.getData().add(new XYChart.Data(model2,results[1][1]));
        series2.getData().add(new XYChart.Data(model3,results[2][1]));

        barChart.getData().add(series2);

        XYChart.Series series3 = new XYChart.Series();
        series3.setName("p2");
        series3.getData().add(new XYChart.Data(model1,results[0][2]));
        series3.getData().add(new XYChart.Data(model2,results[1][2]));
        series3.getData().add(new XYChart.Data(model3,results[2][2]));

        barChart.getData().add(series3);

        XYChart.Series series4 = new XYChart.Series();
        series4.setName("r2");
        series4.getData().add(new XYChart.Data(model1,results[0][3]));
        series4.getData().add(new XYChart.Data(model2,results[1][3]));
        series4.getData().add(new XYChart.Data(model3,results[2][3]));

        barChart.setAnimated(false);
        barChart.getData().add(series4);

    }

    /**
     * 获取一定的 短信 测试集
     * @param fileName 填入 message or spamMessage
     */
    private List<String> getMessage(int num,String fileName){
        List<String> result = new ArrayList<>();
        File file = new File("");
        try(BufferedReader bfReader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath() + "\\数据\\测试集\\" + fileName + ".txt")))){
            String temp;
                int count = 0;
                while (count++ < num && (temp = bfReader.readLine()) != null){
                result.add(temp);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取 正确率 与 召回率
     * @param messageNum 测试集 正常短信数目
     * @param spamNum 测试集 垃圾短信数目
     * @param falseMessageNum 错判为垃圾短信的数目
     * @param falseSpamNum 错判为正常短信的数目
     * @return //double[0] 正常正确率 double[1] 正常召回率 double[2] 垃圾正确率 double[3] 垃圾召回率
     */
    private double[] getReviews(int messageNum,int spamNum,int falseMessageNum,int falseSpamNum){
        double[] results = new double[4];       //double[0] 正常正确率 double[1] 正常召回率 double[2] 垃圾正确率 double[3] 垃圾召回率
        results[0] = Double.parseDouble(df.format(((double)(messageNum - falseMessageNum)) / (messageNum - falseMessageNum + falseSpamNum) * 100));
        results[1] = Double.parseDouble(df.format(((double)(messageNum - falseMessageNum)) / messageNum * 100));
        results[2] = Double.parseDouble(df.format(((double)(spamNum - falseSpamNum)) / (spamNum - falseSpamNum + falseMessageNum) * 100));
        results[3] = Double.parseDouble(df.format(((double)(spamNum - falseSpamNum)) / spamNum * 100));
        return results;
    }

    //个性化页面
    @FXML
    @SuppressWarnings("unchecked")
    private void handleBtnTestAction2(){
        int sumSpam = Integer.parseInt(tf_sumSpam.getText().trim());
        int[] results = new int[sumSpam];
        List<String> spamList = getMessage(sumSpam,"spamMessage");
        List<String> resultList = new ArrayList<>();

        if (_lastSpamSum != sumSpam || isRecount) {
            String[] paths = new String[6];
            File file = new File("");
            String path = file.getAbsolutePath() + "\\数据\\6类训练集\\";
            paths[0] = path + "贷款诈骗_1.txt";
            paths[1] = path + "二手车推销信息_2.txt";
            paths[2] = path + "房地产推销信息_3.txt";
            paths[3] = path + "服务业广告推送信息_4.txt";
            paths[4] = path + "冒充银行扣款类、提供姓名及卡号转账诈骗_5.txt";
            paths[5] = path + "招聘信息_6.txt";
            Sort.initPrioriP(paths);
            for (int i = 0; i < results.length; ++i) {
                List<String> wordList = new ArrayList<>();
                results[i] = Sort.personalization(spamList.get(i).split("\t")[2], wordList);
                classCount[results[i]]++;
                resultList.add(spamList.get(i) + "\n\t分词为：" + wordList.toString() + "\n所属类别：" + results[i] + "\n");
            }
            lv_4.setItems(FXCollections.observableList(resultList));
            isRecount = false;
        }

        int yourSpamSum = sumSpam;
        if (cb_4.isSelected())
            yourSpamSum -= classCount[1];
        if (cb_5.isSelected())
            yourSpamSum -= classCount[2];
        if (cb_6.isSelected())
            yourSpamSum -= classCount[3];
        if (cb_7.isSelected())
            yourSpamSum -= classCount[4];
        if (cb_8.isSelected())
            yourSpamSum -= classCount[5];
        if (cb_9.isSelected())
            yourSpamSum -= classCount[6];

        //画 LineChart
        _series.setName("垃圾短信条数");
        _series.getData().add(new XYChart.Data(String.valueOf(_count++), yourSpamSum));
        lineChart.getData().add(_series);

        _lastSpamSum = sumSpam;
    }

    //清空 选择框、lineChart
    @FXML
    private void handleBtnClearAction(){
        cb_4.setSelected(false);
        cb_5.setSelected(false);
        cb_6.setSelected(false);
        cb_7.setSelected(false);
        cb_8.setSelected(false);
        cb_9.setSelected(false);

        lineChart.getData().clear();

        _count = 1;
        _series.getData().clear();
        Arrays.fill(classCount,0);
        isRecount = true;
    }
}
