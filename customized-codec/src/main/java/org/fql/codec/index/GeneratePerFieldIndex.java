package org.fql.codec.index;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;
import org.fql.codec.custom.CustomCodec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
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
        boolean isFirstTime = true;
        int number = sourceReader.leaves().get(0).reader().getFieldInfos().fieldInfo(fieldName).number;
        if(isMultiField) {
            SortedSetDocValues multiDocValues = sourceReader.leaves().get(0).reader().getSortedSetDocValues(fieldName);
            for(int i=0;i<sourceReader.maxDoc();i++){
                Document doc = new Document();
                doc.add(new StringField("id",String.valueOf(i), Field.Store.YES));
                /*we can assignee any value for type field of the original index, but we must generate the index in single thread */
                if(multiDocValues.advanceExact(i)){
                    long ord = multiDocValues.nextOrd();
                    while (ord!=SortedSetDocValues.NO_MORE_ORDS){
                        /*process the field number*/
                        if(isFirstTime){
                            for(int fieldNumber=1;fieldNumber<number;fieldNumber++){
                                doc.add( new StringField("xxx_"+fieldNumber,"xxx_"+fieldNumber , Field.Store.NO));
                            }
                            isFirstTime =false;
                        }
                        String value = multiDocValues.lookupOrd(ord).utf8ToString();
                        if(MOCK_MAP.containsKey(value)){
                            value = MOCK_MAP.get(value);
                        }
                        doc.add(new SortedSetDocValuesField(fieldName,new BytesRef(value)));
                        doc.add(new StringField(fieldName,value, Field.Store.NO));
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
                        /*process the field number*/
                        if(isFirstTime){
                            for(int fieldNumber=1;fieldNumber<number;fieldNumber++){
                                doc.add( new StringField("xxx_"+fieldNumber,"xxx_"+fieldNumber , Field.Store.NO));
                            }
                            isFirstTime =false;
                        }
                        String value = singleDocValues.lookupOrd(ord).utf8ToString();
                        if(MOCK_MAP.containsKey(value)){
                            value = MOCK_MAP.get(value);
                        }
                        doc.add(new SortedDocValuesField(fieldName,new BytesRef(value)));
                        doc.add(new StringField(fieldName,value, Field.Store.NO));
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
        hackLuceneIndex(originalIndexLocation,newIndexLocation);
    }

    public static void hackLuceneIndex(String originalIndex ,String newIndex) throws IOException{
        // check whether index is optimized
        Directory sourceDirectory = FSDirectory.open(Paths.get(originalIndex));
        if(DirectoryReader.open(sourceDirectory).leaves().size()!=1) {
            System.err.println("Index is not optimized at ["+originalIndex+"] ");
            System.exit(-1);
        }
        Directory desDirectory = FSDirectory.open(Paths.get(newIndex));
        if(DirectoryReader.open(desDirectory).leaves().size()!=1){
            System.err.println("Index is not optimized at ["+newIndex+"] ");
            System.exit(-1);
        }
        SegmentInfos sourceInfos = SegmentInfos.readLatestCommit(sourceDirectory);
        SegmentInfos desInfos = SegmentInfos.readLatestCommit(desDirectory);
        /* hack the *.dvd lucene index file*/
        String sourceDocValuesDataFile = filterFileBySuffix(sourceInfos.files(false),".dvd");
        String desDocValuesDataFile = filterFileBySuffix(desInfos.files(false),".dvd");
        hackIndexData(sourceDirectory,sourceDocValuesDataFile,desDirectory,desDocValuesDataFile,false);

        String sourceDocValuesMetaFile = filterFileBySuffix(sourceInfos.files(false),".dvm");
        String desDocValuesMetaFile = filterFileBySuffix(desInfos.files(false),".dvm");
        hackIndexData(sourceDirectory,sourceDocValuesMetaFile,desDirectory,desDocValuesMetaFile,false);

        /*hack the .pos .tim .tip .doc index file*/
        String sourceDocDataFile = filterFileBySuffix(sourceInfos.files(false),".doc");
        String desDocDataFile = filterFileBySuffix(desInfos.files(false),".doc");
        hackIndexData(sourceDirectory,sourceDocDataFile,desDirectory,desDocDataFile,false);

        String sourceTipDataFile = filterFileBySuffix(sourceInfos.files(false),".tip");
        String desTipDataFile = filterFileBySuffix(desInfos.files(false),".tip");
        hackIndexData(sourceDirectory,sourceTipDataFile,desDirectory,desTipDataFile,false);

        /*since the field type of field "type" is string , so there is no .pos for the new index*/
//        String sourcePosDataFile = filterFileBySuffix(sourceInfos.files(false),".pos");
//        String desPosDataFile = filterFileBySuffix(desInfos.files(false),".pos");
//        hackIndexData(sourceDirectory,sourcePosDataFile,desDirectory,desPosDataFile,false);


        String sourceTimDataFile = filterFileBySuffix(sourceInfos.files(false),".tim");
        String desTimDataFile = filterFileBySuffix(desInfos.files(false),".tim");
        hackIndexData(sourceDirectory,sourceTimDataFile,desDirectory,desTimDataFile,true);

        sourceDirectory.close();
        desDirectory.close();
    }


    private static void processCodecCheckList(IndexInput sourceInput,IndexInput desInput,IndexOutput output) throws IOException
    {
        /* process CODEC_MAGIC*/
        output.writeInt(sourceInput.readInt());
        System.out.println("Skip codec magic "+(desInput.readInt()== CodecUtil.CODEC_MAGIC));
        /* process codec name*/
        output.writeString(sourceInput.readString());
        System.out.println("Skip codec name " +desInput.readString());
        /* process codec index version*/
        output.writeInt(sourceInput.readInt());
        System.out.println("Skip codec index version" +desInput.readInt());

        /** process index header ID (byte array with length 16)
         *  here we need the source index header ID
         */
        byte id[] = new byte[StringHelper.ID_LENGTH];
        sourceInput.readBytes(id,0,id.length);
        output.writeBytes(id,id.length);
        desInput.readBytes(id,0,id.length);
        System.out.println("Skip codec index header id "+StringHelper.idToString(id));

        /* process the suffix*/
        byte suffixLength = sourceInput.readByte();
        byte suffixBytes[] = new byte [suffixLength];
        sourceInput.readBytes(suffixBytes,0,suffixBytes.length);
        output.writeByte(suffixLength);
        output.writeBytes(suffixBytes,suffixBytes.length);

        suffixLength = desInput.readByte();
        suffixBytes = new byte[suffixLength];
        desInput.readBytes(suffixBytes,0,suffixBytes.length);
        System.out.println("Skip codec index suffix "+new String(suffixBytes,0,suffixLength, StandardCharsets.UTF_8));


    }
    private static void hackIndexData(Directory sourceDirectory,String sourceDocValuesFile,Directory desDirectory,String desDocValuesFile,boolean isTim) throws IOException
    {
        IndexInput sourceInput = sourceDirectory.openInput(sourceDocValuesFile, IOContext.READ);
        IndexInput desInput = desDirectory.openInput(desDocValuesFile,IOContext.READ);
        String newOutPutFileName = "new"+desDocValuesFile;
        IndexOutput output = desDirectory.createOutput(newOutPutFileName,IOContext.DEFAULT);

        processCodecCheckList(sourceInput,desInput,output);
        if(isTim){
            processCodecCheckList(sourceInput,desInput,output);
        }

        /*copy the index data */
        long currentFilePointer = desInput.getFilePointer();
        while(currentFilePointer!=(desInput.length()-CodecUtil.footerLength())){
            output.writeByte(desInput.readByte());
            currentFilePointer = desInput.getFilePointer();
        }
        System.out.println("Skip original indexing data");
        currentFilePointer = sourceInput.getFilePointer();
        while(currentFilePointer!=(sourceInput.length()-CodecUtil.footerLength())){
            sourceInput.readByte();
            currentFilePointer = sourceInput.getFilePointer();
        }

        /* process FOOTER_MAGIC*/
        output.writeInt(sourceInput.readInt());
        /* process algorithmID*/
        output.writeInt(sourceInput.readInt());
        output.writeLong(output.getChecksum());
        output.close();
        sourceInput.close();
        desInput.close();

        System.out.println("Start to overwrite the "+sourceDocValuesFile+" with "+newOutPutFileName);
        // more code here
        copyNewIndexData(newOutPutFileName,desDirectory,sourceDocValuesFile,sourceDirectory);
    }

    private static void copyNewIndexData(String source, Directory sourceDir ,String desc,Directory desDir) throws IOException{
        IndexInput indexInput = sourceDir.openInput(source,IOContext.READ);
        desDir.deleteFile(desc);
        IndexOutput output = desDir.createOutput(desc,IOContext.DEFAULT);
        long pointer = indexInput.getFilePointer();
        while(pointer!=indexInput.length()){
            output.writeByte(indexInput.readByte());
            pointer = indexInput.getFilePointer();
        }
        indexInput.close();
        output.close();
    }





    private static String filterFileBySuffix(Collection<String> fileNames , String suffix){
        for(String fileName : fileNames) {
            if(fileName.contains(CustomCodec.DOCVALUES_CODEC_NAME) && fileName.endsWith(suffix)) {
                return fileName;
            }
        }
        return null;
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
