package org.fql.codec.custom;

import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene70.Lucene70Codec;

/**
 * Created by fengqinglei on 2019/11/30.
 */
public class CustomCodec extends Lucene70Codec {
    public final static String DOCVALUES_CODEC_NAME = "Fql";
    public final static String POSTINGS_CODEC_NAME = "Fql";
    public final static String DEFAULT_DOC_VALUES_CODEC_NAME = "Lucene70";
    public final static String DEFAULT_POSTINGS_CODEC_NAME = "Lucene50";
    private final String fieldName ;
    public CustomCodec(String fieldName){
        this.fieldName = fieldName;
    }

    @Override
    public DocValuesFormat getDocValuesFormatForField(String field) {
        if(field.equals(this.fieldName)){
            return DocValuesFormat.forName(DOCVALUES_CODEC_NAME);
        }else {
            return DocValuesFormat.forName(DEFAULT_DOC_VALUES_CODEC_NAME);
        }
    }

    @Override
    public PostingsFormat getPostingsFormatForField(String field) {
        if(field.equals(this.fieldName)){
            return PostingsFormat.forName(POSTINGS_CODEC_NAME);
        }else {
            return PostingsFormat.forName(DEFAULT_POSTINGS_CODEC_NAME);
        }
    }
}
