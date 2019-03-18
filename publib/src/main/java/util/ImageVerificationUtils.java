package util;

import util.http.HttpRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;

/**
 * @Author: leeping
 * @Date: 2019/3/13 11:36
 */
public class ImageVerificationUtils {

    /**
     * 生成随机验证码 String
     */
    public static String getRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int i = 0;
        while (i < length) {
            int t = random.nextInt(123);
            if ((t >= 97 || (t >= 65 && t <= 90) || (t >= 48 && t <= 57))) {
                sb.append((char) t);
                i++;
            }
        }
        System.out.println(sb);
        return sb.toString();
    }
    /**
     * 随机生成图形验证码
     */
    public static InputStream generateImage(int width,int height,String code) throws IOException {
        BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0,0,width,height);
        int interLine = code.length()-1;
        Random r=new Random();
        if(interLine>0){
            int x=r.nextInt(4),y=0;
            int x1=width-r.nextInt(4),y1=0;
            for(int i=0;i<interLine;i++){
                g.setColor(Color.YELLOW);
                y=r.nextInt(height-r.nextInt(4));
                y1=r.nextInt(height-r.nextInt(4));
                g.drawLine(x,y,x1,y1);
            }
        }
        int fontSize=(int)(height*0.7);//字体大小为图片高度的0.7
        int x=0;
        int y;
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,fontSize));
        for(int i=0;i<code.length();i++){
            y =(int)((Math.random()*0.3+0.7)*height);//每个字符高低随机
            g.setColor(Color.WHITE);
            g.drawString(code.charAt(i)+"",x,y);
            x += (width / code.length()) * (Math.random() * 0.3 + 0.8); //依据宽度浮动
        }
        float yawpRate = 0.05f;// 噪声率
        int area = (int) (yawpRate * width * height);//噪点数量
        for (int i = 0; i < area; i++) {
            int xxx = r.nextInt(width);
            int yyy = r.nextInt(height);
            int rgb = Color.BLACK.getRGB();
            image.setRGB(xxx, yyy, rgb);
        }
        g.dispose();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }




}
