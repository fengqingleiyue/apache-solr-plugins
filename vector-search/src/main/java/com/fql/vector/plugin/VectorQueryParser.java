package com.fql.vector.plugin;

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.ExtendedDismaxQParser;
import org.apache.solr.search.SyntaxError;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Base64;

public class VectorQueryParser extends ExtendedDismaxQParser {

    /**
     * the dimension size of the image vector
     */
    private static final int VECTOR_DIMENSION_SIZE = 2048;

    public VectorQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        super(qstr, localParams, params, req);
    }

    @Override
    public Query parse() throws SyntaxError {
        Query mainQuery = super.parse();
        String vector = this.params.get("input_vector_str");
        String vectorField = this.params.get("vector_field");
        return new VectorQuery(new ConstantScoreQuery(mainQuery),getVectorByBase64(vector),vectorField);
    }


    private float[] getVectorByBase64(String vectorStr)
    {
        FloatBuffer floatBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(vectorStr.getBytes())).asFloatBuffer();
        float[] vector = new float[VECTOR_DIMENSION_SIZE];
        floatBuffer.get(vector);
        return vector;
    }
}
