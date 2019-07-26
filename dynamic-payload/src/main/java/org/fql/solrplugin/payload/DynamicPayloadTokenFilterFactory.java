package org.fql.solrplugin.payload;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * Created by fengqinglei on 8/7/2019.
 */
public class DynamicPayloadTokenFilterFactory extends TokenFilterFactory implements MultiTermAwareComponent {

    public DynamicPayloadTokenFilterFactory(Map<String,String> args){
        super(args);
        require(args, "encoder");
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
        return this;
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new DynamicPayloadTokenFilter(tokenStream);
    }
}