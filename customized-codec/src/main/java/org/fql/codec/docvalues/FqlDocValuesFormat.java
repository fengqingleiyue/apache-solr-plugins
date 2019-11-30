package org.fql.codec.docvalues;

import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

import java.io.IOException;

/**
 * Created by fengqinglei on 2019/11/27.
 * see {@link org.apache.lucene.codecs.lucene70.Lucene70DocValuesFormat}
 */
public final class FqlDocValuesFormat extends DocValuesFormat {

    public FqlDocValuesFormat(){
        super("Fql");
    }
    @Override
    public DocValuesConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
        return new FqlDocValuesConsumer(state, DATA_CODEC, DATA_EXTENSION, META_CODEC, META_EXTENSION);
    }

    @Override
    public DocValuesProducer fieldsProducer(SegmentReadState state) throws IOException {
        return new FqlDocValuesProducer(state, DATA_CODEC, DATA_EXTENSION, META_CODEC, META_EXTENSION);
    }

    static final String DATA_CODEC = "FqlDocValuesData";
    static final String DATA_EXTENSION = "dvd";
    static final String META_CODEC = "FqlDocValuesMetadata";
    static final String META_EXTENSION = "dvm";
    static final int VERSION_START = 0;
    static final int VERSION_CURRENT = VERSION_START;

    // indicates docvalues type
    static final byte NUMERIC = 0;
    static final byte BINARY = 1;
    static final byte SORTED = 2;
    static final byte SORTED_SET = 3;
    static final byte SORTED_NUMERIC = 4;

    static final int DIRECT_MONOTONIC_BLOCK_SHIFT = 16;

    static final int NUMERIC_BLOCK_SHIFT = 14;
    static final int NUMERIC_BLOCK_SIZE = 1 << NUMERIC_BLOCK_SHIFT;

    static final int TERMS_DICT_BLOCK_SHIFT = 4;
    static final int TERMS_DICT_BLOCK_SIZE = 1 << TERMS_DICT_BLOCK_SHIFT;
    static final int TERMS_DICT_BLOCK_MASK = TERMS_DICT_BLOCK_SIZE - 1;

    static final int TERMS_DICT_REVERSE_INDEX_SHIFT = 10;
    static final int TERMS_DICT_REVERSE_INDEX_SIZE = 1 << TERMS_DICT_REVERSE_INDEX_SHIFT;
    static final int TERMS_DICT_REVERSE_INDEX_MASK = TERMS_DICT_REVERSE_INDEX_SIZE - 1;
}
