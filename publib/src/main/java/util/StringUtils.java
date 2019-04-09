package util;

import com.google.gson.JsonParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;

public class StringUtils {
    private static final String INTEGER_REGEX = "0|-?([1-9]{1}[0-9]*)";

    //字符串不为空
    public static boolean isEmpty(String str){
        return str == null || str.trim().length() == 0 ;
    }

    //判断一组字符串都不为空
    public static boolean isEmpty(String... arr){
        for (String str : arr){
            if (isEmpty(str)) return true;
        }
        return false;
    }

    public static String trim(String text) {
        if(text == null || "".equals(text)) {
            return text;
        }
        return text.trim();
    }

    //判断对象是否为null 设置默认值
    public static <T> T checkObjectNull(Object object,T def){
        try {
            if (object == null) return def;
            if (def instanceof String){
                return (T) object.toString();
            }
            return (T) object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static String obj2Str(Object object,String def){
        if (object == null) return def;
        return  String.valueOf(object);
    }

    /**
     * 判定字符串是否为整数。
     * @param str
     * @return
     */
    public static boolean isInteger(String str) {
        return !isEmpty(str) && Pattern.matches(INTEGER_REGEX, str);
    }

    public static boolean isBiggerZero(String str) {
        return isInteger(str) && Long.parseLong(str) > 0;
    }

    public static boolean isDateFormatter(String str) {
        try {
            new SimpleDateFormat("yyyy-MM-dd").parse(str);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean isJsonFormatter(String str) {
        try {
            new JsonParser().parse(str);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

}
