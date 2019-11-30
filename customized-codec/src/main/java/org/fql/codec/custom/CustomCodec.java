package org.fql.codec.custom;

import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.lucene70.Lucene70Codec;

/**
 * Created by fengqinglei on 2019/11/30.
 */
public class CustomCodec extends Lucene70Codec {
    private final static String DOCVALUES_CODEC_NAME = "Fql";
    private final static String DEFAULT_CODEC_NAME = "Lucene70";
    private final String fieldName ;
    public CustomCodec(String fieldName){
        this.fieldName = fieldName;
    }

    @Override
    public DocValuesFormat getDocValuesFormatForField(String field) {
        if(field.equals(this.fieldName)){
            return DocValuesFormat.forName(DOCVALUES_CODEC_NAME);
        }else {
            return DocValuesFormat.forName(DEFAULT_CODEC_NAME);
        }
    }
}
