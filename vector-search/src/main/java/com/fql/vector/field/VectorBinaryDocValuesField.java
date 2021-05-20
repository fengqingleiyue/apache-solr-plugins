package com.fql.vector.field;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.Base64;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.uninverting.UninvertingReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;

/**
 * for default solr.BinaryField do not support docvalues
 * customized VectorBinaryDocValuesField is used for store vectors as binary info in solr index
 */
public class VectorBinaryDocValuesField extends FieldType {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void checkSchemaField(SchemaField field) {
        super.checkSchemaField(field);
        if (field.isLarge()) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Field type " + this + " is 'large'; not supported (yet)");
        }
    }

    private String toBase64String(ByteBuffer buf) {
        return Base64.byteArrayToBase64(buf.array(), buf.position(), buf.limit()-buf.position());
    }

    @Override
    public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException {
        writer.writeStr(name, toBase64String(toObject(f)), false);
    }

    @Override
    public SortField getSortField(SchemaField field, boolean top) {
        throw new RuntimeException("Cannot sort on a Binary field");
    }

    @Override
    public UninvertingReader.Type getUninversionType(SchemaField sf) {
        // TODO: maybe just return null?
        if (sf.multiValued()) {
            return UninvertingReader.Type.SORTED_SET_BINARY;
        } else {
            return UninvertingReader.Type.BINARY;
        }
    }

    @Override
    public String toExternal(IndexableField f) {
        return toBase64String(toObject(f));
    }

    @Override
    public ByteBuffer toObject(IndexableField f) {
        BytesRef bytes = f.binaryValue();
        return  ByteBuffer.wrap(bytes.bytes, bytes.offset, bytes.length);
    }

    @Override
    public IndexableField createField(SchemaField field, Object val) {
        if (val == null) return null;
        byte[] buf = null;
        int offset = 0, len = 0;
        if (val instanceof byte[]) {
            buf = (byte[]) val;
            len = buf.length;
        } else if (val instanceof ByteBuffer && ((ByteBuffer)val).hasArray()) {
            ByteBuffer byteBuf = (ByteBuffer) val;
            buf = byteBuf.array();
            offset = byteBuf.position();
            len = byteBuf.limit() - byteBuf.position();
        } else {
            String strVal = val.toString();
            //the string has to be a base64 encoded string
            buf = Base64.base64ToByteArray(strVal);
            offset = 0;
            len = buf.length;
        }

        return new BinaryDocValuesField(field.getName(), new BytesRef(buf,offset,len));
    }

    @Override
    protected void checkSupportsDocValues() { // primitive types support DocValues
    }
}
