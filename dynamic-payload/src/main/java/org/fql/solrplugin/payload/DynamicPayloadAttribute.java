package org.fql.solrplugin.payload;

import org.apache.lucene.util.Attribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fengqinglei on 8/7/2019.
 */
public interface DynamicPayloadAttribute extends Attribute {
    Map<String,Integer> FIELD_MAPPINGS = new HashMap<String,Integer>(){
        {
            put("A",0);
            put("B",1);
            put("C",2);
            put("D",3);
        }
    };
    String DEFAULT_PAYLOAD_TYPE = "UNKNOW_PAYLOAD_TYPE";
    String payloadType();
    void setPayloadType(String type);
    byte getPayLoad();
}
