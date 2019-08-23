package com.onek.user.init;

import com.onek.server.infimp.IIceInitialize;

import static com.onek.user.operations.StoreBasicInfoOp.updateCompInfoToCache;

public class UserInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {

//        //启动线程更新企业信息
//        new Thread(()->{
////            while (true){
//                try {
//                    try {
//                        Thread.sleep(30 * 1000L);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    updateCompInfoToCache();
//                    try {
//                        Thread.sleep( 12 * 60 * 60 * 1000L);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
////            }
//        }).start();

    }

    @Override
    public int priority() {
        return 1;
    }


}
