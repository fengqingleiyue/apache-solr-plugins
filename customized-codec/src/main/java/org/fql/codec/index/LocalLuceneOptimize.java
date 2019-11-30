package org.fql.codec.index;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by fengqinglei on 2019/11/30.
 */
public class LocalLuceneOptimize {

    public static void optimize(String indexLocaltion,Codec codec) {
        try {
            Directory directory = FSDirectory.open(Paths.get(indexLocaltion),new SingleInstanceLockFactory());
            IndexWriterConfig config = new IndexWriterConfig();
            config.setUseCompoundFile(false);
            config.setCodec(codec);
            IndexWriter writer = new IndexWriter(directory,config);
            writer.commit();
            writer.forceMerge(1);
            writer.close();
            directory.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
