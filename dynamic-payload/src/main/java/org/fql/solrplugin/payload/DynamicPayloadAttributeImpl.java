package org.fql.solrplugin.payload;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * Created by fengqinglei on 8/7/2019.
 */
public class DynamicPayloadAttributeImpl extends AttributeImpl implements DynamicPayloadAttribute ,Cloneable {
    private String payloadType;


    public DynamicPayloadAttributeImpl() {
        this(DEFAULT_PAYLOAD_TYPE);
    }

    public DynamicPayloadAttributeImpl(String payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public String payloadType() {
        return payloadType;
    }

    @Override
    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public void clear() {
    }

    @Override
    public void reflectWith(AttributeReflector reflector) {
        reflector.reflect(DynamicPayloadAttribute.class,"payloadType",payloadType);
    }

    @Override
    public void copyTo(AttributeImpl attribute) {
        DynamicPayloadAttribute t = (DynamicPayloadAttribute)attribute;
        t.setPayloadType(payloadType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicPayloadAttributeImpl that = (DynamicPayloadAttributeImpl) o;

        return payloadType != null ? payloadType.equals(that.payloadType) : that.payloadType == null;
    }

    @Override
    public int hashCode() {
        return payloadType != null ? payloadType.hashCode() : 0;
    }

    @Override
    public byte getPayLoad() {
        if(FIELD_MAPPINGS.containsKey(payloadType)){
            int data = FIELD_MAPPINGS.get(payloadType);
            if(data<127) {
                return (byte)data;
            } else {
                return Byte.MAX_VALUE;
            }
        }else {
            return Byte.MAX_VALUE;
        }
    }
}