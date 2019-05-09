package util;

import IceInternal.Ex;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by user on 2017/11/27.
 */
public class TimeUtils {

    /**
     * 一周间隔时间
     */
    public static final long PERIOD_WEEK = 7 * 24 * 60 * 60 * 1000;

    /**
     * 一天得间隔时间
     */
    public static final long PERIOD_DAY = 24 * 60 * 60 * 1000;

    /**
     * 一小时间隔时间
     */
    public static final long PERIOD_HOUR = 24 * 60 * 60 * 1000;

    /**添加x天*/
    public static Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }

    /**添加x天*/
    public static Date subtractDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, -num);
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
     * 例: 2017-11-11
     */
    public static Date str_yMd_2Date(String timeString){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return simpleDateFormat.parse(timeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 例: 2017-11-11 9:50:00
     */
    public static String date_yMd_Hms_2String(Date date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return simpleDateFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 例: 2017-11-11
     */
    public static String date_yMd_2String(Date date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return simpleDateFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 例: 1111 标识xx月xx日
     */
    public static String date_Md_2String(Date date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMdd");
        try {
            return simpleDateFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 例: 2017-11-11 9:50:00
     */
    public static String date_Hms_2String(Date date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        try {
            return simpleDateFormat.format(date);
        } catch (Exception e) {
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

    /**
     * 获取当前日期
     */
    public static String getCurrentDate(){
        try {
            return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return "1900-01-01";
    }


    /**
     * 格式为"HH:mm:ss"
     * 判断当前时间是否在[startDate, endDate]区间，注意时间格式要一致
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean isEffectiveTime(String startTime, String endTime){
        try{
            String format = "HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String nowTime = sdf.format(new Date());
            Date nowDate = sdf.parse(nowTime);
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);
            return isEffectiveDate(nowDate, startDate, endDate);

        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断当前时间是否在[startDate, endDate]区间，注意时间格式要一致
     *
     * @param nowDate 当前时间
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return
     */
    public static boolean isEffectiveDate(Date nowDate, Date startDate, Date endDate) {
        if (nowDate.getTime() == startDate.getTime()
                || nowDate.getTime() == endDate.getTime()) {
            return true;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(nowDate);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startDate);

        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 判断时间1是否在时间2之后
     *
     * @param date1 时间1
     * @param date2 时间2
     * @return
     */
    public static boolean after(String date1, String date2) {

        Date d1 = str_yMd_Hms_2Date(date1);
        Date d2 = str_yMd_Hms_2Date(date2);

        return d1.after(d2);
    }

    public static int getYearByOrderno(String orderno) {
        if (StringUtils.isEmpty(orderno)) {
            return getCurrentYear();
        }

        try {
            return Integer.parseInt("20" + orderno.substring(0, 2));
        } catch (Exception e) {
            return 0;
        }

    }

}
