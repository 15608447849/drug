package com.onek.user.init;

import com.onek.server.infimp.IIceInitialize;

import static com.onek.user.operations.StoreBasicInfoOp.updateCompInfoToCache;

public class UserInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        //启动线程更新企业信息
        new Thread(()->{
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true){
                try {
                    updateCompInfoToCache();
                    break;
                } catch (Exception ignored) {

                }
//                try {
//                    Thread.sleep(15 * 60 * 1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        }).start();
    }


}
