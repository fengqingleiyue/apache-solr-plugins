package org.fql.solrplugin.analysis;

import com.carrotsearch.hppc.CharCharHashMap;
import com.carrotsearch.hppc.CharCharMap;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by fengqinglei on 2019/8/12.
 */
public class ZHTransform {
    public static final CharCharMap HANT_HANT_DIC;
    static {
        Properties properties = new Properties();
        try {
            properties.load(ZHTransform.class.getClassLoader().getResourceAsStream("hant_hans.properties"));
        } catch (IOException e) {
            System.err.println("miss dic file hant_hans.properties");
            System.exit(-1);
        }
        HANT_HANT_DIC = new CharCharHashMap(properties.keySet().size());
        for(Object key : properties.keySet()){
            HANT_HANT_DIC.put(key.toString().toCharArray()[0],properties.get(key).toString().toCharArray()[0]);
        }

    }

    public char transform(char input){
        if(HANT_HANT_DIC.containsKey(input)){
            return HANT_HANT_DIC.get(input);
        }
        return input;
    }
}
