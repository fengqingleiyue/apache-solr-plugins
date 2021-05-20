package com.fql.vector.plugin;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.apache.solr.search.QParser;

public class VectorQueryParserPlugin extends ExtendedDismaxQParserPlugin {

    public static final String NAME = "vector_plugin";

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new VectorQueryParser(qstr,localParams,params,req);
    }

}
