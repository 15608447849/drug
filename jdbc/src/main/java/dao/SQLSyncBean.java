package dao;

import com.google.gson.*;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/8/26 11:19
 */
public class SQLSyncBean {

    private static Object[] toObject(String arrStr){
        arrStr = arrStr.substring(1,arrStr.length()-1);
        String[] arr = arrStr.split(",");
        Object[] oArr = new Object[arr.length];
        System.arraycopy(arr, 0, oArr, 0, oArr.length);
        return oArr;
    }

    private final static Gson builder =  new GsonBuilder()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
             .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> new JsonPrimitive(src.longValue()+""))
             .registerTypeAdapter(Integer.class,(JsonSerializer<Integer>) (src, typeOfSrc, context) -> new JsonPrimitive(src+""))
           /* .registerTypeAdapter(Object[].class, new JsonSerializer<Object[]>() {

                @Override
                public JsonElement serialize(Object[] src, Type typeOfSrc, JsonSerializationContext context) {
//                    System.out.println("序列化 Object[]   :"+ Arrays.toString(src));
//                    String[] arrays = new String[src.length];
//                    for (int i = 0; i < src.length; i++){
//                        arrays[i] = String.valueOf(src[i]);
//                        System.out.println(arrays[i]);
//                    }
                    String arr = Arrays.toString(src);
//                    String arr = Arrays.toString(arrays);

                    return arr.length() > 2 ? new JsonPrimitive(arr) : null;
                }
            })*/
          /*  .registerTypeAdapter(Object[].class, new JsonDeserializer<Object[]>() {
                    @Override
                    public Object[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//                        System.out.println("反序列化 Object[]   :"+ json);
                        return  toObject(json.getAsString());
                    }
                })
            .registerTypeAdapter(new TypeToken<List<Object[]>>(){}.getType(), new JsonDeserializer<List<Object[]>>() {
                   @Override
                   public List<Object[]> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//                       System.out.println("反序列化 List<Object[]>  :"+ json);

                       JsonArray array = json.getAsJsonArray();
                       List<Object[]> list = new ArrayList<>();
                       for (JsonElement element : array){
                           System.out.println(element);
                           list.add(toObject(element.getAsString()));
                       }
                       return list;
                   }
               })*/
            .create();


    public static SQLSyncBean deserialization(String json){
        try {
            return builder.fromJson(json,SQLSyncBean.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean toMaster = false; // 在主库异常时,被从库更新的数据
    int optType;//执行方法标识
    int sharding; //分库
    int tbSharding;//分表
    String[] nativeSQL;//需要执行的多条sql (事务) (需要转换)
    String[] resultSQL;//可执行的sql (已转换)
    List<Object[]> params;//sql参数
    Object[] param; //单sql
    int batchSize;//批量执行时的参数
    int currentExecute = 0;

    public boolean isToMaster(){
        return toMaster;
    }

    public SQLSyncBean(int optType) {
        this.optType = optType;
    }

    void submit(){
        SynDbData.syncI.addSyncBean(this);
    }

    public boolean execute(){
        return SynDbData.post(this);
    }

    @Override
    public String toString() {
        return builder.toJson(this);
    }

    public void errorSubmit() {
        SynDbData.syncI.errorSyncBean(this);
    }
}
