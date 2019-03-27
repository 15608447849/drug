package util;

public class StringUtils {
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
        if (object == null) return def;
        return (T) object;
    }

    public static String obj2Str(Object object,String def){
        if (object == null) return def;
        return  String.valueOf(object);
    }
}
