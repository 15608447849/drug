package elasticsearch;

import java.net.InetAddress;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;


public class ElasticSearchClientFactory  {

	static TransportClient client = null;
	static PreBuiltTransportClient preBuiltTransportClient = null;
	static String host = null;
	static int port = 0;
	static String clusterName = null;
	static Settings settings;

	public static void init(){

	}

	static{
		host = ElasticSearchConfig.ES_CONFIG.pro_host;
		port = Integer.parseInt(ElasticSearchConfig.ES_CONFIG.pro_port);
		clusterName  = ElasticSearchConfig.ES_CONFIG.pro_clusterName;
		synchronized (ElasticSearchClientFactory.class){
			if(client == null){
				settings = Settings.builder().put("cluster.name", clusterName).build();
				try{

					preBuiltTransportClient = new PreBuiltTransportClient(settings);
					client = preBuiltTransportClient
							.addTransportAddress(new TransportAddress(InetAddress.getByName(host), port));
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
	}
	
	public static TransportClient getClientInstance() {
		if(client == null){
			synchronized (ElasticSearchClientFactory.class){
				if(client == null){
					settings = Settings.builder().put("cluster.name", clusterName).build();
					try{

						preBuiltTransportClient = new PreBuiltTransportClient(settings);
						client = preBuiltTransportClient
								.addTransportAddress(new TransportAddress(InetAddress.getByName(host), port));
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}
		}
		return client;
	}
	
}
