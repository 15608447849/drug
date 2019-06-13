package util;

import com.google.gson.JsonParser;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class StringUtils {
    private static final String INTEGER_REGEX = "0|-?([1-9]{1}[0-9]*)";
    private final static String EMAIL_REGEXP =
            "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";

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

            try {
                Class cls = def.getClass();
                String m = "parse"+cls.getSimpleName();
                if (m.contains("Int")) m = "parseInt";
                Method method = cls.getMethod(m, String.class);
                T t = (T) method.invoke(null,object.toString());
                return t;
            } catch (Exception e) {

                return (T) object;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static String obj2Str(Object object){
        return  checkObjectNull(object,"");
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

    public static boolean isEmail(String str) {
        return !isEmpty(str) && Pattern.matches(EMAIL_REGEXP, str);
    }

    public static boolean isJsonFormatter(String str) {
        try {
            new JsonParser().parse(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /* 文字转拼音大写字母 */
    public static String converterToFirstSpell(String chines) {
        String pinyinFirstKey = "";
        char[] nameChar = chines.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (int i = 0; i < nameChar.length; i++) {
            String s = String.valueOf(nameChar[i]);
            if (s.matches("[\\u4e00-\\u9fa5]")) {
                try {
                    String[] mPinyinArray = PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat);
                    pinyinFirstKey += mPinyinArray[0].charAt(0);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                pinyinFirstKey += nameChar[i];
            }
        }
        return pinyinFirstKey.toUpperCase();
    }

}
