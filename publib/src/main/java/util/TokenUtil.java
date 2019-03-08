package util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TokenUtil {

    private TokenUtil(){};
    private static final TokenUtil instance = new TokenUtil();

    public static TokenUtil getInstance() {
        return instance;
    }

    /**
     * 生成Token
     * @return
     */
    public String makeToken(String [] args) {
        String key = String.join(",", args);
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(
                    key.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code;
    }
//
//    public static void main(String[] args) {
//        int userid = 29;
//        int storeid = 593939;
//        String token = TokenUtil.getInstance().makeToken(new String[]{userid+"", storeid+""});
//        System.out.println(token);
//    }
}
