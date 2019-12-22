#!/usr/bin/env bash
#download solr-7.7.2 from https://lucene.apache.org/solr/
echo "start download solr-7.7.2 from https://lucene.apache.org/solr/"
curl "http://mirror.bit.edu.cn/apache/lucene/solr/7.7.2/solr-7.7.2.tgz" -o solr-7.7.2.tgz
tar -xvf solr-7.7.2.tgz
echo "start prepare the jar files"
#set up solr (we use gradle to build the package , please make sure you already install it, the version we use in the demo is 4.10.1 https://downloads.gradle-dn.com/distributions/gradle-4.10.1-bin.zip)
cd ../../../../ && gradle clean jar && rm -rf src/main/resources/demo/solr_home/lib && mkdir src/main/resources/demo/solr_home/lib && cp build/libs/customized-codec-1.0-SNAPSHOT.jar src/main/resources/demo/solr_home/lib
# build the index jar
gradle -PallInOne clean jar && rm -rf src/main/resources/demo/*.jar && cp build/libs/customized-codec-1.0-SNAPSHOT.jar src/main/resources/demo/
# start download the data file
echo "start download the data file"
cd src/main/resources/demo/ && curl "http://s3.amazonaws.com/data.patentsview.org/20191008/download/patent.tsv.zip" -o patent.tsv.zip && unzip patent.tsv.zip && mv 20*/download/patent.tsv ./patent.tsv && rm -rf 20* patent.tsv.zip
echo "start CODEC solr and index data"
# start solr and index the data
./solr-7.7.2/bin/solr start -p 8983 -m 1G -s solr_home && java -cp customized-codec-1.0-SNAPSHOT.jar org.fql.codec.index.IndexPatentData patent.tsv 2 http://127.0.0.1:8983/solr/CODEC
# execute the query
curl "http://127.0.0.1:8983/solr/CODEC/select?q=*:*&facet=true&facet.field=type&facet.limit=10&wt=json&indent=on&rows=0"
# start hack the indexing
echo "start stop Solr and hack Lucene Index"
rm -rf newIndex && mkdir newIndex && ./solr-7.7.2/bin/solr stop -p 8983
java -cp customized-codec-1.0-SNAPSHOT.jar:solr_home/lib/customized-codec-1.0-SNAPSHOT.jar  org.fql.codec.index.GeneratePerFieldIndex solr_home/CODEC/data/index type false newIndex
# stop solr and done
echo "start solr and execute query , you should see 'utility' has been change to 'fql_demo' "
./solr-7.7.2/bin/solr start -p 8983 -m 1G -s solr_home
curl "http://127.0.0.1:8983/solr/CODEC/select?q=*:*&facet=true&facet.field=type&facet.limit=10&wt=json&indent=on&rows=0"
rm -rf newIndex && ./solr-7.7.2/bin/solr stop -p 8983