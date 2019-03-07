package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hyrdpf.util.GeneralHelper;

public class ParameterUtil {
	/**
	 * 提取N的二进制上B位上的数
	 * @param N 待提取的数
	 * @param B 待取的二进制的位
	 */
	public static long getBitValue(long N,int B){
		return ((1<<(B-1))&N);
	}
	/**
	 * 获取10进制数字长度
	 * @param i
	 * @return
	 */
	public static int getIntLength(int i){
		String str = i+"";
		return str.length();		
	}
	
	
	/**
	 *  获取状态码中某一段值
	 * @param cstatus
	 * @param value
	 * @return
	 */
	public static int getRangeCstate(int cstatus,int value){	
		try{			
			return cstatus&value;			
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	
	public static int cleanRangeCState(int cstatus,int value){
		try{			
			return cstatus&(~value);			
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
     * 使用 Map按key进行降序排序
     * @param map
     * @return
     */
	 public static Map<Integer, String> sortMapByKey(Map<Integer, String> map) {
	        List<Map.Entry<Integer,String>> list = new ArrayList<>(map.entrySet());
	        Collections.sort(list, (x, y) -> {
//	            return x.getKey().compareTo(y.getKey());
	            return (x.getKey() > y.getKey()) ? -1 : ((Objects.equals(x.getKey(), y.getKey())) ? 0 : 1);
	        });
	        Map<Integer, String> sortBy = new HashMap<Integer, String>();
	       for(Map.Entry<Integer,String> entry:list){
	           sortBy.put(entry.getKey(),entry.getValue());
	       }
	        return sortBy;
	    }
	
	
	public static boolean isNull(String str){
		return !GeneralHelper.isTrimStrNotEmpty(str);
	}
	
}
