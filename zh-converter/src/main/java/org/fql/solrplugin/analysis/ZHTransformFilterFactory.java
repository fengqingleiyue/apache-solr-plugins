package org.fql.solrplugin.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * Created by fengqinglei on 2019/8/12.
 */
public class ZHTransformFilterFactory extends TokenFilterFactory implements MultiTermAwareComponent {

    private ZHTransform transform;
    public ZHTransformFilterFactory(Map<String, String> args) {
        super(args);
        transform = new ZHTransform();
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
        return this;
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new ZHTransformFilter(input,transform);
    }
}
