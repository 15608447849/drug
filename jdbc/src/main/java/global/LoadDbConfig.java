package global;

import org.hyrdpf.dao.jdbc.JdbcPoolSessionMgr;
import org.hyrdpf.ds.AppConfig;
import org.hyrdpf.util.GeneralHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName LoadDbConfig
 * @Description TODO
 * @date 2019-03-12 18:48
 */
public class LoadDbConfig {

    static {
        /** 初始化LOG4J2日志环境 */
        AppConfig.initLogger("log4j2.xml");
        /** 初始化应用程序环境，如数据源等 */
        AppConfig.initialize();
    }

    public static void initConfig(){}

    public static int getDbMasterNum() {
        Properties props = new Properties();
        try {
            props.load(GeneralHelper.getClassResourceAsStream(JdbcPoolSessionMgr.class, "Segmentation.cfg.properties"));
            return Integer.parseInt(props.getProperty("master"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setDbMasterNum(int master) {
        Properties props = new Properties();
        FileOutputStream propsFile = null;
        try {
            props.load(GeneralHelper.getClassResourceAsStream(JdbcPoolSessionMgr.class, "Segmentation.cfg.properties"));
            props.setProperty("master", master + "");
            propsFile = new FileOutputStream(GeneralHelper.getClassResourcePath(JdbcPoolSessionMgr.class, "Segmentation.cfg.properties"));
            props.store(propsFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (propsFile != null) {
                try {
                    propsFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                propsFile = null;
            }
        }
    }




}
