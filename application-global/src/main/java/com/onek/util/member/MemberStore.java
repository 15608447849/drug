package com.onek.util.member;

import redis.IRedisPartCache;
import redis.proxy.CacheProxyInstance;

import java.math.BigDecimal;

public class MemberStore {

    private static IRedisPartCache memProxy =(IRedisPartCache) CacheProxyInstance.createPartInstance(new MemberUtil());

    public static int getLevelByCompid(int compid){

        int point = 0;
        if(compid > 0){

            MemberEntity memberEntity = (MemberEntity) memProxy.getId(compid);
            if(memberEntity != null){
                point = memberEntity.getAccupoints();
            }

        }
        return getLevelByPoint(point);
    }

    public static int getIntegralByCompid(int compid){

        if(compid > 0){

            MemberEntity memberEntity = (MemberEntity) memProxy.getId(compid);
            if(memberEntity != null){
                return memberEntity.getBalpoints();
            }

        }
        return 0;
    }

    public static int addPoint(int compid, int point){

        if(compid > 0) {
            return memProxy.add(compid, point);
        }
        return 0;
    }

    public static int getLevelByPoint(int point){
        double val = Math.abs(Math.sqrt((point + 250) / 1000)) - 0.5;

        int level = BigDecimal.valueOf(val).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
        if(level < 0){
            level = 1;
        }
        return level;
    }
//
//    public static void main(String[] args) {
//        System.out.println(MemberStore.getLevelByCompid(536862722));
//    }
}
