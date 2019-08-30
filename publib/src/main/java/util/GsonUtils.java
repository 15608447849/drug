package util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Leeping on 2018/6/27.
 * email: 793065165@qq.com
 */

public class GsonUtils {

    private static Gson newGson(){
        return new GsonBuilder()
                .setLongSerializationPolicy(LongSerializationPolicy.STRING)
               /* .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
                   // System.out.println("-----------double-------GSON: "+ src +" - "+ src.longValue()+" ");
//                    if (src == src.longValue())
//                        return new JsonPrimitive(src.longValue());
//                    return new JsonPrimitive(src);
//                    return new JsonPrimitive(src.longValue()+"");

//                    JsonPrimitive r =  new JsonPrimitive(src.longValue()+"");
//                    System.out.println(r);
                    return new JsonPrimitive(src.longValue()+"");
                })*/
              /*  .registerTypeAdapter(Double.class, new JsonDeserializer<Double>() {
                    @Override
                    public Double deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        System.out.println(json+" "+typeOfT+" "+context);
                        String value = json.getAsString();
                        System.out.println("DOUBLE >> "+ value);
                        return Double.parseDouble(value);
                    }
                })*/
             /*   .registerTypeAdapter(String.class, new JsonDeserializer<String>() {
                    @Override
                    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        String value = json.getAsString();
                        System.out.println("String >> "+ json);
//                        return value;
                        return json.getAsString();
                    }
                })*/
            /*    .registerTypeAdapter(Object[].class, new JsonDeserializer<Object[]>() {
                    @Override
                    public Object[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        System.out.println("Object[] >> "+ json);
                        return null;
                    }
                })*/
           /*     .registerTypeAdapter(new TypeToken<List<Object[]>>(){}.getType(), new JsonDeserializer<List<Object[]>>() {
                    @Override
                    public List<Object[]> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        System.out.println("List<Object[]> >> "+ json);
                        return null;
                    }
                })*/
             /*   .registerTypeAdapter(Integer.class,(JsonSerializer<Integer>) (src, typeOfSrc, context) -> {
//                    System.out.println("-------------int-----GSON: "+ src);
//            if (src == src.longValue())
//                return new JsonPrimitive(src.longValue());
//            return new JsonPrimitive(src);
                    return new JsonPrimitive(src+"");

        })*/
                .create();
    }




    /**
     * json to javabean
     *new TypeToken<List<xxx>>(){}.getType()
     * @param json
     */
    public static <T> T jsonToJavaBean(String json,Type type) {
        try {
            if (json==null || json.length()==0) return null;
            return newGson().fromJson(json, type);//对于javabean直接给出class实例
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * javabean to json
     * @param object
     * @return
     */
    public static String javaBeanToJson(Object object){
        return newGson().toJson(object);
    }
    /**
     * json to javabean
     *
     * @param json
     */
    public static <T> T jsonToJavaBean(String json,Class<T> cls) {
        try {
            if (json==null || json.length()==0) return null;
            return newGson().fromJson(json, cls);//对于javabean直接给出class实例
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T,D> HashMap<T,D> string2Map(String json){
        try {
            if (StringUtils.isEmpty(json)) return null;
            return jsonToJavaBean(json, new TypeToken<HashMap<T,D>>() {}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> List<T> json2List(String json, Class<T> clazz){
        List<T> list = new ArrayList<>();
        try {
            Gson gson = newGson();
            JsonArray array = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement element : array) {
                list.add(gson.fromJson(element, clazz));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


}
