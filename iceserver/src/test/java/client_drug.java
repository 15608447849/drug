import com.onek.server.inf.IParam;
import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;

public class client_drug {
    public static void main(String[] args) {
        System.out.println("【客户端开始测试】");
        int status = 0;
        Ice.Communicator ic = null;
        try {
            //node1.cfg中的链接地址
            ic = Ice.Util.initialize(new String[] {"--Ice.Default.Locator=DemoIceGrid/Locator:tcp -h localhost -p 4061"});
            Ice.ObjectPrx base = ic.stringToProxy("userServer"); //InfoModele100-0001:
            String s  = ic.proxyToString(base);
            //在generated中找到接口对应的Helper
            InterfacesPrx prx = InterfacesPrxHelper.checkedCast(base);
            if(prx == null){
                throw new Error("Invalid proxy");
            }
            IRequest request = new IRequest();


            request.cls = "UserServerImp";
            request.method = "login";

            request.param = new IParam();
            request.param.json = "平哥,很帅!";


            //调用服务方法
            String res = prx.accessService(request);
            System.out.println(res);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            status = 1;
            e.printStackTrace();
        }finally {
            if(ic!=null){
                ic.destroy();
            }
        }
        System.exit(status);
    }
}
