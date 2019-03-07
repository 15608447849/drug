package util;

public class StringUtils {
    //字符串不为空
    public static boolean isEmpty(String str){
        return str == null || str.trim().length() == 0 ;
    }

    public static String trim(String text) {
        if(text == null || "".equals(text)) {
            return text;
        }

        return text.trim();
    }

}
