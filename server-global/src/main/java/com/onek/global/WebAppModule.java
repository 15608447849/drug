package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.util.fs.FileServerUtils;
import util.GsonUtils;
import util.http.HttpRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/5/17 16:46
 */
public class WebAppModule {

    private static class Bean{
        public Bean(String container, String fragment, String name) {
            this.container = container;
            this.fragment = fragment;
            this.name = name;
        }

        String container;
        String fragment;
        String name;
        HashMap<String,String> map;

    }

    @UserPermission(ignore = true)
    public String pageInfo(AppContext context){
        return new HttpRequest().accessUrl(FileServerUtils.fileDownloadPrev()+"/page.json").getRespondContent();
    }

    public static void main(String[] args) {
        List<Bean> list = new ArrayList<>();
        Bean b = new Bean("content", "com.bottom.wvapp.fragments.WebFragment","web");
        b.map = new HashMap<>();
        b.map.put("url","www.baidu.com");
        list.add(b);
        list.add(new Bean("content", "com.bottom.wvapp.fragments.TestFragment","web"));

        System.out.println(GsonUtils.javaBeanToJson(list));
    }
}
