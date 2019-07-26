package org.fql.solrplugin.payload;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

/**
 * Created by fengqinglei on 2/7/2019.
 */
public class DynamicPayloadsField extends TextField {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Override
    public IndexableField createField(SchemaField field, Object value) {
        if (!field.indexed() && !field.stored()) {
            if (log.isTraceEnabled())
                log.trace("Ignoring unindexed/unstored field: " + field);
            return null;
        }

        String val;
        try {
            val = toInternal(value.toString());
        } catch (RuntimeException e) {
            throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, "Error while creating field '" + field + "' from value '" + value + "'", e);
        }
        if (val==null) return null;
        String target = null;
        if (val.startsWith("[|") && val.contains("|]")) {
            target = val.substring(2,val.indexOf("|]"));
            if (DynamicPayloadAttribute.FIELD_MAPPINGS.containsKey(target)) {
                String parsedData = val.substring(val.indexOf("|]")+2);
                TokenizerChain tokenizerChain = (TokenizerChain)field.getType().getIndexAnalyzer();
                Tokenizer tk = tokenizerChain.getTokenizerFactory().create(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY);
                TokenStream ts = tk;
                for (TokenFilterFactory filter : tokenizerChain.getTokenFilterFactories()) {
                    ts = filter.create(ts);
                }
                Analyzer.TokenStreamComponents components = new Analyzer.TokenStreamComponents(tk, ts);
                Reader stringReader = tokenizerChain.initReader(field.getName(),new StringReader(parsedData));
                tk.setReader(stringReader);
                TokenStream stream = components.getTokenStream();
                stream.getAttribute(DynamicPayloadAttribute.class).setPayloadType(target);
                return new Field(field.getName(), stream, field);
            } else {
                return super.createField(field.getName(), val, field);
            }
        } else {
            return super.createField(field.getName(), val, field);
        }
    }
}