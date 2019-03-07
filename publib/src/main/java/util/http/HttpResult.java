package util.http;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * 配合ftc文件服务器使用
 * lzp
 */
public class HttpResult extends HttpUtil.CallbackAbs  {

    private String text;

    void bindParam(StringBuffer sb,Map<String,String > map){
        Iterator<Map.Entry<String,String>> it = map.entrySet().iterator();
        Map.Entry<String,String> entry ;

        while (it.hasNext()) {
            entry = it.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.deleteCharAt(sb.length()-1);
    }

    public HttpResult accessUrl(String url){
        new HttpUtil.Request(url,this)
                .setReadTimeout(1000)
                .setConnectTimeout(1000)
                .text()
                .execute();
        return this;
    }

    private List<String> pathList = new ArrayList<>();
    private List<String> nameList = new ArrayList<>();

    private List<HttpUtil.FormItem> formItems = new ArrayList<>();

    /**
     * 上传文件
     */
    public HttpResult addFile(File file,String remotePath,String remoteFileName){
        if (remotePath==null) remotePath = "/java/";
        if (remoteFileName==null) remoteFileName = file.getName();
        pathList.add(remotePath);
        nameList.add(remoteFileName);
        formItems.add(new HttpUtil.FormItem("file", file.getName(), file));
        return this;
    }

    /**
     * 上传流
     */
    public HttpResult addStream(InputStream stream, String remotePath, String remoteFileName){
        if (remotePath==null) remotePath = "/java/";
        if (remoteFileName==null) throw new NullPointerException("需要上传的远程文件名不可以为空");
        pathList.add(remotePath);
        nameList.add(remoteFileName);
        formItems.add(new HttpUtil.FormItem("file", remoteFileName, stream));
        return this;
    }

    private static String join(List list, String separator) {
        StringBuffer sb = new StringBuffer();
        for (Object obj : list){
            sb.append(obj.toString()).append(separator);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * 文件上传地址
     */
    public HttpResult fileUploadUrl(String url){
        HashMap<String,String> headParams = new HashMap<>();
        headParams.put("specify-path",join(pathList,";"));
        headParams.put("specify-filename",join(nameList,";"));
        new HttpUtil.Request(url, HttpUtil.Request.POST, this)
                .setFormSubmit()
                .setParams(headParams)
                .addFormItemList(formItems)
                .upload().execute();
        return this;
    }

    /**
     * 获取文件列表
     */
    public HttpResult getTargetDirFileList(String url,String dirPath,boolean isSub){
        HashMap<String,String> headParams = new HashMap<>();
        headParams.put("specify-path",dirPath);
        headParams.put("ergodic-sub",isSub+"");
        new HttpUtil.Request(url, HttpUtil.Request.POST, this)
                .setParams(headParams)
                .setReadTimeout(1000).
                setConnectTimeout(1000)
                .text()
                .execute();
        return this;
    }

    //获取返回的文本信息
    public String getRespondContent(){
        return text;
    }

    @Override
    public void onResult(HttpUtil.Response response) {
        this.text = response.getMessage();
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
        this.text = e.toString();
    }

}
