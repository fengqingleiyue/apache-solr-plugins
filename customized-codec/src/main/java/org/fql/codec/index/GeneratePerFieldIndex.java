package org.fql.codec.index;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.BytesRef;
import org.fql.codec.custom.CustomCodec;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fengqinglei on 2019/11/30.
 */
public class GeneratePerFieldIndex {
    /**
     * In order to demo the change of data in a easy way , here we just use a map to change some data of
     * the target field , so we can easily understand the code.
     */
    private final static Map<String,String> MOCK_MAP = new HashMap<String,String>(){{
        put("utility","fql_demo");
    }};

    public static void main(String args[]) throws IOException
    {
        if(args.length!=4){
            System.err.println("Usage:GeneratePerFieldIndex <originalIndexLocation> <fieldName> <isMultiField> <newIndexLocation>");
            return;
        }
        String originalIndexLocation = args[0];
        String fieldName = args[1];
        boolean isMultiField = Boolean.valueOf(args[2]);
        String newIndexLocation = args[3];

        // Load customized codec
        CustomCodec customCodec =new CustomCodec(fieldName);

        // check if index is optimized
        Directory sourceDirectory = FSDirectory.open(Paths.get(originalIndexLocation),new SingleInstanceLockFactory());
        IndexReader sourceReader = DirectoryReader.open(sourceDirectory);
        if(sourceReader.leaves().size()!=1){
            sourceReader.close();
            sourceDirectory.close();
            System.err.println("The Index is not optimized at ["+originalIndexLocation+"] , start optimize");
            LocalLuceneOptimize.optimize(originalIndexLocation,customCodec);
            sourceDirectory = FSDirectory.open(Paths.get(originalIndexLocation),new SingleInstanceLockFactory());
            sourceReader = DirectoryReader.open(sourceDirectory);
            System.err.println("Index Merged");
        }
        if(sourceReader.leaves().size()!=1){
            System.err.println("The Index is not optimized at ["+originalIndexLocation+"] , program will exit");
            System.exit(-1);
        }
        System.out.println("==>Start to generated indexing");
        Directory desDirectory = FSDirectory.open(Paths.get(newIndexLocation));
        IndexWriterConfig writerConfig = new IndexWriterConfig();
        writerConfig.setUseCompoundFile(false);
        writerConfig.setCodec(customCodec);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        /*we need use LogMergePolicy since we need keep the doc Order */
        writerConfig.setMergePolicy(new LogByteSizeMergePolicy());
        IndexWriter descWriter = new IndexWriter(desDirectory,writerConfig);
        if(isMultiField) {
            SortedSetDocValues multiDocValues = sourceReader.leaves().get(0).reader().getSortedSetDocValues(fieldName);
            for(int i=0;i<sourceReader.maxDoc();i++){
                Document doc = new Document();
                doc.add(new StringField("id",String.valueOf(i), Field.Store.YES));
                /*we can assignee any value for type field of the original index, but we must generate the index in single thread */
                if(multiDocValues.advanceExact(i)){
                    long ord = multiDocValues.nextOrd();
                    while (ord!=SortedSetDocValues.NO_MORE_ORDS){
                        String value = multiDocValues.lookupOrd(ord).utf8ToString();
                        if(MOCK_MAP.containsKey(value)){
                            value = MOCK_MAP.get(value);
                        }
                        doc.add(new SortedSetDocValuesField(fieldName,new BytesRef(value)));
                        ord = multiDocValues.nextOrd();
                    }
                }
                descWriter.addDocument(doc);
            }
        } else {
            SortedDocValues singleDocValues = sourceReader.leaves().get(0).reader().getSortedDocValues(fieldName);
            for(int i=0;i<sourceReader.maxDoc();i++){
                /*we can assignee any value for type field of the original index, but we must generate the index in single thread */
                Document doc = new Document();
                doc.add(new StringField("id",String.valueOf(i), Field.Store.YES));
                if(singleDocValues.advanceExact(i)){
                    int ord = singleDocValues.ordValue();
                    if(ord!=SortedDocValues.NO_MORE_DOCS){
                        String value = singleDocValues.lookupOrd(ord).utf8ToString();
                        if(MOCK_MAP.containsKey(value)){
                            value = MOCK_MAP.get(value);
                        }
                        doc.add(new SortedDocValuesField(fieldName,new BytesRef(value)));
                    }
                }
                descWriter.addDocument(doc);
            }
        }
        descWriter.commit();
        descWriter.forceMerge(1);
        descWriter.close();
        desDirectory.close();
        sourceReader.close();
        sourceDirectory.close();
        System.out.println("==>Start to verify the generated index");
        verifyNewIndex(newIndexLocation);
        System.out.println("==>Start to Hack Lucene Index");
    }

    public static void verifyNewIndex (String newIndexLocation) throws IOException{
        Directory sourceDirectory = FSDirectory.open(Paths.get(newIndexLocation),new SingleInstanceLockFactory());
        IndexReader sourceReader = DirectoryReader.open(sourceDirectory);
        if(sourceReader.leaves().size()!=1){
            System.err.println("The Index is not optimized at ["+newIndexLocation+"] , program will exit");
            System.exit(-1);
        }
        for(int i=0;i<sourceReader.maxDoc();i++){
            if(i!=Integer.valueOf(sourceReader.document(i).get("id"))){
                System.err.println("Error in verify index ["+newIndexLocation+"],This should not happen , program will exit");
                System.exit(-1);
            }
        }
        sourceReader.close();
        sourceDirectory.close();
    }
}
