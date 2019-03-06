package util;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by lzp on 2017/5/9.
 *
 */
public class FileUtils {
    public static final String PROGRESS_HOME_PATH = ".";
    public static final String SEPARATOR = "/";//File.separator;
    private static byte[] ENDFD = "\n".getBytes();

    /**
     * 替换文件分隔符,检测文件前缀或后缀
     */
    public static String replaceFileSeparatorAndCheck(String path,String prefix,String suffix){
        path = path.replace("\\",SEPARATOR);
        if (!StringUtils.isEmpty(prefix)){
            if (path.startsWith(prefix)){
                path = path.substring(1);
            }
        }
        if (!StringUtils.isEmpty(suffix)){
            if (path.endsWith(suffix)){
                path = path.substring(0,path.length()-1);
            }
        }
        return  path;
    }

    /**检查目录 存在返回true*/
    public static boolean checkDir(String dir){
        File dirs = new File(dir);
        if(!dirs.exists()){
//            ("mkdirs file : "+ dirs.getAbsolutePath());
            return dirs.mkdirs();
        }
        return true;
    }

    /**
     * 删除单个文件
     *
     * @param sPath
     *            被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String sPath) {

            File file = new File(sPath);
        if ( file.exists() && file.isFile()) {
//            ("delete file : "+ file.getAbsolutePath());
            return file.delete();
        }
        return false;
    }

    public static void closeStream(FileChannel in, FileChannel out){
        try {
            if (in != null) in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void closeStream(InputStream in, OutputStream out, RandomAccessFile raf,HttpURLConnection httpConnection){
        try {
            if (in != null) in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (httpConnection != null) httpConnection.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (out != null) out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (raf != null) raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //从命名 不会删除源文件,需要自行判断决定是否删除
    public static boolean rename(File sourceFile,File targetFile){
        try {
            if (sourceFile.renameTo(targetFile)){
                return true;
            }else{
                if (copyFile(sourceFile, targetFile)){
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean copyFile(File source,File target) throws IOException {

        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new RandomAccessFile(source,"rw").getChannel();
            out = new RandomAccessFile(target,"rw").getChannel();
            in.transferTo(0, in.size(), out);//连接两个通道，并且从in通道读取，然后写入out通道
            out.close();
            in.close();
//            ("启动文件复制: "+ source+ " - "+ target+" 成功.");

            return true;
        }  catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeStream(in,out);
        }
    }

    /**
     * 保存对象到文件
     * @param obj
     */
    public static void writeObjectToFile(Object obj, String filePath)
    {


        File file =new File(filePath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            ObjectOutputStream objOut=new ObjectOutputStream(out);

            objOut.writeObject(obj);
            objOut.flush();
            objOut.close();
//            ("序列化","成功");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }



    public static Object readObjectFromFile(String filePath)
    {
        Object temp=null;
        File file =new File(filePath);
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            ObjectInputStream objIn=new ObjectInputStream(in);
            temp=objIn.readObject();
            objIn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }


    public static boolean writeMapToFile(Map<String,String> maps, String path){
        try {
            Map<String,String> map = maps;
            File file = new File(path);
            if (!file.exists()) file.createNewFile();

            StringBuilder str = new StringBuilder();
            Iterator iter = map.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry entry = (Map.Entry)iter.next();
                str.append(entry.getKey()+"="+entry.getValue()).append("\n");
            }
            FileWriter fw = new FileWriter(path, false);
            fw.write(str.toString());
            fw.flush();
            fw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Map<String,String> readFileToMap(String path){
        try {
            File file = new File(path);
            if (!file.exists()) return null;
            StringBuilder str = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line=null;
            String key;
            String val;
            Map<String,String> map = new HashMap<>();
            while ( (line = reader.readLine()) != null){
                key = line.substring(0,line.indexOf("="));
                val = line.substring(line.indexOf("=")+1);
                map.put(key,val);
            }
            reader.close();
            return map.size()>0?map:null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //指定位置存
    public static void writeByteToFilePoint(String path,byte[] content,int length,int point){
        RandomAccessFile raf = null;
        try {
            byte[] bytes =  new byte[length];
            System.arraycopy(content, 0, bytes, 0, content.length);
            bytes[content.length] = ENDFD[0];
            raf = new RandomAccessFile(path,"rw");
            raf.seek(point);
            raf.write(bytes,0,bytes.length); // 写进去的长度
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (raf!=null) raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 获取结束位置
     */
    private static int getEndPoint(byte[] content) {
        int length = content.length;
        for (int i = 0; i < length; i++) {
            if(ENDFD[0] == content[i]) return i;
        }
        return length;
    }
    public static String readFilePointToByte(String path,int point,int length){
        RandomAccessFile raf = null;
        try {
            byte[] bytes = new byte[length];
            raf = new RandomAccessFile(path,"r");
            raf.seek(point);
            raf.read(bytes);
            raf.close();
            int endPoint = getEndPoint(bytes);
            byte[] source = new byte[endPoint];
            System.arraycopy(bytes, 0, source, 0, endPoint);
            return  new String(source);
        } catch (Exception e) {
            ;
        }finally {
            try {
                if (raf!=null) raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getFileText(String tmpFile) {
        return getFileText(tmpFile,true);
    }
    public static String getFileText(String tmpFile,boolean isDelete) {
        FileChannel inChannel = null;
        try {
            inChannel = new RandomAccessFile(tmpFile,"rw").getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) inChannel.size());
            buffer.clear();
            inChannel.read(buffer);
            buffer.flip();
           return new String(buffer.array(),"UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (inChannel!=null){
                try {
                    inChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isDelete) deleteFile(tmpFile);
        }
        return null;
    }

    public static boolean checkFile(String path) {
        File file = new File(path);
        if (file.exists()) return true;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkFileNotCreate(String path) {
        File file = new File(path);
        if (file.exists()&& file.isFile()) return true;
        return false;
    }

    public static boolean writeStringToFile(String content, String pathDir,String fileName,boolean isAppend) {

        if (!checkDir(pathDir)) return false;
        File file = new File(pathDir+SEPARATOR+fileName);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        FileChannel out = null;
        try {
            out = new FileOutputStream(file,isAppend).getChannel();
           out.write(Charset.forName("UTF-8").encode(content));
           return true;
        } catch (IOException e) {
        } finally {
            closeStream(null,out);
        }
        return false;
    }

    public static boolean checkFileLength(String path,long totalSize) {
        File file = new File(path);
        if (file.exists() && file.isFile()){
           return file.length() == totalSize;
        }else{
            return false;
        }
    }

    public static String getFilePath(File file) {
        if (file==null) return null;
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }




    public static String readFileText(String path,String charset){
        if (StringUtils.isEmpty(charset)) charset = "UTF-8";
        try(FileInputStream fis = new FileInputStream(path)) {
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[1024];
            int len;
            while ( (len = fis.read(bytes))>0 ){
                sb.append( new String (bytes,0,len,charset));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static <T> T readFileJson2Object(String path,String charset,Class<T> classType){
        String json = readFileText(path,charset);

        if (json!=null){
            try {
                return new Gson().fromJson(json,classType);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
