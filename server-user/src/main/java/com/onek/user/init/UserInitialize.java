package com.onek.user.init;

import com.onek.server.infimp.IIceInitialize;

import static com.onek.user.operations.StoreBasicInfoOp.updateCompInfoToCache;

public class UserInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        //启动线程更新企业信息
        new Thread(()->{

            while (true){
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    updateCompInfoToCache();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                try {
//                    Thread.sleep(15 * 60 * 1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        }).start();
    }

    @Override
    public int priority() {
        return 1;
    }


}
