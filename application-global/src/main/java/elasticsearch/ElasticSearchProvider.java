package elasticsearch;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.hyrdpf.util.LogUtil;

/**
 * 搜索引擎框架(ES)提供类
 * 
 * @author JiangWenGuang
 * @since 2018-7-2
 *
 */
public class ElasticSearchProvider {
	
//	public static GenericObjectPool<TransportClient> clientPool = null;
//	
//	static {
//		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();// 对象池配置类，不写也可以，采用默认配置
//		poolConfig.setMaxTotal(8);// 采用默认配置maxTotal是8，池中有8个client
//		EsClientPoolFactory esClientPoolFactory = new EsClientPoolFactory();// 要池化的对象的工厂类，这个是我们要实现的类
//		clientPool = new GenericObjectPool<>(esClientPoolFactory, poolConfig);// 利用对象工厂类和配置类生成对象池
//	}

	
	/**
	 * 添加索引和索引类型 指定索引类型字段的名字和类型
	 * 
	 * @param index 索引名
	 * @param type 索引类型
	 * @param mapping 用来构建属性类型对象
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回空对象 20180824
	 */
	public static CreateIndexResponse addIndex(String index, String type, XContentBuilder mapping) {
		TransportClient client = ElasticSearchClientFactory.getClientInstance();
		try {
//			client = clientPool.borrowObject(); 
			IndicesExistsRequest inExistsRequest = new IndicesExistsRequest(index);
			IndicesExistsResponse inExistsResponse = client.admin().indices().exists(inExistsRequest).actionGet();

			System.out.println("index ["+ index +"] is exist :" + inExistsResponse.isExists());
			if (!inExistsResponse.isExists()) {
				CreateIndexRequestBuilder cib = client.admin().indices().prepareCreate(index);
				cib.addMapping(type, mapping);
				CreateIndexResponse response = cib.execute().actionGet();
				System.out.println("add index isAcknowledged: " + response.isAcknowledged() + "; index :" + response.index());
				return response;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;

	}
	
	/**
	 * 删除索引
	 *
	 * @param index 索引名
	 * @param type 索引类型
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回false 20180824
	 */
	public static boolean delIndex(String index, String type) {

		TransportClient client = ElasticSearchClientFactory.getClientInstance();;
		try {
			IndicesExistsRequest inExistsRequest = new IndicesExistsRequest(index);
			IndicesExistsResponse inExistsResponse = client.admin().indices().exists(inExistsRequest).actionGet();

			System.out.println("index ["+ index +"] is exist :" + inExistsResponse.isExists());
			if (inExistsResponse.isExists()) {
				DeleteIndexResponse dResponse = client.admin().indices().prepareDelete(index).execute().actionGet();
				System.out.println("index ["+ index +"] delete status :" + dResponse.isAcknowledged());
				return dResponse.isAcknowledged();
			} else {
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;

		}

	}

	/**
	 * 添加文档
	 *
	 * @param object 文档对象 JSON格式
	 * @param index 索引名
	 * @param type 索引类型
	 * @param id 文档id
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回空对象 20180824
	 */
	public static IndexResponse addDocument(JSONObject object, String index, String type, String id) {

		TransportClient client = ElasticSearchClientFactory.getClientInstance();
		try {
			IndexResponse response = client.prepareIndex(index, type).setSource(object).setId(id).get();
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 添加文档集合
	 *
	 * @param objectList 文档对象对象 JSON格式
	 * @param index 索引名
	 * @param type 索引类型
	 * @return
	 * @version
	 */
	public static int addDocumentList(List<JSONObject> objectList, String index, String type) {

		TransportClient client = ElasticSearchClientFactory.getClientInstance();
		try {
			BulkRequestBuilder bulkRequest = client.prepareBulk();
			int count = 0;
			for(JSONObject jsonObject: objectList) {
				bulkRequest.add(client.prepareIndex(index, type).setSource(jsonObject).setId(jsonObject.getString("id")));
				count++;
			}
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if(bulkResponse.hasFailures()){
				return -1;
			}

			return count;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

	}

	/**
	 * 添加文档
	 *
	 * @param object 文档对象 键值对格式
	 * @param index 索引名
	 * @param type 索引类型
	 * @param id 文档id
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回空对象 20180824
	 */
	public static IndexResponse addDocument(Map<String, Object> object, String index, String type, String id) {

		TransportClient client = ElasticSearchClientFactory.getClientInstance();
		try {
			IndexResponse response = client.prepareIndex(index, type).setSource(object).setId(id).get();
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return null;

		}

	}

	/**
	 * 根据文档id修改文档  JSON格式
	 *
	 * @param object 修改文档对象 JSON格式
	 * @param index 索引名
	 * @param type 索引类型
	 * @param id 文档id
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回空对象 20180824
	 */
	public static UpdateResponse updateDocumentById(JSONObject object, String index, String type, String id) {
		try {
        	UpdateResponse response = ElasticSearchClientFactory.getClientInstance().prepareUpdate(index, type, id).setDoc(object, XContentType.JSON).get();
        	return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

	/**
	 * 根据文档id修改文档对象  键值对格式
	 *
	 * @param object 修改文档对象  键值对格式
	 * @param index 索引类型
	 * @param type 索引类型
	 * @param id 文档id
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回空对象 20180824
	 */
	public static UpdateResponse updateDocumentById(Map<String, Object> object, String index, String type, String id) {
		try {
        	UpdateResponse response = ElasticSearchClientFactory.getClientInstance().prepareUpdate(index, type, id).setDoc(object).get();
        	return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

	/**
	 * 根据文档id删除文档对象 键值对格式
	 *
	 * @param index 索引类型
	 * @param type 索引类型
	 * @param id 文档id
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回空对象 20180824
	 */
	public static DeleteResponse deleteDocumentById(String index, String type, String id) {
		try {
        	DeleteResponse response = ElasticSearchClientFactory.getClientInstance().prepareDelete(index, type, id).get();
        	return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

	/**
	 * 根据文档id获取文档对象 键值对格式
	 *
	 * @param index 索引类型
	 * @param type 索引类型
	 * @param id 文档id
	 * @return
	 * @version V1.0.0.2018082701 JiangWG 异常情况返回空对象 20180824
	 */
	public static GetResponse getDocumentById(String index, String type, String id) {
		try {
        	GetResponse response = ElasticSearchClientFactory.getClientInstance().prepareGet(index, type, id).get();
        	return response;
        } catch (Exception e) {
            //e.printStackTrace();
        	LogUtil.getDefaultLogger().warn("index ["+ index +"] id ["+ id +"] : get document fail");
            return null;
        }

    }

}
