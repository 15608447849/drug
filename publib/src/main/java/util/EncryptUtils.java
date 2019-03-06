package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by user on 2017/6/5.
 */
public class EncryptUtils {

    public static byte[] getFileMd5(File file,long startPort,long endPort){
        byte[] result = null;
        RandomAccessFile randomAccessFile = null;
        try {

            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(startPort);
            byte[] buffer = new byte[(int) (endPort - startPort)];
            randomAccessFile.read(buffer);
            result = getBytesMd5(buffer);
        } catch (Exception e) {
        }finally {
            if (randomAccessFile!=null){
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                }
            }
        }
        return result;
    }
    /**
     * 获取文件md5的byte值
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static byte[] getFileMd5(File file){
        byte[] result = null;
        try {
            try(DigestInputStream digestInputStream = new DigestInputStream(new FileInputStream(file),MessageDigest.getInstance("MD5"))){
                byte[] buffer =new byte[512];
                while (digestInputStream.read(buffer) > 0);
                result = digestInputStream.getMessageDigest().digest();
            }
        } catch (Exception e) {
        }
        return result;
    }
    /**
     * 获取文件MD5的String
     * @param file
     * @return
     * @throws IOException
     */
    public static String getFileMd5ByString(File file) throws Exception {
        return byteToHexString(getFileMd5(file));
    }
    /**
     * 获取一段字节数组的md5
     * @param buffer
     * @return
     */
    public static byte[] getBytesMd5(byte[] buffer) {
        byte[] result = null;
        try {
            result =  MessageDigest.getInstance("MD5").digest(buffer);
        } catch (Exception e) {
        }
        return result;
    }
    /**
     * 获取一段字节数组的md5
     * @param buffer
     * @return
     */
    public static byte[] getBytesMd5(byte[] buffer,int offset,int len) {
        byte[] result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(buffer,offset,len);
            result =  md.digest();
        } catch (Exception e) {
        }
        return result;
    }
    public static String getBytesMd5ByString(byte[] buffer){
        return byteToHexString(getBytesMd5(buffer));
    }
    public static String getBytesMd5ByString(byte[] buffer,int offset,int len){
        return byteToHexString(getBytesMd5(buffer,offset,len));
    }
    /**
     * byte->16进制字符串
     * @param bytes
     * @return
     */
    public static String byteToHexString(byte[] bytes) {
        StringBuffer hexStr = new StringBuffer();
        int num;
        for (int i = 0; i < bytes.length; i++) {
            num = bytes[i];
            if(num < 0) {
                num += 256;
            }
            if(num < 16){
                hexStr.append("0");
            }
            hexStr.append(Integer.toHexString(num));
        }
        return hexStr.toString().toUpperCase();
    }

    /**
     * 比较MD5字节数组
     * @param digesta
     * @param digestb
     * @return
     */
    public static boolean isEqualMD5(byte[] digesta,byte[] digestb){
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            return messagedigest.isEqual(digesta,digestb);
        } catch (NoSuchAlgorithmException e) {
        }
        return false;
    }

    public static boolean isEqualFileMd5(File src,File dest){
        return isEqualMD5(getFileMd5(src), getFileMd5(dest));
    }

    //Mark Adler发明的adler-32算法
    public static  String adler32Hex(byte[] data){
        int a =1,b = 0;
        for (int index = 0; index < data.length; ++index)
        {
            a = (a + data[index]) % 65521;
            b = (b + a) % 65521;
        }
        return Integer.toHexString((b << 16) | a);
    }

    /**
     * MD5 string 加密
     * @param str
     * @return
     */
    public static String encryption(String str){
        return byteToHexString(getBytesMd5(str.getBytes()));
    }


    //Mark Adler发明的adler-32算法
    public static  String adler32Hex(byte[] data,int offset,int len){
        int a =1,b = 0;
        for (int index = offset; index < len; ++index)
        {
            a = (a + data[index]) % 65521;
            b = (b + a) % 65521;
        }
        return Integer.toHexString((b << 16) | a);
    }
}
