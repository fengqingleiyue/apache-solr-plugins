package org.fql.solrplugin.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by fengqinglei on 2019/8/10.
 */
public class ZHConverterMain {
    public static void main(String args[])throws IOException
    {
        String filename = "hant_hans.properties";
        Properties properties = new Properties();
        InputStream inputStream = ZHConverterMain.class.getClassLoader().getResourceAsStream(filename);
        properties.load(inputStream);

        for(Object key : properties.keySet()){
            System.out.println(key.toString().toCharArray().length);
        }
    }
}
