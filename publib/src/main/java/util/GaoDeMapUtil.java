package util;

import util.http.HttpRequest;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/12 15:29
 */
public class GaoDeMapUtil {

    public static String apiKey = "c59217680590515b7c8369ff5e8fe124";

    private static class Geocode{
        String location;
    }

    private static class JsonBean{
        int status;
        List<Geocode> geocodes;
        String province;
        String city;
    }



    /**
     * 获取地址经纬度 index,当前执行次数
     * 经度(longitude)在前，纬度(latitude)在后
     */
    private static String addressConvertLatLon(String address,int index){
        try {
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/geocode/geo?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("address", URLEncoder.encode(address,"UTF-8"));
            map.put("city","");
            map.put("batch","false");
            map.put("sig","");
            map.put("output","JSON");
            map.put("callback","");
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            //System.out.println(sb.toString()+"\naddress "+address+","+result);
            if(StringUtils.isEmpty(result)) throw  new NullPointerException();
            JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
            if (jsonBean == null || jsonBean.status != 1 || jsonBean.geocodes.size() == 0) throw  new NullPointerException();
            return jsonBean.geocodes.get(0).location;
        } catch (Exception e) {
            index++;
            if (index<3) return addressConvertLatLon(address,index);
        }
        return null;
    }
    /**
     * 获取地址经纬度
     */
    public static String addressConvertLatLon(String address){
        return addressConvertLatLon(address.trim(),0);
    }

    /**
     * ip转地址信息
     */
    private static String ipConvertAddress(String ip,int index){
        //https://restapi.amap.com/v3/ip?ip=113.247.55.143&key=c59217680590515b7c8369ff5e8fe124
        //{"status":"1","info":"OK","infocode":"10000","province":"湖南省","city":"长沙市","adcode":"430100","rectangle":"112.6534116,27.96920845;113.3946776,28.42655248"}
        try {
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/ip?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("ip",ip);
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            if(StringUtils.isEmpty(result)) throw  new NullPointerException();
            JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
            if (jsonBean == null || jsonBean.status != 1) throw  new NullPointerException();
            return jsonBean.province+jsonBean.city;
        } catch (NullPointerException e) {
            index++;
            if (index<3) return ipConvertAddress(ip,index);
        }
        return null;
    }
    /**
     * ip转换地址信息
     */
    public static String ipConvertAddress(String ip){
        return ipConvertAddress(ip.trim(),0);
    }


    private static boolean checkPointOnLine(Point2D.Double point, Point2D.Double pointS, Point2D.Double pointD) {
        Line2D line = new Line2D.Double(pointS,pointD);
        return line.contains(point);
    }

    /**
     * 一个点是否在多边形内或线上
     *
     * @param point
     *            要判断的点
     * @param polygon
     *            组成的顶点坐标集合
     * @return
     */
    private static boolean checkPointOnRange(Point2D.Double point, List<Point2D.Double> polygon) {
//        if (polygon.contains(point)) return true;

//        //判断是否在线上
//        for (int i= 0 ; i < polygon.size() ; i++){
//            Point2D.Double pointS ;
//            Point2D.Double pointD ;
//            pointS = polygon.get(i);
//            pointD = polygon.get( i+1 == polygon.size() ? 0 : i+1);
//            if (checkPointOnLine(point,pointS,pointD)) return true;
//            System.out.println(point+" 不包含在线段( "+ pointS+" -> "+ pointD+" )上面");
//        }

        java.awt.geom.GeneralPath peneralPath = new java.awt.geom.GeneralPath();
        Point2D.Double first = polygon.get(0);
        // 通过移动到指定坐标（以双精度指定），将一个点添加到路径中
        peneralPath.moveTo(first.x, first.y);
        polygon.remove(0);
        for (Point2D.Double d : polygon) {
            // 通过绘制一条从当前坐标到新指定坐标（以双精度指定）的直线，将一个点添加到路径中。
            peneralPath.lineTo(d.x, d.y);
        }
        // 将几何多边形封闭
        peneralPath.lineTo(first.x, first.y);
        peneralPath.closePath();
        // 测试指定的 Point2D 是否在 Shape 的边界内。
        return peneralPath.contains(point);
    }

    /**
     * @param singe 单个点的json
     * @param points 多边形的json
     * @return true - 包含在多边形内
     */
    public static boolean checkPointOnRange(String singe,String points) {
        Point2D.Double point = GsonUtils.jsonToJavaBean(singe,Point2D.Double.class);
        List<Point2D.Double> polygon = GsonUtils.json2List(points,Point2D.Double.class);
        return checkPointOnRange(point,polygon);
    }

    /**
     * @param multiple 多个点的json
     * @param points 多边形的json
     * @return true - 包含在多边形内
     */
    public static boolean[] checkMorePointOnRange(String multiple,String points) {

        List<Point2D.Double> pointList = GsonUtils.json2List(multiple,Point2D.Double.class);
        List<Point2D.Double> polygon = GsonUtils.json2List(points,Point2D.Double.class);

        boolean[] res = new boolean[pointList.size()];
        for (int i = 0;i<res.length;i++){
            res[i] = checkPointOnRange(pointList.get(i),polygon);
        }
        return res;
    }

    public static void main(String[] args) {




//https://lbs.amap.com/api/javascript-api/example/relationship-judgment/point-surface-relation  console.log(JSON.stringify(point))

//        Point2D.Double point2d = new Point2D.Double(112.988035,28.22271);//商务标志楼 true
//        Point2D.Double point2d = new Point2D.Double(112.993995,28.187931);//湘雅二医院 -false
       /* Point2D.Double point2d = new Point2D.Double(112.919378,28.219301);//测试点
        System.out.println(GsonUtils.javaBeanToJson(point2d));
        point2d = GsonUtils.jsonToJavaBean(GsonUtils.javaBeanToJson(point2d),Point2D.Double.class);

        List<Point2D.Double> polygon = new ArrayList<>();
        polygon.add(new Point2D.Double(112.938888,28.228272) ); //市政府
        polygon.add(new Point2D.Double(113.012932,28.233654) );//德雅路口
        polygon.add(new Point2D.Double(112.988412,28.223999) );//华创
        polygon.add(new Point2D.Double(112.987862,28.220076) );//新时代广场
        polygon.add(new Point2D.Double(112.975598,28.220521) );//雅泰花园
        polygon.add(new Point2D.Double(112.919358,28.219294) );//商学院
        System.out.println(GsonUtils.javaBeanToJson(polygon));
        System.out.println(checkPointOnRange(point2d,polygon));*/

       String singe = "{\"x\":112.919378,\"y\":28.219301}";
       String points = "[{\"x\":112.938888,\"y\":28.228272},{\"x\":113.012932,\"y\":28.233654},{\"x\":112.988412,\"y\":28.223999},{\"x\":112.987862,\"y\":28.220076},{\"x\":112.975598,\"y\":28.220521},{\"x\":112.919358,\"y\":28.219294}]";
        System.out.println(checkPointOnRange(singe,points));
    }
}
