package com.onek;

import util.StringUtils;

/**
 * @Author: leeping
 * @Date: 2019/3/20 11:00
 */
public class FileServerUtils {
    // 文件上传地址
    public static String fileUploadAddress(){
        return "http://" + GlobalProperties.INSTANCE.fileServerAddress +"/upload";
    }
    // 文件下载地址 前缀 ,例如 下载文件 /目录/文件.png -> 下载前缀/目录/文件.png
    public static String fileDownloadPrev(){
        return "http://" + GlobalProperties.INSTANCE.fileServerAddress ;
    }

}