package org.fql.codec.postings;

import org.apache.lucene.codecs.MultiLevelSkipListReader;
import org.apache.lucene.store.IndexInput;

import java.io.IOException;
import java.util.Arrays;

import static org.fql.codec.postings.FqlPostingsFormat.BLOCK_SIZE;

/**
 * Created by fengqinglei on 2019/12/10.
 * see {@link org.apache.lucene.codecs.lucene50.Lucene50SkipReader}
 */
public class FqlSkipReader extends MultiLevelSkipListReader {
    private long docPointer[];
    private long posPointer[];
    private long payPointer[];
    private int posBufferUpto[];
    private int payloadByteUpto[];

    private long lastPosPointer;
    private long lastPayPointer;
    private int lastPayloadByteUpto;
    private long lastDocPointer;
    private int lastPosBufferUpto;

    public FqlSkipReader(IndexInput skipStream, int maxSkipLevels, boolean hasPos, boolean hasOffsets, boolean hasPayloads) {
        super(skipStream, maxSkipLevels, BLOCK_SIZE, 8);
        docPointer = new long[maxSkipLevels];
        if (hasPos) {
            posPointer = new long[maxSkipLevels];
            posBufferUpto = new int[maxSkipLevels];
            if (hasPayloads) {
                payloadByteUpto = new int[maxSkipLevels];
            } else {
                payloadByteUpto = null;
            }
            if (hasOffsets || hasPayloads) {
                payPointer = new long[maxSkipLevels];
            } else {
                payPointer = null;
            }
        } else {
            posPointer = null;
        }
    }

    /**
     * Trim original docFreq to tell skipReader read proper number of skip points.
     *
     * Since our definition in Lucene50Skip* is a little different from MultiLevelSkip*
     * This trimmed docFreq will prevent skipReader from:
     * 1. silly reading a non-existed skip point after the last block boundary
     * 2. moving into the vInt block
     *
     */
    protected int trim(int df) {
        return df % BLOCK_SIZE == 0? df - 1: df;
    }

    public void init(long skipPointer, long docBasePointer, long posBasePointer, long payBasePointer, int df) throws IOException {
        super.init(skipPointer, trim(df));
        lastDocPointer = docBasePointer;
        lastPosPointer = posBasePointer;
        lastPayPointer = payBasePointer;

        Arrays.fill(docPointer, docBasePointer);
        if (posPointer != null) {
            Arrays.fill(posPointer, posBasePointer);
            if (payPointer != null) {
                Arrays.fill(payPointer, payBasePointer);
            }
        } else {
            assert posBasePointer == 0;
        }
    }

    /** Returns the doc pointer of the doc to which the last call of
     * {@link MultiLevelSkipListReader#skipTo(int)} has skipped.  */
    public long getDocPointer() {
        return lastDocPointer;
    }

    public long getPosPointer() {
        return lastPosPointer;
    }

    public int getPosBufferUpto() {
        return lastPosBufferUpto;
    }

    public long getPayPointer() {
        return lastPayPointer;
    }

    public int getPayloadByteUpto() {
        return lastPayloadByteUpto;
    }

    public int getNextSkipDoc() {
        return skipDoc[0];
    }

    @Override
    protected void seekChild(int level) throws IOException {
        super.seekChild(level);
        docPointer[level] = lastDocPointer;
        if (posPointer != null) {
            posPointer[level] = lastPosPointer;
            posBufferUpto[level] = lastPosBufferUpto;
            if (payloadByteUpto != null) {
                payloadByteUpto[level] = lastPayloadByteUpto;
            }
            if (payPointer != null) {
                payPointer[level] = lastPayPointer;
            }
        }
    }

    @Override
    protected void setLastSkipData(int level) {
        super.setLastSkipData(level);
        lastDocPointer = docPointer[level];

        if (posPointer != null) {
            lastPosPointer = posPointer[level];
            lastPosBufferUpto = posBufferUpto[level];
            if (payPointer != null) {
                lastPayPointer = payPointer[level];
            }
            if (payloadByteUpto != null) {
                lastPayloadByteUpto = payloadByteUpto[level];
            }
        }
    }

    @Override
    protected int readSkipData(int level, IndexInput skipStream) throws IOException {
        int delta = skipStream.readVInt();
        docPointer[level] += skipStream.readVLong();

        if (posPointer != null) {
            posPointer[level] += skipStream.readVLong();
            posBufferUpto[level] = skipStream.readVInt();

            if (payloadByteUpto != null) {
                payloadByteUpto[level] = skipStream.readVInt();
            }

            if (payPointer != null) {
                payPointer[level] += skipStream.readVLong();
            }
        }
        return delta;
    }
}
