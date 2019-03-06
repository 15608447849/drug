package objectref;

import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

public class ObjectPool {

    private long time = System.currentTimeMillis(); //长期未更新时间 清理所有 移除池

    private ReentrantLock lock = new ReentrantLock();

    private final int maxNumber = 50; // 对象池最大的大小 ,当超过这个数量 删除对象

    private Vector objects =  new Vector(); //存放对象池中对象的向量


    /**
     * 放入一个空闲对象
     * 如果超过对象池大小限制, 丢弃
     */
    public void putObject(Object obj){
        try{
            lock.lock();
            time = System.currentTimeMillis();
            if (objects.size() > maxNumber){
                return;
            }
            objects.addElement(obj);
            System.out.println("putObject 当前大小 "+objects.size());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    /**
     * 取出一个空闲对象
     * 从列表删除
     */
    public Object getObject(){
        Object obj = null;
        try{
            lock.lock();
            time = System.currentTimeMillis();
            Enumeration enumerate = objects.elements();

            if (enumerate.hasMoreElements()) {
                obj = enumerate.nextElement();
                objects.removeElement(obj);
            }
            System.out.println("getObject 当前大小 "+objects.size());
            return obj;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return obj;
    }


    /**
     * 检测长期未使用,是否清理所有并通知是否移除自己
     * ideaTime 超时时间
     * true 移除自己
     */
    public boolean checkSelf(long ideaTime){
        if (System.currentTimeMillis() - time > ideaTime) {
            try{
                lock.lock();

                Enumeration enumerate = objects.elements();

                while (enumerate.hasMoreElements()) {
                    // 从对象池向量中删除
                    objects.removeElement(enumerate.nextElement());
                }

                return true;
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                lock.unlock();
            }
        }
        return false;
    }



}
