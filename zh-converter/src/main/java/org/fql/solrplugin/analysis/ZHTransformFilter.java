package org.fql.solrplugin.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

/**
 * Created by fengqinglei on 2019/8/12.
 */
public class ZHTransformFilter extends TokenFilter {
    private  ZHTransform transform;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public ZHTransformFilter(TokenStream input, ZHTransform transform) {
        super(input);
        this.transform = transform;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            char [] data = new char[termAtt.length()];
            for(int i=0;i<termAtt.length();i++){
                data[i]=transform.transform(termAtt.charAt(i));
            }
            termAtt.setEmpty().append(new String(data));
            return true;
        } else {
            return false;
        }
    }
}
