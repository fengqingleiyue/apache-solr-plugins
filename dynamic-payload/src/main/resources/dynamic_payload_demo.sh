#!/bin/bash
echo "clone solr source code from github"
git clone https://github.com/apache/lucene-solr.git
echo "reset code to solr-7.7.2 (d4c30fc2856154f2c1fefc589eb7cd070a415b94)"
cd lucene-solr && git reset --hard d4c30fc2856154f2c1fefc589eb7cd070a415b94 && cd ../
echo "apply the payload patch"
cp patch ./lucene-solr && cd ./lucene-solr && git apply patch && cd ../
echo "start build solr-core jar files"
cd ./lucene-solr/ && ant ivy-bootstrap && cd ./solr && ant dist && cd ../../
echo "start download solr project"
wget "https://www.apache.org/dyn/closer.lua/lucene/solr/7.7.2/solr-7.7.2.tgz" && tar -xvf solr-7.7.2.tgz
echo "apply the solr-core jars"
rm -rf solr-7.7.2/server/solr-webapp/webapp/WEB-INF/lib/solr-core-7.7.2.jar
cp lucene-solr/solr/dist/solr-core-7.7.2-SNAPSHOT.jar solr-7.7.2/server/solr-webapp/webapp/WEB-INF/lib/solr-core-7.7.2.jar
echo "start solr on port 38984"
./solr-7.7.2/bin/solr start -p 38984 -m 1g -s ./solr_home -force
echo "insert sample docs into TEST"
curl -X POST -d '[{"id":"doc1","DATA":["[|A|]test1"]}]' "http://127.0.0.1:38984/solr/TEST/update?commitWithin=1000&overwrite=true&wt=json"
curl -X POST -d '[{"id":"doc2","DATA":["[|B|]test1"]}]' "http://127.0.0.1:38984/solr/TEST/update?commitWithin=1000&overwrite=true&wt=json"
curl -X POST -d '[{"id":"doc3","DATA":["[|C|]test1"]}]' "http://127.0.0.1:38984/solr/TEST/update?commitWithin=1000&overwrite=true&wt=json"
curl -X POST -d '[{"id":"doc4","DATA":["[|D|]test1"]}]' "http://127.0.0.1:38984/solr/TEST/update?commitWithin=1000&overwrite=true&wt=json"
echo "query example"
curl 'http://127.0.0.1:38984/solr/TEST/select?q=%7B!payload_check+f%3DDATA+payloads%3D1%7Dtest1'
echo "stop solr intance"
./solr-7.7.2/bin/solr  stop



