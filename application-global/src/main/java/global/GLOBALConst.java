package global;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName GLOBALConst
 * @Description TODO
 * @date 2019-03-27 19:51
 */
public class GLOBALConst {

    public static final int _SMALLINTMAX = 65535;
    /**切分数据库服务器规则：每8192个公司共用一台数据库服务器*/
    public static final int _DMNUM = 8192;
    /**同一台数据库服务器里切分数据库规则：每8192个公司共用一个库，也就是说每台数据库服务器上会有8个数据库*/
    public static final int _MODNUM_EIGHT = 8;
    /**初始企业码*/
    public static final int  COMP_INIT_VAR =  536862720;
}
