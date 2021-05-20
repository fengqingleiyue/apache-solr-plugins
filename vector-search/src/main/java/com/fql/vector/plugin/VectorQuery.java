package com.fql.vector.plugin;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.FilterScorer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class VectorQuery extends Query {

    /**
     * main query , find the docids that used to calculate the vector score
     */
    private Query query;

    /**
     * the vector that used to do cosine operation
     */
    private float [] targetVector;

    /**
     * specify the field name which used to get the vectors
     */
    private String vectorField;

    /**
     * @param query
     * @param targetVector
     * @param vectorField
     */
    public VectorQuery(Query query,float [] targetVector,String vectorField) {
        this.query = query;
        this.targetVector = targetVector;
        this.vectorField = vectorField;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
        Weight inner = query.createWeight(searcher,needsScores,1);
        return new VectorScoreWeight(this,inner,new float[this.targetVector.length]);

    }

    /** Expert: called to re-write queries into primitive queries. For example,
     * a PrefixQuery will be rewritten into a BooleanQuery that consists
     * of TermQuerys.
     */
    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        Query  rewritten = query.rewrite(reader);
        if (rewritten == query) return  this;
        return new VectorQuery(rewritten,this.targetVector,this.vectorField);
    }




    @Override
    public String toString(String field) {
        return "VectorQuery(" + this.query.toString(field) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorQuery that = (VectorQuery) o;
        return Objects.equals(query, that.query) &&
                Arrays.equals(targetVector, that.targetVector) &&
                Objects.equals(vectorField, that.vectorField);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(query, vectorField);
        result = 31 * result + Arrays.hashCode(targetVector);
        return result;
    }


    /**
     * customized VectorScoreWeight
     */
    private static class VectorScoreWeight extends Weight
    {

        private Weight inner;
        private VectorQuery query;
        private float [] vector;
        protected VectorScoreWeight(VectorQuery query,Weight inner,float [] vector) {
            super(query);
            this.query = query;
            this.inner = inner;
            this.vector = vector;
        }

        @Override
        public void extractTerms(Set<Term> terms) {
            this.inner.extractTerms(terms);
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            Scorer scorer = scorer(context);
            if (scorer.iterator().advance(doc) !=doc)
            {
                return Explanation.noMatch("No match");
            } else {
                return Explanation.match(scorer.score(), "dot score ");
            }
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            Scorer in = inner.scorer(context);
            if (in == null)
            {
                return null;
            }

            BinaryDocValues binaryDocValues = context.reader().getBinaryDocValues(this.query.vectorField);
            return new FilterScorer(in){
                @Override
                public float score() throws IOException {
                    int docId = docID();
                    if (binaryDocValues.advanceExact(docId))
                    {
                        // get vector data
                        BytesRef ref = binaryDocValues.binaryValue();
                        FloatBuffer floatBuffer =  ByteBuffer.wrap(ref.bytes,ref.offset,ref.length).asFloatBuffer();
                        floatBuffer.get(vector);
                        return computeScore(vector);

                    } else {
                        return 0;
                    }
                }
            };

        }

        /**
         * calculate l2 distance
         * @param array
         * @return
         */
        private float computeScore(float [] array)
        {
            float l2 = 0.0f;
            for (int i = 0; i < this.query.targetVector.length; i++) {
                l2 += Math.pow(array[i] - this.query.targetVector[i], 2.0D);
            }
            return 1.0F / (1.0F + l2);
        }


        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            return this.inner.isCacheable(ctx);
        }
    }
}

