package operate;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author by kissx on 2016/11/11.
 * 调用张华平老师的分词
 */
public class ChineseSegmentation {

    public interface CLibrary extends Library {
        CLibrary Instance = (CLibrary) Native.loadLibrary("F:\\科研训练\\演示系统\\NLPIR", CLibrary.class);
        boolean NLPIR_Init(byte[] sDataPath, int encoding, byte[] sLicenceCode);
        String NLPIR_ParagraphProcess(String sSrc, int bPOSTagged);
        String NLPIR_GetKeyWords(String sLine, int nMaxKeyLimit, boolean bWeightOut);
        void NLPIR_Exit();
    }

    public static String transString(String aidString, String ori_encoding,String new_encoding) {
        try {
            return new String(aidString.getBytes(ori_encoding), new_encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 调用张华平老师的分词
     * @param sInput 待分析的句子
     * @return 得到 List<String> 即分词序列
     * @throws Exception
     */
    public static List<String> result(String sInput) throws Exception{
        String argu = "";
        String system_charset = "UTF-8";
        int charset_type = 1;
        List<String> wordList = new ArrayList<>();

        if (!CLibrary.Instance.NLPIR_Init(argu.getBytes(system_charset), charset_type, "0".getBytes(system_charset))) {
            System.err.println("初始化失败！");
        }
        String nativeBytes;
        nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 3);
        String[] words = nativeBytes.split(" ");
        for (String word : words) {
            if(word.contains("/nr") | word.contains("/t") | word.contains("/p") | word.contains("/u") |
                    word.contains("/e") | word.contains("/y") | word.contains("/o") | word.contains("/x") |
                    word.contains("/w"))
                continue;
            boolean flag_emptyString = true;
            for(int j = 0;j < word.length();j++){
                if(word.charAt(j) != ' ')
                    flag_emptyString = false;
            }
            if(flag_emptyString)
                continue;
            int position = word.trim().lastIndexOf('/');
            if(position < 0)
                continue;
            String substring = word.trim().substring(0, position);
            if(substring.length() > 1 && !substring.contains("x") && !substring.contains("'"))
                wordList.add(substring);
        }
        return wordList;
    }


    public static void main(String[] args) throws Exception {
        String message = "金宝贝商行x月xx日—x月xx日周年庆三店同庆童装'童鞋x～x折奶粉x.x折'会员积分兑大奖'君心孕妇装'品牌婴儿服饰'用品火爆热销中";
        List<String> result = result(message);
        System.out.println(result);
    }
}
