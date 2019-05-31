package util;

import util.http.HttpRequest;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static java.lang.Double.parseDouble;

/**
 * @Author: leeping
 * @Date: 2019/4/12 15:29
 */
public class GaoDeMapUtil {

    public static String apiKey = "c59217680590515b7c8369ff5e8fe124";

    public static class Point{
        public double lon;
        public double lat;

        public Point(double lon, double lat){
            this.lon = lon;
            this.lat = lat;
        }

        public Point(Double[] doubles){
            this(doubles[0],doubles[1]);
        }

        /**
         * 经度(longitude)在前，纬度(latitude)在后
         * @param pointStr "112.984200,28.205628"
         */
        public Point(String pointStr){
            this( parseDouble(pointStr.split(",")[0]), parseDouble(pointStr.split(",")[1]));
        }

        @Override
        public String toString() {
            return "{" + "lon=" + lon + ", lat=" + lat + '}';
        }
    }

    private static class DataBean {
        String location;
        String adcode;
        String polyline;
    }

    private static class JsonBean{
        int status;
        List<DataBean> geocodes;
        String province;
        String city;
        List<DataBean> districts;
    }

    /**
     * 获取地址信息 index,当前执行次数
     */
    private static DataBean addressConvertLatLon(String address,int index){
        try {
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/geocode/geo?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("address", URLEncoder.encode(address.trim(),"UTF-8"));
            map.put("city","");
            map.put("batch","false");
            map.put("sig","");
            map.put("output","JSON");
            map.put("callback","");
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            if(StringUtils.isEmpty(result)) throw  new NullPointerException();
            JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
            if (jsonBean == null || jsonBean.status != 1 || jsonBean.geocodes.size() == 0) throw  new NullPointerException();
            return jsonBean.geocodes.get(0);
        } catch (Exception e) {
            index++;
            if (index<3) return addressConvertLatLon(address,index);
        }
        return null;
    }
    //获取地区边界信息
    private static DataBean areaPolyline(String address,int index){
        try{
            DataBean d = addressConvertLatLon(address, 0);
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/config/district?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("keywords",d.adcode);
            map.put("subdistrict","0");
            map.put("filter",d.adcode);
            map.put("extensions","all");
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            System.out.println(result);
            if(StringUtils.isEmpty(result)) throw  new NullPointerException();
            JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
            if (jsonBean == null || jsonBean.status != 1 || jsonBean.districts.size() == 0) throw  new NullPointerException();
            return jsonBean.districts.get(0);
        } catch (Exception e) {
            index++;
            if (index<3) return areaPolyline(address,index);
        }
        return null;
    }

    /**
     * 获取地址经纬度
     */
    public static Point addressConvertLatLon(String address){
        return new Point(Objects.requireNonNull(addressConvertLatLon(address, 0)).location);
    }

    /**
     * 获取地址边界点Point
     */
    public static List<Point> areaPolyline(String address){
        return listArrayJsonToPointJson(pointArray2ListDouble("["+Objects.requireNonNull(areaPolyline(address, 0)).polyline+"]"));
    }

    /**
     * ip转地址信息
     */
    private static String ipConvertAddress(String ip,int index){
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

    //判断点在线上
    private static boolean checkPointOnLine(Point2D.Double point, Point2D.Double pointS, Point2D.Double pointD) {
        Line2D line = new Line2D.Double(pointS,pointD);
        return line.contains(point);
    }

    /**一个点是否在多边形内或线上
     * @param point  要判断的点
     * @param polygon 组成的顶点坐标集合
     * @return true 包含
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



    private static String pointJsonToPoint2DJson(Object pointOrPointList){
        return GsonUtils.javaBeanToJson(pointOrPointList).replace("lon","x").replace("lat","y");
    }

    /**
     * @param singe 单个点
     * @param points 多边形的点线性集合
     * @return true - 包含在多边形内
     */
    public static boolean checkPointOnRange(Point singe,List<Point> points) {
        Point2D.Double point = GsonUtils.jsonToJavaBean(pointJsonToPoint2DJson(singe),Point2D.Double.class);
        List<Point2D.Double> polygon = GsonUtils.json2List(pointJsonToPoint2DJson(points),Point2D.Double.class);
        return checkPointOnRange(point,polygon);
    }

    // [lon1,lat1,lon2,lat2] -> List:[lon,lat]
    private static List<Double[]> pointArray2ListDouble(Double[] doubles){
        List<Double[]> list = new ArrayList<>();
        for (int i = 0 ; i<doubles.length ;i+=2){
            list.add(new Double[]{doubles[i],doubles[i+1]});
        }
        return list;
    }
    //point  -> json - list<double[]>
    private static String pointArray2ListDouble(String json){
       return GsonUtils.javaBeanToJson(pointArray2ListDouble(Objects.requireNonNull(GsonUtils.jsonToJavaBean(json, Double[].class))));
    }

    // list<double[]> -> List<Point>
    private static List<Point> listArrayJsonToPointJson(String json){
        //[[112.938888,28.228272],[112.988412,28.223999],[112.975598,28.220521]]
        try {
            List<Double[]> list = GsonUtils.json2List(json,Double[].class);
            List<Point> points = new ArrayList<>();
            if (list!=null && list.size()>0) {
                for(Double[] doubles : list){
                    points.add(new Point(doubles));
                }
            }
            return points;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //[{"lon":112.919358,"lat":28.219294}] -> [[112.975598,28.220521]]
    public static String pointJsonToListArrayJson(String json){
        try {
            List<Point> points = GsonUtils.json2List(json,Point.class);
            if (points!=null && points.size() > 0){
                List<Double[]> list = new ArrayList<>();
                for (int i = 0 ; i<points.size() ;i+=2){
                    list.add(new Double[]{points.get(i).lon,points.get(i).lat});
                }
                return GsonUtils.javaBeanToJson(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {

//        List<Point> points = areaPolyline("湖南省长沙市");
//        System.out.println(points);

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

//       String singe = "{\"lon\":112.919378,\"lat\":28.219301}";
//       String points = "[{\"lon\":112.938888,\"lat\":28.228272},{\"lon\":113.012932,\"lat\":28.233654},{\"lon\":112.988412,\"lat\":28.223999},{\"lon\":112.987862,\"lat\":28.220076},{\"lon\":112.975598,\"lat\":28.220521},{\"lon\":112.919358,\"lat\":28.219294}]";
//       Point p = GsonUtils.jsonToJavaBean(singe,Point.class);
//       List<Point> list = GsonUtils.json2List(points,Point.class);
//
//       System.out.println(checkPointOnRange(p,list));
    }
}
