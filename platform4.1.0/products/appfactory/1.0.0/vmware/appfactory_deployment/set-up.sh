#JAVA_HOME=/opt/jdk1.6.0_25
#export JAVA_HOME
#PATH=$PATH:/uer/bin:$JAVA_HOME/bin
#export PATH

M3_HOME=/opt/maven/apache-maven-3.0.4
export M3_HOME
export PATH=$M3_HOME/bin:$PATH

ANT_HOME=/opt/ant/apache-ant-1.8.4
export ANT_HOME
export PATH=$ANT_HOME/bin:$PATH
#This is required to skip host name verfication in git client
export GIT_SSL_NO_VERIFY=1

ELB_HOME=`pwd`/setup/elb/wso2elb-2.0.3
APPFACTORY_HOME=`pwd`/setup/appfactory/wso2appfactory-1.0.0
CONTROLLER_HOME=`pwd`/setup/controller/wso2stratos-manager-2.0.2
DEV_CLOUD_AS_HOME=`pwd`/setup/dev-cloud/wso2as-5.0.2
TEST_CLOUD_AS_HOME=`pwd`/setup/test-cloud/wso2as-5.0.2
PROD_CLOUD_AS_HOME=`pwd`/setup/prod-cloud/wso2as-5.0.2
STAGING_CLOUD_AS_HOME=`pwd`/setup/staging-cloud/wso2as-5.0.2
APIMANAGER_HOME=`pwd`/setup/apimanager/wso2am-1.4.0
JENKINS_HOME=`pwd`/setup/jenkins
REDMINE_HOME=`pwd`/setup/redmine/apache-tomcat-7.0.32
GITBLIT_HOME=`pwd`/setup/gitblit
DEV_SS_HOME=`pwd`/setup/dev-ss/wso2ss-1.0.2
TEST_SS_HOME=`pwd`/setup/test-ss/wso2ss-1.0.2
STAGING_SS_HOME=`pwd`/setup/staging-ss/wso2ss-1.0.2
PROD_SS_HOME=`pwd`/setup/prod-ss/wso2ss-1.0.2


echo "*******Starting to set up Appfactory************"
#resources maven
#unzip resources/apache-maven-2.2.1-bin.zip
#resources java
#sh resources/jdk-6u25-linux-x64.bi
SETUP_DIR=`pwd`/setup;
echo $SETUP_DIR
if [ ! -d "$SETUP_DIR" ];then
echo "Setting up the deployment first time"
#cd /home/appfactory/wso2appfactory_m3
#/usr/bin/unzip  -q resources/m2-repo.zip -d /root
#mvn clean install
mkdir setup
#configure elb
mkdir setup/elb
echo "Setting up ELB........"

# Commenting elb 2.0.1 generation due to a bug in it.
# We will be using elb 2.0.0 which is in resources folder
#cp -r elb/target/wso2elb-2.0.1 setup/elb

/usr/bin/unzip  -q resources/wso2elb-2.0.3.zip -d setup/elb

cp resources/elb-carbon.xml $ELB_HOME/repository/conf/carbon.xml
cp resources/elb-axis2.xml $ELB_HOME/repository/conf/axis2/axis2.xml
cp resources/loadbalancer.conf  $ELB_HOME/repository/conf

#configure appfactory
mkdir setup/appfactory
echo "Setting up Appfactory........"
#cp -r  appfactory/target/wso2appfactory-1.0.0  setup/appfactory
/usr/bin/unzip  -q resources/wso2appfactory-1.0.0.zip   -d setup/appfactory

cp resources/wso2server.sh $APPFACTORY_HOME/bin
cp resources/appfactory.xml $APPFACTORY_HOME/repository/conf/appfactory/appfactory.xml
cp resources/appfactory-user-mgt.xml $APPFACTORY_HOME/repository/conf/user-mgt.xml
cp resources/appfactory-registry.xml $APPFACTORY_HOME/repository/conf/registry.xml
cp resources/appfactory.xml $APPFACTORY_HOME/repository/conf/appfactory/appfactory.xml
cp resources/appfactory-carbon.xml $APPFACTORY_HOME/repository/conf/carbon.xml
cp resources/appfactory-axis2.xml $APPFACTORY_HOME/repository/conf/axis2/axis2.xml
cp resources/appfactory-confirmation-email-config.xml $APPFACTORY_HOME/repository/conf/email/confirmation-email-config.xml
cp resources/appfactory-invite-user-email-config.xml $APPFACTORY_HOME/repository/conf/email/invite-user-email-config.xml
cp resources/appfactory-sso-idp-config.xml $APPFACTORY_HOME/repository/conf/sso-idp-config.xml
cp resources/appfactory-humantask.xml $APPFACTORY_HOME/repository/conf/humantask.xml
cp resources/carbon-console-web.xml $APPFACTORY_HOME/repository/conf/tomcat/carbon/WEB-INF/web.xml

cp resources/org.wso2.carbon.appfactory.apiManager.integration-1.0.2.jar $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.apiManager.integration_1.0.2.jar
#this is required for fast app page load
cp resources/cloud-manager-tenant-mgt.xml $APPFACTORY_HOME/repository/conf/tenant-mgt.xml 


cp resources/mysql-connector-java-5.1.12-bin.jar $APPFACTORY_HOME/repository/components/lib
mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/af-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.archetype -DartifactId=af-archetype -Dversion=1.0.0 -Dpackaging=jar
mvn archetype:generate -DartifactId=afdefault -DgroupId=org.wso2.af -DarchetypeArtifactId=maven-archetype-webapp -Dversion=SNAPSHOT -DinteractiveMode=false

mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/jaxrs-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.jaxrsarchetype -DartifactId=jaxrs-archetype -Dversion=1.0.0 -Dpackaging=jar
mvn archetype:generate -DartifactId=jaxrsdefault -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.jaxrsarchetype -DarchetypeArtifactId=jaxrs-archetype -DarchetypeVersion=1.0.0 -DgroupId=org.wso2.af -Dversion=SNAPSHOT -DinteractiveMode=false -DarchetypeCatalog=local

mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/jaxws-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.jaxwsarchetype -DartifactId=jaxws-archetype -Dversion=1.0.0 -Dpackaging=jar
mvn archetype:generate -DartifactId=jaxwsdefault -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.jaxwsarchetype -DarchetypeArtifactId=jaxws-archetype -DarchetypeVersion=1.0.0 -DgroupId=org.wso2.af -Dversion=SNAPSHOT -DinteractiveMode=false -DarchetypeCatalog=local

mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/jaggery-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.jaggeryarchetype -DartifactId=jaggery-archetype -Dversion=1.0.0 -Dpackaging=jar

mvn archetype:generate -DartifactId=jaggerydefault -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.jaggeryarchetype -DarchetypeArtifactId=jaggery-archetype -DarchetypeVersion=1.0.0 -DgroupId=org.wso2.af -Dversion=SNAPSHOT -DinteractiveMode=false -DarchetypeCatalog=local
 
mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/bpel-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.bpelarchetype -DartifactId=bpel-archetype -Dversion=1.0.0 -Dpackaging=jar

mvn archetype:generate -DartifactId=bpeldefault -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.bpelarchetype -DarchetypeArtifactId=bpel-archetype -DarchetypeVersion=1.0.0 -DgroupId=org.wso2.af -Dversion=SNAPSHOT -DinteractiveMode=false -DarchetypeCatalog=local

mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/esb-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.esbarchetype -DartifactId=esb-archetype -Dversion=1.0.0 -Dpackaging=jar

mvn archetype:generate -DartifactId=esbdefault -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.esbarchetype -DarchetypeArtifactId=esb-archetype -DarchetypeVersion=1.0.0 -DgroupId=org.wso2.af -Dversion=SNAPSHOT -DinteractiveMode=false -DarchetypeCatalog=local

mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/dbs-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.dbsarchetype -DartifactId=dbs-archetype -Dversion=1.0.0 -Dpackaging=jar
 
mvn archetype:generate -DartifactId=dbsdefault -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.dbsarchetype -DarchetypeArtifactId=dbs-archetype -DarchetypeVersion=1.0.0 -DgroupId=org.wso2.af -Dversion=SNAPSHOT -DinteractiveMode=false -DarchetypeCatalog=local

mvn install:install-file -Dfile=$APPFACTORY_HOME/repository/resources/maven/php-archetype-1.0.0.jar -DgroupId=org.wso2.carbon.appfactory.maven.phparchetype -DartifactId=php-archetype -Dversion=1.0.0 -Dpackaging=jar

mvn archetype:generate -DartifactId=phpdefault -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.phparchetype -DarchetypeArtifactId=php-archetype -DarchetypeVersion=1.0.0 -DgroupId=org.wso2.af -Dversion=SNAPSHOT -DinteractiveMode=false -DarchetypeCatalog=local

#configure controller
mkdir setup/controller
echo "Setting up Controller........"
/usr/bin/unzip  -q resources/wso2stratos-manager-2.0.2 -d setup/controller

cp resources/cloud-manager-user-mgt.xml $CONTROLLER_HOME/repository/conf/user-mgt.xml
cp resources/cloud-manager-axis2.xml $CONTROLLER_HOME/repository/conf/axis2/axis2.xml
cp resources/cloud-manager-registry.xml $CONTROLLER_HOME/repository/conf/registry.xml
cp resources/cloud-manager-carbon.xml $CONTROLLER_HOME/repository/conf/carbon.xml
cp resources/cloud-manager-tenant-mgt.xml $CONTROLLER_HOME/repository/conf/tenant-mgt.xml
cp resources/cloud-manager-stratos.xml $CONTROLLER_HOME/repository/conf/multitenancy/stratos.xml

mkdir $CONTROLLER_HOME/repository/conf/appfactory
cp $APPFACTORY_HOME/repository/conf/appfactory/appfactory.xml $CONTROLLER_HOME/repository/conf/appfactory

cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.common_1.0.2.jar $CONTROLLER_HOME/repository/components/dropins
cp $APPFACTORY_HOME/repository/lib/org.wso2.carbon.appfactory.tenant.roles-1.0.2.jar $CONTROLLER_HOME/repository/components/dropins
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $CONTROLLER_HOME/repository/components/dropins
cp resources/mysql-connector-java-5.1.12-bin.jar $CONTROLLER_HOME/repository/components/lib

#configure dev cloud
mkdir setup/dev-cloud
echo "Setting up Dev Cloud........"
#cp -r as/target/wso2as-5.0.0  setup/dev-cloud
#cp -r as/target/wso2as-5.0.1 setup/dev-cloud
/usr/bin/unzip  -q resources/wso2as-5.0.2 -d setup/dev-cloud
cp resources/cloud-manager-user-mgt.xml $DEV_CLOUD_AS_HOME/repository/conf/user-mgt.xml
cp resources/dev-cloud-registry.xml $DEV_CLOUD_AS_HOME/repository/conf/registry.xml
cp resources/dev-cloud-carbon.xml $DEV_CLOUD_AS_HOME/repository/conf/carbon.xml
cp resources/dev-cloud-axis2.xml $DEV_CLOUD_AS_HOME/repository/conf/axis2/axis2.xml
cp resources/catalina-server.xml $DEV_CLOUD_AS_HOME/repository/conf/tomcat/catalina-server.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $DEV_CLOUD_AS_HOME/repository/components/dropins
cp resources/mysql-connector-java-5.1.12-bin.jar $DEV_CLOUD_AS_HOME/repository/components/lib
cp resources/org.wso2.carbon.appfactory.registry.handler-1.0.2.jar $DEV_CLOUD_AS_HOME/repository/components/dropins
cp resources/as-wso2server.sh $DEV_CLOUD_AS_HOME/bin
cp resources/org.wso2.carbon.tomcat_4.0.5.jar $DEV_CLOUD_AS_HOME/repository/components/plugins/org.wso2.carbon.tomcat_4.0.5.jar

#configure test cloud
mkdir setup/test-cloud
echo "Setting up Testing Cloud........"
#cp -r  as/target/wso2as-5.0.1  setup/test-cloud
/usr/bin/unzip  -q resources/wso2as-5.0.2 -d setup/test-cloud
#/usr/bin/unzip  -q resources/wso2as-5.0.0.zip  -d setup/test-cloud


cp resources/cloud-manager-user-mgt.xml $TEST_CLOUD_AS_HOME/repository/conf/user-mgt.xml
cp resources/test-cloud-registry.xml $TEST_CLOUD_AS_HOME/repository/conf/registry.xml
cp resources/test-cloud-carbon.xml $TEST_CLOUD_AS_HOME/repository/conf/carbon.xml
cp resources/test-cloud-axis2.xml $TEST_CLOUD_AS_HOME/repository/conf/axis2/axis2.xml
cp resources/catalina-server.xml $TEST_CLOUD_AS_HOME/repository/conf/tomcat/catalina-server.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $TEST_CLOUD_AS_HOME/repository/components/dropins
cp resources/mysql-connector-java-5.1.12-bin.jar $TEST_CLOUD_AS_HOME/repository/components/lib
cp resources/org.wso2.carbon.appfactory.registry.handler-1.0.2.jar $TEST_CLOUD_AS_HOME/repository/components/dropins
cp resources/as-wso2server.sh $TEST_CLOUD_AS_HOME/bin
cp resources/org.wso2.carbon.tomcat_4.0.5.jar $TEST_CLOUD_AS_HOME/repository/components/plugins/org.wso2.carbon.tomcat_4.0.5.jar

#configure prod cloud
mkdir setup/prod-cloud
echo "Setting up Prod Cloud........"
#cp -r  as/target/wso2as-5.0.1  setup/prod-cloud
/usr/bin/unzip  -q resources/wso2as-5.0.2 -d  setup/prod-cloud
#/usr/bin/unzip  -q resources/wso2as-5.0.0.zip  -d setup/prod-cloud


cp resources/cloud-manager-user-mgt.xml $PROD_CLOUD_AS_HOME/repository/conf/user-mgt.xml
cp resources/prod-cloud-registry.xml $PROD_CLOUD_AS_HOME/repository/conf/registry.xml
cp resources/prod-cloud-carbon.xml $PROD_CLOUD_AS_HOME/repository/conf/carbon.xml
cp resources/prod-cloud-axis2.xml $PROD_CLOUD_AS_HOME/repository/conf/axis2/axis2.xml
cp resources/catalina-server.xml $PROD_CLOUD_AS_HOME/repository/conf/tomcat/catalina-server.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $PROD_CLOUD_AS_HOME/repository/components/dropins
cp resources/mysql-connector-java-5.1.12-bin.jar $PROD_CLOUD_AS_HOME/repository/components/lib 
cp resources/org.wso2.carbon.appfactory.registry.handler-1.0.2.jar $PROD_CLOUD_AS_HOME/repository/components/dropins 
cp resources/as-wso2server.sh $PROD_CLOUD_AS_HOME/bin
cp resources/org.wso2.carbon.tomcat_4.0.5.jar $PROD_CLOUD_AS_HOME/repository/components/plugins/org.wso2.carbon.tomcat_4.0.5.jar

#configure staging cloud
mkdir setup/staging-cloud
echo "Setting up staging Cloud........"
/usr/bin/unzip  -q resources/wso2as-5.0.2 -d  setup/staging-cloud
#/usr/bin/unzip  -q resources/wso2as-5.0.0.zip  -d setup/prod-cloud


cp resources/cloud-manager-user-mgt.xml $STAGING_CLOUD_AS_HOME/repository/conf/user-mgt.xml
cp resources/staging-cloud-registry.xml $STAGING_CLOUD_AS_HOME/repository/conf/registry.xml
cp resources/staging-cloud-carbon.xml $STAGING_CLOUD_AS_HOME/repository/conf/carbon.xml
cp resources/staging-cloud-axis2.xml $STAGING_CLOUD_AS_HOME/repository/conf/axis2/axis2.xml
cp resources/catalina-server.xml $STAGING_CLOUD_AS_HOME/repository/conf/tomcat/catalina-server.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $STAGING_CLOUD_AS_HOME/repository/components/dropins
cp resources/mysql-connector-java-5.1.12-bin.jar $STAGING_CLOUD_AS_HOME/repository/components/lib
cp resources/org.wso2.carbon.appfactory.registry.handler-1.0.2.jar $STAGING_CLOUD_AS_HOME/repository/components/dropins
cp resources/as-wso2server.sh $STAGING_CLOUD_AS_HOME/bin
cp -r resources/patch0001 $STAGING_CLOUD_AS_HOME/repository/components/patches

cp resources/org.wso2.carbon.tomcat_4.0.5.jar $STAGING_CLOUD_AS_HOME/repository/components/plugins/org.wso2.carbon.tomcat_4.0.5.jar
#config jenkins
echo "Setting up Jenkins ........"
mkdir setup/jenkins
mkdir setup/jenkins/storage
mkdir setup/jenkins/repository
mkdir setup/jenkins/repository/plugins

cp resources/jenkins.war  setup/jenkins
cp resources/jenkinsServer.sh setup/jenkins
cp resources/hudson.tasks.Maven.xml setup/jenkins/repository
cp resources/hudson.plugins.sonar.SonarPublisher.xml setup/jenkins/repository
cat resources/org.wso2.carbon.appfactory.jenkins.AppfactoryPluginManager.xml | sed -e "s@APPFACTORY_HOME@$APPFACTORY_HOME@g" | sed -e "s@JENKINS_HOME@$JENKINS_HOME@g" > setup/jenkins/repository/org.wso2.carbon.appfactory.jenkins.AppfactoryPluginManager.xml
cat resources/config.xml | sed -e "s@APPFACTORY_HOME@$APPFACTORY_HOME@g"  > setup/jenkins/repository/config.xml
cp resources/*.hpi setup/jenkins/repository/plugins
cp resources/*.jpi setup/jenkins/repository/plugins
cp resources/.netrc /root


#config api manager
echo "Setting up APIManager ........"
mkdir setup/apimanager
/usr/bin/unzip  -q resources/wso2am-1.4.0.zip -d setup/apimanager

cp resources/api-manager-user-mgt.xml $APIMANAGER_HOME/repository/conf/user-mgt.xml
cp resources/api-manager-carbon.xml $APIMANAGER_HOME/repository/conf/carbon.xml
cp resources/api-manager-registry.xml $APIMANAGER_HOME/repository/conf/registry.xml
cp resources/api-manager-api-manager.xml $APIMANAGER_HOME/repository/conf/api-manager.xml
cp resources/api-manager-master-datasource.xml $APIMANAGER_HOME/repository/conf/datasources/master-datasources.xml
cp resources/mysql-connector-java-5.1.12-bin.jar $APIMANAGER_HOME/repository/components/lib

#This is a temp fix
cp resources/org.wso2.carbon.identity.authenticator.saml2.sso-4.0.3.jar $APIMANAGER_HOME/repository/components/dropins

cat resources/api-manager-publisher-site.json | sed -e "s@APPFACTORY_HOME@$APPFACTORY_HOME@g" | sed -e "s@APIMANAGER_HOME@$APIMANAGER_HOME@g" > $APIMANAGER_HOME/repository/deployment/server/jaggeryapps/publisher/site/conf/site.json
cat resources/api-manager-store-site.json | sed -e "s@APPFACTORY_HOME@$APPFACTORY_HOME@g" | sed -e "s@APIMANAGER_HOME@$APIMANAGER_HOME@g" > $APIMANAGER_HOME/repository/deployment/server/jaggeryapps/store/site/conf/site.json

#config dev-ss
echo "Setting up Development Storage Server"
mkdir setup/dev-ss
/usr/bin/unzip -q resources/wso2ss-1.0.2.zip -d setup/dev-ss

cp resources/dev-ss-rss-config.xml $DEV_SS_HOME/repository/conf/etc/rss-config.xml
cp resources/dev-ss-carbon.xml $DEV_SS_HOME/repository/conf/carbon.xml
cp resources/mysql-connector-java-5.1.12-bin.jar $DEV_SS_HOME/repository/components/lib
cp resources/appfactory-user-mgt.xml $DEV_SS_HOME/repository/conf/user-mgt.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $DEV_SS_HOME/repository/components/dropins

cp resources/org.wso2.carbon.rssmanager.core_4.0.5.jar $DEV_SS_HOME/repository/components/plugins/org.wso2.carbon.rssmanager.core_4.0.5.jar

#config test-ss
echo "Setting up Testing Storage Server"
mkdir setup/test-ss
/usr/bin/unzip -q resources/wso2ss-1.0.2.zip -d setup/test-ss

cp resources/test-ss-rss-config.xml $TEST_SS_HOME/repository/conf/etc/rss-config.xml
cp resources/test-ss-carbon.xml $TEST_SS_HOME/repository/conf/carbon.xml
cp resources/mysql-connector-java-5.1.12-bin.jar $TEST_SS_HOME/repository/components/lib
cp resources/appfactory-user-mgt.xml $TEST_SS_HOME/repository/conf/user-mgt.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $TEST_SS_HOME/repository/components/dropins

cp resources/org.wso2.carbon.rssmanager.core_4.0.5.jar $TEST_SS_HOME/repository/components/plugins/org.wso2.carbon.rssmanager.core_4.0.5.jar

#config staging-ss
echo "Setting up Staging Storage Server"
mkdir setup/staging-ss
/usr/bin/unzip -q resources/wso2ss-1.0.2.zip -d setup/staging-ss

cp resources/staging-ss-rss-config.xml $STAGING_SS_HOME/repository/conf/etc/rss-config.xml
cp resources/staging-ss-carbon.xml $STAGING_SS_HOME/repository/conf/carbon.xml
cp resources/mysql-connector-java-5.1.12-bin.jar $STAGING_SS_HOME/repository/components/lib
cp resources/appfactory-user-mgt.xml $STAGING_SS_HOME/repository/conf/user-mgt.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $STAGING_SS_HOME/repository/components/dropins

cp resources/org.wso2.carbon.rssmanager.core_4.0.5.jar $STAGING_SS_HOME/repository/components/plugins/org.wso2.carbon.rssmanager.core_4.0.5.jar

#config prod-ss
echo "Setting up Production Storage Server"
mkdir setup/prod-ss
/usr/bin/unzip -q resources/wso2ss-1.0.2.zip -d setup/prod-ss

cp resources/prod-ss-rss-config.xml $PROD_SS_HOME/repository/conf/etc/rss-config.xml
cp resources/prod-ss-carbon.xml $PROD_SS_HOME/repository/conf/carbon.xml
cp resources/mysql-connector-java-5.1.12-bin.jar $PROD_SS_HOME/repository/components/lib
cp resources/appfactory-user-mgt.xml $PROD_SS_HOME/repository/conf/user-mgt.xml
cp $APPFACTORY_HOME/repository/components/plugins/org.wso2.carbon.appfactory.userstore_1.0.2.jar $PROD_SS_HOME/repository/components/dropins

cp resources/org.wso2.carbon.rssmanager.core_4.0.5.jar $PROD_SS_HOME/repository/components/plugins/org.wso2.carbon.rssmanager.core_4.0.5.jar

#config redmine
echo "Setting up Redmine ........"
mkdir setup/redmine
/usr/bin/unzip  -q resources/apache-tomcat-7.0.32.zip  -d setup/redmine 

#config gitblit
echo "Setting up Gitblit ........"
mkdir setup/gitblit
#Download from http://gitblit.googlecode.com/files/gitblit-1.1.0.zip
/usr/bin/unzip  -q resources/gitblit-1.1.0.zip  -d setup/gitblit
cat resources/gitblit.properties | sed -e "s@APPFACTORY_HOME@$APPFACTORY_HOME@g" > setup/gitblit/gitblit.properties
#following are copied through feature now
#cp resources/org.wso2.carbon.appfactory.gitblit.git.repository.provider-1.0.2.jar $APPFACTORY_HOME/repository/components/dropins
#cp $GITBLIT_HOME/gitblit.jar $APPFACTORY_HOME/repository/components/lib
cp resources/appfactory.gitblit.plugin-0.0.1-SNAPSHOT-jar-with-dependencies.jar $GITBLIT_HOME/ext
cp resources/jenkins.groovy $GITBLIT_HOME/groovy

chmod +x $APPFACTORY_HOME/bin/wso2server.sh
chmod +x $CONTROLLER_HOME/bin/wso2server.sh
chmod +x $DEV_CLOUD_AS_HOME/bin/wso2server.sh 
chmod +x $TEST_CLOUD_AS_HOME/bin/wso2server.sh
chmod +x $PROD_CLOUD_AS_HOME/bin/wso2server.sh
chmod +x $STAGING_CLOUD_AS_HOME/bin/wso2server.sh
chmod +x $ELB_HOME/bin/wso2server.sh
chmod +x $APIMANAGER_HOME/bin/wso2server.sh

echo "Creating Databases ........"

MYSQL=`which mysql`
 
Q1="DROP DATABASE IF EXISTS userstore;"
Q2="DROP DATABASE IF EXISTS registry;"
Q3="DROP DATABASE IF EXISTS am;"
Q4="DROP DATABASE IF EXISTS registryAM;"

Q5="CREATE DATABASE userstore;"
Q6="USE userstore;"
Q7="SOURCE $APPFACTORY_HOME/dbscripts/mysql.sql;"

Q8="CREATE DATABASE registry;"
Q9="USE registry;"

Q10="CREATE DATABASE registryAM;"
Q11="USE registryAM;"

Q12="CREATE DATABASE am;"
Q13="USE am;"
Q14="SOURCE $APIMANAGER_HOME/dbscripts/apimgt/mysql.sql;"
Q15="SOURCE $APIMANAGER_HOME/dbscripts/mysql.sql;"

Q16="CREATE DATABASE rss_mgt;"
Q17="USE rss_mgt;"
Q18="SOURCE $DEV_SS_HOME/dbscripts/mysql/wso2_rss_mysql.sql;"

Q19="DROP DATABASE IF EXISTS rss_mgt;"

SQL="${Q1}${Q2}${Q3}${Q4}${Q5}${Q6}${Q7}${Q8}${Q9}${Q7}${Q10}${Q11}${Q15}${Q12}${Q13}${Q14}"

$MYSQL -uroot -proot -A -e "$SQL";

RSS_SQL="${Q19}${Q16}${Q17}${Q18}";

DEV_SOCK="/var/run/mysqld/mysqld1.sock";
$MYSQL -S"$DEV_SOCK" -uroot -proot -A -e "$RSS_SQL";

TEST_SOCK="/var/run/mysqld/mysqld2.sock";
$MYSQL -S"$TEST_SOCK" -uroot -proot -A -e "$RSS_SQL";

STAGING_SOCK="/var/run/mysqld/mysqld3.sock";
$MYSQL -S"$STAGING_SOCK" -uroot -proot -A -e "$RSS_SQL";

PROD_SOCK="/var/run/mysqld/mysqld4.sock";
$MYSQL -S"$PROD_SOCK" -uroot -proot -A -e "$RSS_SQL";

fi

echo "Starting ELB ........"
cd $ELB_HOME/bin
nohup ./wso2server.sh start &

echo "Starting App Factory ........"
cd  $APPFACTORY_HOME/bin
nohup ./wso2server.sh  start &
sleep 1m

echo "Starting Controller ........"
cd $CONTROLLER_HOME/bin
nohup ./wso2server.sh start &

echo "Starting DEV as ........"
cd $DEV_CLOUD_AS_HOME/bin
nohup ./wso2server.sh start & 

echo "Starting TEST as ........"
cd $TEST_CLOUD_AS_HOME/bin
nohup ./wso2server.sh start & 

echo "Starting STAGING as ........"
cd $STAGING_CLOUD_AS_HOME/bin
nohup ./wso2server.sh start & 

echo "Starting PROD as ........"
cd $PROD_CLOUD_AS_HOME/bin
nohup ./wso2server.sh start &

echo "Starting API Manager ........"
cd $APIMANAGER_HOME/bin
nohup ./wso2server.sh start & 

#configure scm manager
echo "staring jenkins........"
cd $JENKINS_HOME
JENKINS_HOME=`pwd`/repository;
export JENKINS_HOME;
nohup java -jar jenkins.war &

echo "Starting Redmine ........"
cd $REDMINE_HOME/bin
nohup sh startup.sh &

echo "Starting Gitblit ........"
cd $GITBLIT_HOME
nohup java -jar gitblit.jar &


#unzip -q resourcs/scm-server-app.zip 
#cp resources/scm-server-config.xml scm-server/conf/server-config.xml
