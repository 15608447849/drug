package com.onek.util;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import util.StringUtils;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@PropertiesFilePath("/mail.properties")
public class EmailUtil extends ApplicationPropertiesBase {
    //发件人地址
    @PropertiesName("mail.senderAddress")
    private String senderAddress;
    //发件人账户名
    @PropertiesName("mail.senderAccount")
    private String senderAccount;
    //发件人授权码
    @PropertiesName("mail.senderPassword")
    private String senderPassword;
    //用户认证方式
    @PropertiesName("mail.smtp.auth")
    private String smtp_auth;
    //传输协议
    @PropertiesName("mail.transport.protocol")
    private String transport_protocol;
    //发件人的SMTP服务器地址
    @PropertiesName("mail.smtp.host")
    private String smtp_host;

    private static final EmailUtil HOLDER = new EmailUtil();

    private final Properties MAIL_PROP = new Properties();

    private EmailUtil() {
        MAIL_PROP.setProperty("mail.smtp.auth", smtp_auth);
        MAIL_PROP.setProperty("mail.transport.protocol", transport_protocol);
        MAIL_PROP.setProperty("mail.smtp.host", smtp_host);
        MAIL_PROP.setProperty("mail.smtp.port", "465");
        MAIL_PROP.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        MAIL_PROP.setProperty("mail.smtp.socketFactory.port", "465");
    }

    public static EmailUtil getEmailUtil() {
        return HOLDER;
    }

    public boolean sendEmail(String content, String targetMail) {
        if (!StringUtils.isEmail(targetMail)) {
            return false;
        }

        try {
            Session session = Session.getInstance(MAIL_PROP);
            session.setDebug(true);
            Message msg = getMimeMessage(session, targetMail, content);
            Transport transport = session.getTransport();
            transport.connect(senderAccount, senderPassword);
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private MimeMessage getMimeMessage(Session session, String targetMail, String content) throws Exception{
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(senderAddress));
        msg.setRecipients(MimeMessage.RecipientType.TO, new InternetAddress[] {
                new InternetAddress(targetMail)});
        msg.setSubject("【一块医药】邮箱认证信息", "UTF-8");
        msg.setContent(content, "text/html;charset=UTF-8");
        msg.setSentDate(new Date());
        return msg;
    }
}
