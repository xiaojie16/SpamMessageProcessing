package operate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * @author by kissx on 2016/11/19.
 */
public class Method {

    /**
     * 截取某文本的前 size 行
     * @param size 大小
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     */
    private static void cutFile(int size,int start,String sourceFile,String targetFile){
        try(BufferedReader bfReader = new BufferedReader(new FileReader(sourceFile));
            BufferedWriter bfWriter = new BufferedWriter(new FileWriter(targetFile))){
            String temp;
            int count = 1;
            while (count < start && bfReader.readLine() != null ){
                ++count;
            }
            while ((temp = bfReader.readLine()) != null && count < size + start){
                bfWriter.write(temp + "\n");
                ++count;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){

    }

}
