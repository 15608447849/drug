package util.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Created by Leeping on 2018/7/29.
 * email: 793065165@qq.com
 *
*/
public class HttpUtil {

    /**
     * 回调接口
     */
    public interface Callback{
        void onProgress(File file, long progress, long total);
        void onResult(Response response);
        void onError(Exception e);
    }

    /**
     * 回调接口抽象类
     */
    public static class CallbackAbs implements Callback {

        @Override
        public void onProgress(File file, long progress, long total) {

        }

        @Override
        public void onResult(Response response) {

        }

        @Override
        public void onError(Exception e) {
            onResult(new Response(e));
        }
    }

    /**
     * 模拟form 表单
     */
    private static final class HttpFrom{
        final static String LINEND = "\r\n";
        final static String PREFFIX = "--";
        final static String BOUNDARY = "*****";//定义的分割符号
        final static String CONTENT_TYPE = "multipart/form-data;boundary="+BOUNDARY;
        final static String PREV = LINEND+PREFFIX + BOUNDARY + LINEND;
        final static String SUX = "Content-Type: application/octet-stream"+ LINEND+LINEND;
        final static String END = PREFFIX + BOUNDARY + PREFFIX + LINEND;
        static String fileExplain(String filedName, String fileName){
            return "Content-Disposition: form-data;" +  //类型
                    "name=\""+filedName+"\";" +    //域名
                    "filename=\"" + fileName + "\";"//文件名
                    + LINEND;
        }
        static String textExplain(String key,String value){
            return "Content-Disposition: form-data;" +  //类型
                    "name=\""+key+"\";"     //域名
                    +LINEND+LINEND + value;
        }
    }

    //表单项
    public static final class FormItem{
        boolean isFile = false;
        boolean isStream = false;
        String field;
        String file;
        String key;
        String value;
        File fileItem;
        InputStream inputStream;

        //表单文件
        public FormItem(String field, String file, File fileItem) {
            this.field = field;
            this.file = file;
            this.fileItem = fileItem;
            isFile = true;
        }

        //表单文件
        public FormItem(String field, String file, InputStream inputStream) {
            this.field = field;
            this.file = file;
            this.inputStream = inputStream;
            isFile = true;
            isStream = true;
        }
        //表单文本
        public FormItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    //请求
    public static final class Request implements Runnable{

        public static final String GET = "GET";

        public static final String POST = "POST";

        // true 上传文件
        private boolean isUpdate = false;
        // true 下载文件
        private boolean isDownload = false;
        //true 访问文本
        private boolean isText = false;

        // 服务器地址
        private String url;
        // 访问类型
        private String type = GET;

        // 是否表单数据提交 默认 = false
        private boolean isForm = false;

        //是否 流数据传输
        private boolean isStream = false;

        // 如果是表单数据提交 ,提交的表单项
        private List<FormItem> formList = new ArrayList<>();

        // 需要下载的文件的存储在本地的位置
        private File downloadFileLoc;

        /** 连接超时 */
        private int connectTimeout = 60 * 1000;
        /** 读取超时 */
        private int readTimeout = 30 * 1000;
        /** header 参数 */
        private Map<String,String> params;
        /** 监听回调 */
        private Callback callback;

        private final int locCacheByteMax = 1024*8;

        public Request(String url) {
            this(url,GET,null);
        }

        public Request(String url, Callback callback) {
            this(url,GET,callback);
        }

        public Request(String url, String type, Callback callback) {
            this.url = url;
            this.type = type;
            this.callback = callback;
        }

        public Request download(){
            isUpdate = false;
            isText = false;
            isDownload = true;
            return this;
        }

        public Request upload(){
            isUpdate = true;
            isText = false;
            isDownload = false;
            return this;
        }

        public Request text(){
            isUpdate = false;
            isText = true;
            isDownload = true;
            return this;
        }

        public Request setUrl(String url) {
            this.url = url;
            return this;
        }

        public Request setType(String type) {
            this.type = type;
            return this;
        }

        public Request setParams(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Request setCallback(Callback callback) {
            this.callback = callback;
            return this;
        }

        //表单数据
        public Request setFormSubmit() {
            isUpdate = true;
            isForm = true;
            isStream = false;
            return this;
        }

        //流传输
        public Request setStream(){
            isUpdate = true;
            isForm = false;
            isStream = true;
            return this;
        }

        public Request setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Request setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        //添加表单项
        public Request addFormItemList(List<FormItem> items){
            for (FormItem item:items) addFormItem(item);
            return this;
        }

        public Request addFormItem(FormItem file) {
            formList.add(file);
            return this;
        }

        public Request setDownloadFileLoc(File downloadFileLoc) {
            this.downloadFileLoc = downloadFileLoc;
            return this;
        }

        public int getLocCacheByteMax() {
            return locCacheByteMax;
        }

        public void execute() {
            if (isDownload){
                fileDownload(this);
            }else if (isUpdate){
                fileUpload(this);
            }
        }

        @Override
        public void run() {
            execute();
        }
    }

    //响应
    public static final class Response {
        private String message;
        private Object data;
        private boolean isSuccess;
        private boolean isError;
        private Exception exception;

        Response() {

        }

        Response(Object data){
            this.data = data;
            this.isSuccess = true;
        }

        Response(Exception exception) {
            this.exception = exception;
            isError = true;
        }

        Response(boolean isSuccess,String message) {
            this.isSuccess = isSuccess;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public <T> T getData() {
            return (T)data;
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public boolean isError() {
            return isError;
        }

        public Exception getException() {
            return exception;
        }
    }

    /**
     * 文件上传
     */
    private static void fileUpload(Request request) {
        Callback callback = request.callback;
        HttpURLConnection con = null;
        OutputStream out = null;
        InputStream in = null;
        try {
            URL url = new URL(request.url);
            con = (HttpURLConnection) url.openConnection();
            connectionSetting(con, request);
            connectionAddHeadParams(con,request);
            out = con.getOutputStream();
            updateFileByForm(out,request,callback);
//            updateFileByStream(out,request,callback); 未实现
            con.connect(); //连接服务
            if ( con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                in = con.getInputStream();
                String result = inputStreamToString(in);
                if (callback!=null) callback.onResult(new Response(true,result));
            } else {
                if (callback!=null) callback.onResult(new Response());
            }

        } catch (Exception e) {
            if (callback!=null) callback.onError(e);
        } finally {
          closeIo(out,in);
          if (con!=null) con.disconnect();//断开连接
        }
    }

    //流转字符串信息
    public static String inputStreamToString(InputStream inputStream) {
        String result = null;
        BufferedReader reader = null;
        try {
            reader =  new BufferedReader(new InputStreamReader(inputStream,"utf-8"));
            StringBuffer resultBuffer = new StringBuffer();
            String tempLine;
            while ((tempLine = reader.readLine()) != null) {
                resultBuffer.append(tempLine);
            }
            result = resultBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            closeIo(reader);
        }
        return result;
    }

    /*
     表单传输 : https://blog.csdn.net/wangpeng047/article/details/38303865
                1. 定义分隔符 BOUNDARY
                2. 设置  "Content-Type", "multipart/form-data; boundary=" + BOUNDARY
                3.文件体:
     提交文本: 键值对
     while(){
     strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
     strBuf.append("Content-Disposition: form-data; name=\"" + inputName + "\"\r\n\r\n");
     strBuf.append(inputValue);
     }
     out.write(strBuf);
     提交文件:
     while(){
     strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
     strBuf.append("Content-Disposition: form-data; name=\"" + filedName + "\"; filename=\"" + filename + "\"\r\n");
     strBuf.append("Content-Type:" + contentType + "\r\n\r\n");
     out.write(strBUf);
     out.write(文件流);
     }
     out.write("\r\n--" + BOUNDARY + "--\r\n") 表单标识.
    上传表单
    */
    private static void updateFileByForm(OutputStream out, Request request,Callback callback) throws Exception{
        //判断表单是否有效
        if (!request.isForm || request.formList==null || request.formList.size() == 0) return ;

        for(FormItem item : request.formList){
                //表单数据
                if (!item.isFile) {
                    //文本类型
                    writeBytesByForm(out, HttpFrom.PREV); //前缀
                    writeBytesByForm(out, HttpFrom.textExplain(item.key,item.value)); //表单说明信息
                }else{
                    //文件类型
                    if (!item.isStream && !item.fileItem.exists()){
                        throw new FileNotFoundException(item.fileItem.getAbsolutePath());
                    }
                    writeBytesByForm(out, HttpFrom.PREV); //前缀
                    writeBytesByForm(out, HttpFrom.fileExplain(item.field,item.file)); //表单说明信息
                    writeBytesByForm(out, HttpFrom.SUX); //后缀

                    if (item.isStream){
                        writeInputStreamToOut(out,item.inputStream,request,callback);//接入流
                    }else{
                        writeFileStreamToOut(out,item.fileItem,request,callback);//接入文件流
                    }
                }
        }

        //添加表单后缀
        writeBytesByForm(out, HttpFrom.LINEND);
        writeBytesByForm(out, HttpFrom.END);
        out.flush(); //刷新流
    }

    /**写字节流*/
    private static void writeBytesByForm(OutputStream out, String s) throws IOException {
        out.write(s.getBytes());
    }

    /** 写入文件流到服务器*/
    private static void writeFileStreamToOut(OutputStream out, File fileItem,Request request,Callback callback){
            //文件流
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileItem); //文件流
                //缓存数据字节
                byte[] cache = new byte[request.getLocCacheByteMax()];
                long total = fileItem.length();//文件大小
                long progress = 0;//当前进度
                int len = 0;//传输数据量
                while ((len = fis.read(cache)) != -1) {
                    out.write(cache, 0, len);
                    progress +=len;
                    if (callback!=null) callback.onProgress(fileItem,progress,total);
                }
            } catch (IOException e) {
                if (callback!=null) callback.onError(e);
            }finally {
                closeIo(fis);
            }
    }

    /** 写流上传 */
    private static void writeInputStreamToOut(OutputStream out,InputStream inputStream, Request request, Callback callback){
        try {
            //缓存数据字节
            byte[] cache = new byte[request.getLocCacheByteMax()];
            int len;
            while ((len = inputStream.read(cache)) != -1) {
                out.write(cache, 0, len);
            }
        } catch (IOException e) {
            if (callback!=null) callback.onError(e);
        }finally {
            closeIo(inputStream);
        }

    }

    /**文件下载*/
    private static void fileDownload(Request request){

        Callback callback = request.callback;
        HttpURLConnection con = null;
        OutputStream out = null;//本地文件输出流
        InputStream in = null; //服务器下载输入流
        try {
            URL url = new URL(request.url);
            con = (HttpURLConnection) url.openConnection();
            connectionSetting(con,request);
            connectionAddHeadParams(con,request);
            con.connect();//连接
            int code = con.getResponseCode();
            String message = con.getResponseMessage();
            if ( code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL) {
                in = con.getInputStream();
                if (request.isText){
                    //访问文本信息
                    String text = inputStreamToString(in);
                    if (callback!=null) callback.onResult(new Response(true, text)
                    );
                }else{
                    //下载文件
                    long fileLength = con.getContentLength();
                    if (fileLength<=0) fileLength = con.getContentLengthLong();
                    if (fileLength <= 0) throw new IllegalArgumentException("远程服务器文件不存在");
                    writeServiceStreamToFile(in,fileLength,request,callback);
                }

            }else{
                if (callback!=null) callback.onResult(
                        new Response(
                        false,
                        "response code = "+code+", response message = "+ message)
                );
            }
        }catch (Exception e){
            if (callback!=null) callback.onError(e);
        }finally {
            closeIo(out,in);
            if (con!=null) con.disconnect();//断开连接
        }
    }

    //服务器流内容写入文件
    private static void writeServiceStreamToFile(InputStream in, long total,Request request, Callback callback) {
        File file = request.downloadFileLoc;
        if (file==null) throw new IllegalArgumentException("没有设置本地文件路径");
        FileOutputStream fos = null;
        try{
            boolean isDown = file.exists() && file.length() == total;
            if (!isDown){
                fos = new FileOutputStream(file);
                byte[] cache = new byte[request.getLocCacheByteMax()];
                long progress = 0;//当前进度
                int len = 0;//传输数据量

                while ( (len = in.read(cache)) > 0 ){
                    fos.write(cache,0,len);
                    fos.flush();
                    progress+=len;
                    if (callback!=null) callback.onProgress(file ,progress,total);
                }
                if (progress != total ){
                    throw new IllegalStateException("下载进度("+progress+")与文件大小("+total+")不匹配");
                }
            }

            if (callback != null) callback.onResult(new Response(file));
        }catch (Exception e){
            if (callback!=null) callback.onError(e);
        }finally {
            closeIo(fos);
        }
    }

    //连接参数
    private static void connectionSetting(HttpURLConnection con,Request request) throws Exception {
        con.setRequestMethod(request.type);// GET POST

        con .setUseCaches(false);
        con.setDefaultUseCaches(false);

        if (request.connectTimeout>0){
            con.setConnectTimeout(request.connectTimeout);
        }
        if (request.readTimeout>0){
            con.setReadTimeout(request.readTimeout);
        }

        con.setRequestProperty("Charset", "UTF-8");
        con.setRequestProperty("Connection", "keep-alive");  //设置连接的状态
        con.setRequestProperty("Connection", "keep-alive");  //设置连接的状态

        if (request.isUpdate){
            con.setDoOutput(true);
            con.setChunkedStreamingMode(0);//直接将流提交到服务器上
        }
        if (request.isDownload){
            con.setDoInput(true);
            con.setRequestProperty("Accept-Encoding", "identity");
            con.setRequestProperty("Range", "bytes=" + 0 + "-");
        }

        if (request.isForm){
            con.setRequestProperty("Content-Type", HttpFrom.CONTENT_TYPE);//表单传输
        }else{
            con.setRequestProperty("Content-Type", "application/octet-stream");//传输数据类型,流传输
        }

    }

    //添加头信息
    private static void connectionAddHeadParams(HttpURLConnection con, Request request) {
        Iterator<Map.Entry<String,String>> iterator;
        Map.Entry<String,String> entry;
        Map<String,String> map = request.params;
        if (map!=null){
            iterator = map.entrySet().iterator();
            while (iterator.hasNext()){
                entry = iterator.next();
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    //关闭连接
    public static void closeIo(Closeable... closeable){
        for (Closeable c: closeable){
            if (c == null) continue;
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static String formText(String url, String type, Map<String,String> params){



        String text = null;
        HttpURLConnection con = null;

        try{

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                sb.append(e.getKey());
                sb.append("=");
                sb.append(URLEncoder.encode(e.getValue(),"UTF-8"));
                sb.append("&");
            }
            sb.substring(0, sb.length() - 1);

            String content = sb.toString();
            if (type .equals("GET") && params.size()>0){
                url += "?" +content ;
            }

            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod(type);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (type.equals("POST")){
                OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8);
                osw.write(content);
                osw.flush();
                osw.close();
//                con.getOutputStream().write(content.getBytes("ISO-8859-1"));
//                con.getOutputStream().flush();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            sb.delete(0,sb.length());
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            text = sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (con!=null) {
                con.disconnect();
                con = null;
            }
        }
        return text;
    }

}
