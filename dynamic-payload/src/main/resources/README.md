**instructions(说明)**
>* The jar dynamic-payload-1.0-SNAPSHOT.jar is built by command 'mvn clean package'
   dynamic-payload-xxx.jar 是使用命令'mvn clean package'命令生成的
>* Please put the dynamic-payload-1.0-SNAPSHOT.jar under solr_home/TEST/lib folder (create the lib folder if not exists), before you run the dynamic_pay_load_demo.sh
   在运行dynamic_pay_load_demo.sh脚本前请将dynamic-payload-1.0-SNAPSHOT.jar放到solr_home/TEST/lib目录下(如果lib目录不存在则新建)
>* Since the paylaod QParser is not 100% ready for plugin mode, so we need change some code of solr source code
   (int the patch file)
   由于solr的payload QParser没有100%可以通过插件形式开发，所以我们得对源码做一些改动（在patch文件中）
>* you can find all the details & execute scripts in 'resource' folder
   在'resource'文件夹下可以找到相应的文件和可执行的demo脚本

