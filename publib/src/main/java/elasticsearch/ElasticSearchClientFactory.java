package elasticsearch;

import java.net.InetAddress;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import util.StringUtils;


@PropertiesFilePath("/elasticsearch.properties")
public class ElasticSearchClientFactory extends ApplicationPropertiesBase {

	@PropertiesName("elasticsearch.host")
	public String pro_host ;
	@PropertiesName("elasticsearch.port")
	public String pro_port ;
	@PropertiesName("elasticsearch.cluster.name")
	public String pro_clusterName ;

	private static TransportClient client = null;
	private static PreBuiltTransportClient preBuiltTransportClient = null;
	private static String host = null;
	private static int port = 0;
	private static String clusterName = null;
	private static Settings settings;

	@Override
	protected void initialization() {
		super.initialization();
		host = StringUtils.trim(pro_host);
		port = Integer.parseInt(pro_port);
		clusterName = StringUtils.trim(pro_clusterName);

		settings = Settings.builder().put("cluster.name", clusterName).build();

	}
	static {


		try {
			preBuiltTransportClient = new PreBuiltTransportClient(settings);
			client = preBuiltTransportClient
					.addTransportAddress(new TransportAddress(InetAddress.getByName(host), port));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private ElasticSearchClientFactory() {
       
	}
	
	public static TransportClient getClientInstance() {
		return client;
	}
	
}
