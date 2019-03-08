package elasticsearch;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.elasticsearch.client.transport.TransportClient;

public class EsClientPoolFactory implements PooledObjectFactory<TransportClient>{

	@Override
	public void activateObject(PooledObject<TransportClient> arg0) throws Exception {
		
	}

	@Override
	public void destroyObject(PooledObject<TransportClient> pooledObject) throws Exception {
		TransportClient client = pooledObject.getObject();
		if(client != null) {
			client.close();
		}
      
	}

	@Override
	public PooledObject<TransportClient> makeObject() throws Exception {
//		ElasticSearchClientFactory clientFactory = new ElasticSearchClientFactory();
//        TransportClient client = null;
//        try {
//            client = clientFactory.getClientInstance();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return new DefaultPooledObject<TransportClient>(ElasticSearchClientFactory.getClientInstance());

	}

	@Override
	public void passivateObject(PooledObject<TransportClient> pooledObject) throws Exception {
		

	}

	@Override
	public boolean validateObject(PooledObject<TransportClient> arg0) {
		return false;
	}

}
