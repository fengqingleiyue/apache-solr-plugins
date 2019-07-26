package org.fql.solrplugin.payload;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

/**
 * Created by fengqinglei on 8/7/2019.
 */
public class DynamicPayloadTokenFilter extends TokenFilter {
    private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
    private final DynamicPayloadAttribute dypayAtt = addAttribute(DynamicPayloadAttribute.class);

    protected DynamicPayloadTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            byte payload = dypayAtt.getPayLoad();
            if(payload==Byte.MAX_VALUE){
                payAtt.setPayload(null);
            }else {
                byte array [] = {payload};
                payAtt.setPayload(new BytesRef(array));
            }
            return true;
        } else return false;
    }
}