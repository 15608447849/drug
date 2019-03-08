package objectref;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 对象反射工具
 * 创建对象
 * 调用对象方法
 * 静态方法不用考虑并发
 *
 */
public class ObjectRefUtil {

    private ObjectRefUtil(){ }

    public static Object createObject(Class cls,Class[] parameterTypes,Object[] parameters) throws Exception{
        Constructor constructor = cls.getConstructor(parameterTypes);
        Object obj =constructor.newInstance(parameters);
        return obj;
    }

    /**
     * 反射创建对象
     * @param classPath 全类名
     * @param parameterTypes 参数类型数组
     * @param parameters 参数实际数据
     * @return 反射对象
     */
    public static Object createObject(String classPath,Class[] parameterTypes,Object[] parameters) throws Exception{
            Class cls = Class.forName(classPath);
            return createObject(cls,parameterTypes,parameterTypes);
    }

    /**
     * 反射调用某个类方法
     * @param holder 方法持有者
     * @param methodName 方法名
     * @param parameterTypes 方法参数 类类型
     * @param parameters 方法实际参数
     * @return 方法调用返回值
     */
   public static Object callMethod(Object holder,String methodName,Class[] parameterTypes,Object... parameters) throws Exception{
       Object obj = null;
           //getDeclaredMethod*()获取的是类自身声明的所有方法,包含public、protected和private方法
           // getMethod*()获取的是类的所有共有方法,这就包括自身的所有public方法,和从基类继承的、从接口实现的所有public方法
           Method method = holder.getClass().getMethod(methodName,parameterTypes);
       return method.invoke(holder,parameters);//调用对象的方法
   }



}
