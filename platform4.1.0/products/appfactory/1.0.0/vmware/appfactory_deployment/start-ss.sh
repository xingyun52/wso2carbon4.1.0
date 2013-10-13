JAVA_HOME=/opt/jdk1.6.0_25
export JAVA_HOME
PATH=$PATH:/uer/bin:$JAVA_HOME/bin
export PATH

M3_HOME=/opt/maven/apache-maven-3.0.4
export M3_HOME
export PATH=$M3_HOME/bin:$PATH

ANT_HOME=/opt/ant/apache-ant-1.8.4
export ANT_HOME
export PATH=$ANT_HOME/bin:$PATH
#This is required to skip host name verfication in git client
export GIT_SSL_NO_VERIFY=1


DEV_SS_HOME=`pwd`/setup/dev-ss/wso2ss-1.0.2
TEST_SS_HOME=`pwd`/setup/test-ss/wso2ss-1.0.2
STAGING_SS_HOME=`pwd`/setup/staging-ss/wso2ss-1.0.2
PROD_SS_HOME=`pwd`/setup/prod-ss/wso2ss-1.0.2

cd $DEV_SS_HOME/bin
nohup ./wso2server.sh start &

cd $TEST_SS_HOME/bin
nohup ./wso2server.sh start &

cd $STAGING_SS_HOME/bin
nohup ./wso2server.sh start &

cd $PROD_SS_HOME/bin
nohup ./wso2server.sh start &

sleep 1m

tail -f $DEV_SS_HOME/repository/logs/wso2carbon.log | while read -t 30 line 
  do
    if echo $line | grep -q 'WSO2 Carbon started'; then
      echo 'Server Started'
      
      MYSQL=`which mysql`

      Q1="use rss_mgt;"
      Q2="update RM_SERVER_INSTANCE SET TENANT_ID=-1234;"

      SQL="${Q1}${Q2}";
      SOCK="/var/run/mysqld/mysqld1.sock";
      $MYSQL -S"$SOCK" -uroot -proot -A -e "$SQL";

      cd $DEV_SS_HOME/bin
      nohup ./wso2server.sh restart &
      break;
    fi
  done

tail -f $TEST_SS_HOME/repository/logs/wso2carbon.log | while read -t 30 line 
  do
    if echo $line | grep -q 'WSO2 Carbon started'; then
      echo 'Server Started'

      MYSQL=`which mysql`

      Q1="use rss_mgt;"
      Q2="update RM_SERVER_INSTANCE SET TENANT_ID=-1234;"

      SQL="${Q1}${Q2}";
      SOCK="/var/run/mysqld/mysqld2.sock";
      $MYSQL -S"$SOCK" -uroot -proot -A -e "$SQL";

      cd $TEST_SS_HOME/bin
      nohup ./wso2server.sh restart &
      break;
    fi
  done

tail -f $STAGING_SS_HOME/repository/logs/wso2carbon.log | while read -t 30 line 
  do
    if echo $line | grep -q 'WSO2 Carbon started'; then
      echo 'Server Started'

      MYSQL=`which mysql`

      Q1="use rss_mgt;"
      Q2="update RM_SERVER_INSTANCE SET TENANT_ID=-1234;"

      SQL="${Q1}${Q2}";
      SOCK="/var/run/mysqld/mysqld3.sock";
      $MYSQL -S"$SOCK" -uroot -proot -A -e "$SQL";

      cd $STAGING_SS_HOME/bin
      nohup ./wso2server.sh restart &
      break;
    fi
  done

tail -f $PROD_SS_HOME/repository/logs/wso2carbon.log | while read -t 30 line 
  do
    if echo $line | grep -q 'WSO2 Carbon started'; then
      echo 'Server Started'

      MYSQL=`which mysql`

      Q1="use rss_mgt;"
      Q2="update RM_SERVER_INSTANCE SET TENANT_ID=-1234;"

      SQL="${Q1}${Q2}";
      SOCK="/var/run/mysqld/mysqld4.sock";
      $MYSQL -S"$SOCK" -uroot -proot -A -e "$SQL";

      cd $PROD_SS_HOME/bin
      nohup ./wso2server.sh restart &
      break;
    fi
  done
