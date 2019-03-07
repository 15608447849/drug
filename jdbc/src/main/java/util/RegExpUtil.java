package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 *
 */
public class RegExpUtil {
	public static final String SELECT = "(\\s)?((s|S)(E|e)(L|l)(E|e)(C|c)(T|t))(\\s)";
	public static final String FROM = "(\\s)((f|F)(R|r)(o|O)(m|M))(\\s)";
	public static final String WHERE ="((w|W)(H|h)(e|E)(R|r)(E|e))";
	public static final String GROUP_BY = "(\\s)((g|G)(r|R)(o|O)(u|U)(p|P))(\\s)+((b|B)(Y|y))(\\s)";
	private static int getRegxIndex(String str,String regexp){		
        Pattern pt=Pattern.compile(regexp);  
        Matcher mt=pt.matcher(str);       
        int end = -1;
        while(mt.find()){
        	end = mt.end();
        }
       return end;
    } 
	
	public static int sfw(String str){
		StringBuilder sb = new StringBuilder();
		sb.append(SELECT).append("[\\s\\S]*").append(FROM).append("[\\s\\S]*").append(WHERE);
        String regex = sb.toString();
        return getRegxIndex(str,regex);
	}
	
	public static int sf(String str){
		StringBuilder sb = new StringBuilder();
		sb.append(SELECT).append("[\\s\\S]*").append(FROM);
        String regex = sb.toString();
        return getRegxIndex(str,regex);
	}
	
	public static int s(String str){
		StringBuilder sb = new StringBuilder();
		sb.append(SELECT);
        String regex = sb.toString();
        return getRegxIndex(str,regex);
	}
	
	public static int groupBy(String str){
		StringBuilder sb = new StringBuilder();
		sb.append(GROUP_BY);
        String regex = sb.toString();
        return getRegxIndex(str,regex);
	}
	
	
	
	
}
