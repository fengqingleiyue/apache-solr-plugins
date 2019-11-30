package org.fql.codec.index;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by fengqinglei on 2019/11/26.
 */
public class IndexPatentData {
    public static void main(String args[]) throws IOException {
        if (args.length!=3){
            System.out.println("Usage IndexPatentData <filename> <indexThread> <solrAaddress>");
            return;
        }
        String fileName = args[0];
        int threadCount = Integer.valueOf(args[1]);
        String solrAddress = args[2];
        int produced = 0;
        DataProducer producer = new DataProducer(new File(fileName));
        DataConsumer consumers[] = new DataConsumer[threadCount];
        for(int i=0;i<threadCount;i++){
            consumers[i] = new DataConsumer(1,solrAddress,50);
            new Thread(consumers[i]).start();
        }
        long start = System.currentTimeMillis();
        int index = 0;
        while (producer.hasNext()) {
            SolrInputDocument document = producer.next();
            if(document==null){
                continue;
            }
            consumers[index++].add(document);
            produced ++;
            if(index==consumers.length){
                index =0 ;
            }
            if( (System.currentTimeMillis()-start) >=30000 ){
                System.out.println("processed ["+produced+"] docs");
                start = System.currentTimeMillis();
            }

        }
        producer.close();
        System.out.println("[Producer] send ["+produced+"] docs");
        for(DataConsumer consumer : consumers){
            consumer.stop();
        }
    }


    private static class DataConsumer implements Runnable {
        private ArrayBlockingQueue<SolrInputDocument> queue;
        private volatile  boolean end;
        private HttpSolrClient client;
        private ArrayList<SolrInputDocument> buffer;
        private int bufferSize ;
        private int recieved;
        private int processed;
        public DataConsumer(int queueCapacity,String solrAddress,int bufferSize){
            this.queue = new ArrayBlockingQueue<SolrInputDocument>(queueCapacity,true);
            end = false;
            buffer = new ArrayList<SolrInputDocument>(bufferSize);
            this.bufferSize = bufferSize;
            this.recieved = 0;
            this.processed = 0;
            client = new HttpSolrClient.Builder().withBaseSolrUrl(solrAddress).build();
            client.setRequestWriter(new BinaryRequestWriter());
        }

        public boolean add(SolrInputDocument document) {
            try {
                this.queue.put(document);
                this.recieved ++;
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
        @Override
        public void run() {
            while (!end) {
               if (this.queue.size()>0){
                   try {
                       SolrInputDocument document = this.queue.take();
                       this.buffer.add(document);
                   } catch (InterruptedException e) {
                       continue;
                   }
               }
               if(this.buffer.size()>=this.bufferSize){
                   try {
                       this.client.add(this.buffer);
                       processed+=this.buffer.size();
                   } catch (SolrServerException e) {
                       e.printStackTrace();
                   } catch (IOException e) {
                       e.printStackTrace();
                   } finally {
                       this.buffer.clear();
                   }
               }
            }
            // drain all the remain doc into buffer
            try {
                this.queue.drainTo(this.buffer);
                if(this.buffer.size()>0){
                    this.client.add(this.buffer);
                    processed+=this.buffer.size();
                }
                client.commit();
            } catch (SolrServerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                buffer.clear();
                try {
                    client.close();
                } catch (IOException e) {
                }
            }
            System.out.println("[Consumer] "+ Thread.currentThread().getName()+" got ["+this.recieved+"] doc and processed ["+this.processed+"] doc");
        }

        public void stop()
        {
            this.end = true;
        }
    }


    private static class DataProducer implements Iterator<SolrInputDocument> {
        private BufferedReader dataReader;
        private String line;
        public DataProducer(File inputFile) throws IOException {
            dataReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), Charset.forName("UTF-8")));
            dataReader.readLine();
        }

        @Override
        public boolean hasNext() {
            try {
                line = dataReader.readLine();
                return !StringUtils.isEmpty(line);
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
            return false;
        }

        @Override
        public SolrInputDocument next() {
            // split the line by \t
            String [] dataArray = line.split("\\t");
            if(dataArray.length!=11) {
                System.out.println("broken data =>"+line);
                return null;
            }
            SolrInputDocument document = new SolrInputDocument();
            document.setField("id",dataArray[0]);
            document.setField("type",dataArray[1]);
            document.setField("country",dataArray[3]);
            document.setField("date",Integer.valueOf(dataArray[4].replaceAll("-","")));
            document.setField("abstract",dataArray[5]);
            document.setField("title",dataArray[6]);
            document.setField("kind",dataArray[7]);
            return document;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() method is not supported");
        }
        public void close()
        {
            if (dataReader!=null){
                try {
                    dataReader.close();
                } catch (IOException e) {
                    // Ignore here
//                    e.printStackTrace();
                }
            }
        }
    }
}
