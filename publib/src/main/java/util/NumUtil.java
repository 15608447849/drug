package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class NumUtil {

    /**
     * 基准数字和数组中其他数字相加排列组合
     * 比如: 128 {256,512}
     * 得到结果就是 128+256 128+512 128+256+512
     * @param aa 基准数字
     * @param bb 数组
     * @param result 得到结果
     * @return
     */
    public static Set<Integer> arrangeAdd(int aa, List<Integer> bb, Set<Integer> result){

        if(bb.size()<=0){
            return result;
        }
        for(int i : bb){
            result.add(aa + i);
            List<Integer> cc = new ArrayList<>();
            for(int j : bb){
                if(i != j){
                    cc.add(j);
                }
            }
            arrangeAdd(aa+i,cc, result);
        }
        return result;
    }

    /**
     * 获取这个数字所有二进制唯一的位数
     *
     * @param val
     * @return
     */
    public static List<Integer> getNonZeroBits(int val){
        List<Integer>  list = new ArrayList<>();
        int index = 0;
        while(val > 0){
            if((val & 1) > 0){
                list.add(1 << index);
            }

            index++;
            val >>=  1;
        }
        return list;
    }
}
