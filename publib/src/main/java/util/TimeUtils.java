package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by user on 2017/11/27.
 */
public class TimeUtils {

    /**
     * 一天得间隔时间
     */
    public static final long PERIOD_DAY = 24 * 60 * 60 * 1000;

    /**添加x天*/
    private static Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }
    /**string -> date ,  参数:"11:00:00"  如果小于当前时间,向后加一天*/
    public static Date str_Hms_2Date(String timeString) {
        try {
            String[] strArr = timeString.split(":");

            Calendar calendar = Calendar.getInstance();
            if (strArr.length >= 1){
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(strArr[0]));
            }else{
                calendar.set(Calendar.HOUR_OF_DAY, 0);
            }
            if (strArr.length >= 2){
                calendar.set(Calendar.MINUTE, Integer.parseInt(strArr[1]));
            }else{
                calendar.set(Calendar.MINUTE,0);
            }
            if (strArr.length >= 3){
                calendar.set(Calendar.SECOND, Integer.parseInt(strArr[2]));
            }else{
                calendar.set(Calendar.SECOND, 0);
            }
            Date date = calendar.getTime();
            if (date.before(new Date())) {
                date = addDay(date, 1);
            }
            return date;
        } catch (Exception e) {
            e.printStackTrace();
        }
       return null;
    }

    /**
     * 例: 2017-11-11 9:50:00
     */
    public static Date str_yMd_Hms_2Date(String timeString){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return simpleDateFormat.parse(timeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 毫秒数-> x天x小时x分x秒
     * @author lzp
     */
    public static String formatDuring(long mss) {
        long days = mss / (1000 * 60 * 60 * 24);
        long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss % (1000 * 60)) / 1000;
        StringBuilder sb = new StringBuilder();
        if (days > 0){
            sb.append(days + "天");
        }
        if (hours > 0){
            sb.append(hours + "小时");
        }
        if (minutes > 0){
            sb.append(minutes + "分钟");
        }
        if (seconds > 0){
            sb.append(seconds + "秒");
        }
        return sb.toString();
    }

    /**
     * 获取当前年份
     */
    public static int getCurrentYear(){
        try {
            return Integer.parseInt(new SimpleDateFormat("yyyy").format(new Date()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 1900;
    }



}
