package com.fql.vector.main;


import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.Base64;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class VectorSearchMain {


    private static class Vector{

        @SerializedName("image_path")
        private String imagePath;
        @SerializedName("image_vec")
        private List<Float> imageVec;

        public Vector() {
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public List<Float> getImageVec() {
            return imageVec;
        }

        public void setImageVec(List<Float> imageVec) {
            this.imageVec = imageVec;
        }
    }

    private static String generateSHA(byte[] message) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA"); // 此处的sha代表sha1
        byte[] cipherBytes = messageDigest.digest(message);
        return Hex.encodeHexString(cipherBytes);
    }

    public static void main(String args[]) throws IOException, NoSuchAlgorithmException, SolrServerException {

        String vectorFile = args[0];
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(vectorFile)));
        // index vectors into solr
        HttpSolrClient client = new  HttpSolrClient.Builder().withBaseSolrUrl("http://127.0.0.1:8983/solr/VECTOR")
                .allowCompression(true).withResponseParser(new BinaryResponseParser()).build();
        Gson gson = new Gson();
        String line = null;
        while((line=reader.readLine()) !=null)
        {

            Vector vector = gson.fromJson(line,Vector.class);
            SolrInputDocument document = new SolrInputDocument();
            String imagePath = vector.getImagePath();
            document.setField("id",generateSHA(imagePath.getBytes(Charset.forName("UTF-8"))));
            document.setField("image_url",vector.getImagePath());
            List<Float> vectors = vector.getImageVec();
            ByteBuffer byteBuffer = ByteBuffer.allocate(vectors.size()*4);
            byteBuffer.asFloatBuffer().put(ArrayUtils.toPrimitive(vectors.toArray(new Float[0])));
            byteBuffer.flip();
            String base64Vector = Base64.byteArrayToBase64(byteBuffer.array());
            document.setField("image_vector",base64Vector);
            client.add(document);
            System.out.println("index vector ==>"+ imagePath);
        }
        client.commit();
        reader.close();
        client.close();

    }
}
