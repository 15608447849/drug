package util;

import util.http.HttpRequest;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;

/**
 * @Author: leeping
 * @Date: 2019/4/12 15:29
 */
public class GaoDeMapUtil {

    public static String apiKey = "c59217680590515b7c8369ff5e8fe124";

    public static class Point{
        public double lng;
        public double lat;

        public Point(double lng, double lat){
            this.lng = lng;
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
            return "{" + "lng=" + lng + ", lat=" + lat + '}';
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
    /**
     * 解析地址
     * @param address
     * @return
     */
    public static List<Map<String,String>> addressResolution(String address){
        /*
         * java.util.regex是一个用正则表达式所订制的模式来对字符串进行匹配工作的类库包。它包括两个类：Pattern和Matcher Pattern
         *    一个Pattern是一个正则表达式经编译后的表现模式。 Matcher
         *    一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
         *    首先一个Pattern实例订制了一个所用语法与PERL的类似的正则表达式经编译后的模式，然后一个Matcher实例在这个给定的Pattern实例的模式控制下进行字符串的匹配工作。
         */
        String regex="(?<province>[^省]+自治区|.*?省|.*?行政区|.*?市)(?<city>[^市]+自治州|.*?地区|.*?行政单位|.+盟|市辖区|.*?市|.*?县)(?<county>[^县]+县|.+区|.+市|.+旗|.+海域|.+岛)?(?<town>[^区]+区|.+镇)?(?<village>.*)";
        Matcher m= Pattern.compile(regex).matcher(address);
        String province=null,city=null,county=null,town=null,village=null;
        List<Map<String,String>> table=new ArrayList<Map<String,String>>();
        Map<String,String> row=null;
        while(m.find()){
            row=new LinkedHashMap<String,String>();
            province=m.group("province");
            row.put("province", province==null?"":province.trim());
            city=m.group("city");
            row.put("city", city==null?"":city.trim());
            county=m.group("county");
            row.put("county", county==null?"":county.trim());
            town=m.group("town");
            row.put("town", town==null?"":town.trim());
            village=m.group("village");
            row.put("village", village==null?"":village.trim());
            table.add(row);
        }
        return table;
    }


    //获取地区边界信息
    private static DataBean areaPolyline(String address,int index){
        try{
            String temp = address;
            if (index<4){
                Map<String,String>  map = addressResolution(address).get(0);
                if (!StringUtils.isEmpty(map.get("province"))){
                    temp = map.get("province");
                }
                if (!StringUtils.isEmpty(map.get("city"))){
                    temp = map.get("city");
                }
                if (!StringUtils.isEmpty(map.get("county"))){
                    temp = map.get("county");
                }
            }else{
                DataBean d = addressConvertLatLon(address, 0);
                temp = d.adcode;
            }

            temp = URLEncoder.encode(temp,"UTF-8");
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/config/district?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("keywords",temp);
            map.put("subdistrict","0");
            if (index>=4) map.put("filter",temp);
            map.put("extensions","all");
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            if(StringUtils.isEmpty(result)) throw  new NullPointerException();
            JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
            if (jsonBean == null || jsonBean.status != 1 || jsonBean.districts.size() == 0) throw  new NullPointerException();
            return jsonBean.districts.get(0);
        } catch (Exception e) {
            index++;
            if (index<6) return areaPolyline(address,index);
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
    public static List<List<Point>> areaPolyline(String address){
            return handleSourcePolyline(Objects.requireNonNull(areaPolyline(address, 0)).polyline);
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
        return GsonUtils.javaBeanToJson(pointOrPointList).replace("lng","x").replace("lat","y");
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

    // [lon1,lat1,lon2,lat2] -> List:[lng,lat]
    private static List<Double[]> pointArray2ListDouble(Double[] doubles){
        List<Double[]> list = new ArrayList<>();
        for (int i = 0 ; i<doubles.length ;i+=2){
            list.add(new Double[]{doubles[i],doubles[i+1]});
        }
        return list;
    }
    //[x,y;x,y]  -> list<double[]>
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

    //[{"lng":112.919358,"lat":28.219294}] -> [[112.975598,28.220521]]
    public static String pointJsonToListArrayJson(String json){
        try {
            List<Point> points = GsonUtils.json2List(json,Point.class);
            if (points!=null && points.size() > 0){
                List<Double[]> list = new ArrayList<>();
                for (int i = 0 ; i<points.size() ;i++){
                    list.add(new Double[]{points.get(i).lng,points.get(i).lat});
                }
                return GsonUtils.javaBeanToJson(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //处理 | , -> {[x,y] [x,y]} -> LIST<POINT>[]
    private static List<List<Point>> handleSourcePolyline(String line){
        String[] arr = line.split("\\|");
        List<List<Point>> listArr = new ArrayList<>();
        for (String temp : arr){
            List<Point> list = listArrayJsonToPointJson(pointArray2ListDouble("[" + temp + "]"));
            listArr.add(list);
        }
        return listArr;
    }


//    //[x,y;x,y]  -> list<double[]>
//    private static String pointArray2ListDouble(String json){
//        return GsonUtils.javaBeanToJson(pointArray2ListDouble(Objects.requireNonNull(GsonUtils.jsonToJavaBean(json, Double[].class))));
//    }



    public static void main(String[] args) {



//        String json = "[{\"P\":28.59241124249085,\"Q\":112.75455302149055,\"lng\":112.754553,\"lat\":28.592411},{\"P\":28.51279746479939,\"Q\":112.9880124941468,\"lng\":112.988012,\"lat\":28.512797},{\"P\":28.489867193303038,\"Q\":112.73120707422493,\"lng\":112.731207,\"lat\":28.489867}]";

//        System.out.println(pointJsonToListArrayJson(json));

//        System.out.println( areaPolyline("湖南省株洲市渌口区"));

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

//       String singe = "{\"lng\":112.919378,\"lat\":28.219301}";
//       String points = "[{\"lng\":112.938888,\"lat\":28.228272},{\"lng\":113.012932,\"lat\":28.233654},{\"lng\":112.988412,\"lat\":28.223999},{\"lng\":112.987862,\"lat\":28.220076},{\"lng\":112.975598,\"lat\":28.220521},{\"lng\":112.919358,\"lat\":28.219294}]";
//       Point p = GsonUtils.jsonToJavaBean(singe,Point.class);
//       List<Point> list = GsonUtils.json2List(points,Point.class);
//
//       System.out.println(checkPointOnRange(p,list));
    }
}
