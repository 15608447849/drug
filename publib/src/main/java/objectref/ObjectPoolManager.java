package objectref;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * lzp
 * 对象池
 * 随系统运行则启动
 * 单例
 */
public class ObjectPoolManager extends Thread{

    //最大清理时间
    private final long maxClearTime = 2 * 60 * 60 * 1000L;

    //线程安全的池 k 类全路径 可用的对象的矢量池
    private LinkedHashMap<String, ObjectPool> poolMap = new LinkedHashMap<>(new LinkedHashMap<>());

    private ObjectPoolManager(){
        setDaemon(true);
        setName("对象池守护线程-"+getId());
        start();
    }


    private static class Holder{
     private static ObjectPoolManager INSTANCE = new ObjectPoolManager();
    }

    //获取实例
    public static ObjectPoolManager get(){
        return Holder.INSTANCE;
    }

    @Override
    public void run() {

        while (true){
            //间隔执行 清理对象池
            try {
                sleep(maxClearTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Iterator<Map.Entry<String, ObjectPool>> iterator = poolMap.entrySet().iterator();
            Map.Entry<String, ObjectPool> entry;
            while (iterator.hasNext()){
                entry = iterator.next();
                if (entry.getValue().checkSelf(maxClearTime)){
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 获取一个池中缓存的对象
     */
    public Object getObject(String classPath){
        ObjectPool pool = poolMap.get(classPath);
        if (pool == null){
            return null;
        }
        return pool.getObject();
    }

    /**
     * 存入一个对象
     */
    public void putObject(String classPath,Object object){
        ObjectPool pool = poolMap.get(classPath);
        if (pool==null){
            pool = new ObjectPool();
            poolMap.put(classPath,pool);
        }
        pool.putObject(object);
    }

}
