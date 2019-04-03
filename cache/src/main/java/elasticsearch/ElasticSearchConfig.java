package elasticsearch;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

@PropertiesFilePath("/elasticsearch.properties")
public class ElasticSearchConfig extends ApplicationPropertiesBase {

    public static ElasticSearchConfig ES_CONFIG = new ElasticSearchConfig();

    @PropertiesName("elasticsearch.host")
    public static String pro_host ;
    @PropertiesName("elasticsearch.port")
    public static String pro_port ;
    @PropertiesName("elasticsearch.cluster.name")
    public static String pro_clusterName ;

    @Override
    protected void initialization() {
        super.initialization();
    }

}
