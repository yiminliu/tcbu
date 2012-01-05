#! /usr/bin/ksh
. /apps/webadmin/.profile


PROJECT=$HOME/tscp/mvne/truconnect

CLASSPATH=$CLASSPATH:$PROJECT/bin/
CLASSPATH=$CLASSPATH:$PROJECT/lib/ojdbc14.jar
#CLASSPATH=$CLASSPATH:$PROJECT/lib/truconnect_test_1.7.3.jar
#CLASSPATH=$CLASSPATH:$PROJECT/lib/truconnect_test_1.8.2.2.jar
#CLASSPATH=$CLASSPATH:$PROJECT/lib/truconnect_test_1.9.1.3.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/truconnect_test_1.9.2.5.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/slf4j-api-1.6.1.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/slf4j-log4j12-1.6.1.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/mail.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/log4j-1.2.16.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/jta-1.1.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/javassist-3.12.0.GA.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/hibernate3.jar

CLASSPATH=$CLASSPATH:$PROJECT/lib/dom4j-1.6.1.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/commons-collections-3.1.jar
CLASSPATH=$CLASSPATH:$PROJECT/lib/antlr-2.7.6.jar
#CLASSPATH=$CLASSPATH:$PROJECT/resources/hibernate.cfg.xml
#CLASSPATH=$CLASSPATH:$PROJECT/resources/log4j.properties
CLASSPATH=$CLASSPATH:$HOME/SUNWappserver/lib/activation.jar

#echo 'Classpath:'
#echo $CLASSPATH

javac -cp $CLASSPATH com/tc/bu/TruConnectBackend.java

exit 0