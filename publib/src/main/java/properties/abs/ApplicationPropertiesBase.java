package properties.abs;

import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import properties.infs.FieldConvert;
import properties.infs.baseImp.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 自动读取属性文件
 * 子类继承,属性使用注解 PropertiesName
 * 子类不标识,则默认属性文件名application.properties
 *
 */
@PropertiesFilePath("/application.properties")
public abstract class ApplicationPropertiesBase {

    private static final HashMap<String, FieldConvert> baseType = new HashMap<>();

    static {
        baseType.put("class java.lang.String",new StringConvertImp());
        baseType.put("boolean",new BooleanConvertImp());
        baseType.put("int",new IntConvertImp());
        baseType.put("float",new FloatConvertImp());
        baseType.put("double",new DoubleConvertImp());
        baseType.put("long",new LongConvertImp());
    }

    private static final Properties properties = new Properties();

    public ApplicationPropertiesBase() {
        try {
            String filePath = getPropertiesFilePath();
            InputStream in = readPathProperties(filePath);
            if (in==null) throw new Exception("配置文件获取失败: "+ filePath);
            properties.clear();
            properties.load(in);
            in.close();
            autoReadPropertiesMapToField();
            initialization();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream readPathProperties(String filePath) throws FileNotFoundException {
        //优先从外部配置文件获取
        String dirPath = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        File file = new File(dirPath+"/resources"+filePath);
        if (file.exists()){
            //try { System.out.println("配置文件: "+ file.getCanonicalPath()); } catch (IOException ignored) {  }
            return new FileInputStream(file);
        }
        //在获取当前jar包下是否存在
        /*URL url = this.getClass().getResource(filePath);
        if (url == null) {
            return this.getClass().getResourceAsStream( filePath );
        }
        System.out.println("jar默认配置文件: "+ url.getFile());
        try {
            return url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
//        file = new File(url.getFile());
//        if (file.exists()){
//            try { System.out.println("存在配置文件: "+ file.getCanonicalPath()); } catch (IOException ignored) {  }
//        }

        return this.getClass().getResourceAsStream( filePath );
    }




    private String getPropertiesFilePath() {
        PropertiesFilePath annotation = this.getClass().getAnnotation(PropertiesFilePath.class);
        return annotation.value();
    }

    protected void autoReadPropertiesMapToField(){
        Class clazz = this.getClass();
        Field[] fields = clazz.getDeclaredFields();//某个类的所有声明的字段，即包括public、private和protected ,但是不包括父类的申明字段

        for (Field field : fields){
            PropertiesName name = field.getAnnotation(PropertiesName.class);
            if(null != name){
                boolean canAccess = field.isAccessible();//如果成员为私有，暂时让私有成员运行被访问和修改
                if(!canAccess){
                    field.setAccessible(true);
                }
                String key = name.value();
                String value = properties.getProperty(key);
                if (value==null || value.length()==0) {
                    System.err.println("找不到属性名:"+key+"\n全部属性:"+properties);
                    continue;
                }
                //获取属性类型
                String type = field.getGenericType().toString();
                if(baseType.containsKey(type)){
                    try {
                        baseType.get(type).setValue(this,field,value);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                field.setAccessible(canAccess);//改回原来的访问方式
            }
        }
    }

    protected void initialization(){
        Field[] fields = getClass().getDeclaredFields();
        System.out.println("读取配置文件:");
        for(Field field:fields){
            field.setAccessible(true); // 设置些属性是可以访问的
            PropertiesName name = field.getAnnotation(PropertiesName.class);
            if (name == null) continue;
            try {
                String k = name.value();
                String v = field.get(this).toString();
                System.out.println( "\t" + k + " = "  + v);
            } catch (IllegalAccessException ignored) {
            }
        }

    }
}
