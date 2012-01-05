#! /usr/bin/ksh
. /apps/webadmin/.profile

###############################################################################
##
##                          TruConnect Backend Process
##
##    -Overview
##    -This script is designed to run TruConnect's backend processes
##    
##    -ChangeLog
##    -6/6/2011-dta- Added this script
##    -9/5/2011-Jalcanta- Implement anti-pileup routine
##
##
###############################################################################
#
# Error messaging function
error_message(){
if [ $# -lt 1 ] || [ $# -gt 2 ]; then
   echo "No argument sent.\n\nUsage:\n\terror_message \[message_text\|filename\] \{F\|W default Fatal\}">/tmp/error_message.$$
elif [ "x$1" = "x" ]; then
   echo "Null argument 1. \n\nUsage:\n\terror_message \[message_text\|filename\] \{F\|W default Fatal\}">/tmp/error_message.$$
else
   if [ ! -f $1 ]; then
      echo $1>/tmp/error_message.$$
   else
      if [ -s $1 ]; then
         mv $1 /tmp/error_message.$$
      else
         echo "File is empty.\n\nUsage:\n\terror_message \[message_text\|filename\]">/tmp/error_message.$$
      fi
   fi
fi
mailx -s "$0 Error at $(hostname)" -r tscdba@telscape.net -c omsreports2@telscape.net ossops@telscape.net < /tmp/error_message.$$

rm /tmp/error_message.$$
if [ "x$2" = "x" ] || [ "x$2" = "xF" ]; then
   exit 1
fi
}

# main
# Do not let this job get stuck for more than 24 hrs
find /tmp -name TrueConnectBackEnd_Proc.flg -ctime +0 -print 1>$$.tmp 2>>/dev/null

if [ -s $$.tmp ]; then
   error_message "UNIX job $0 in $(whoami)@$(hostname) has been waiting for another processes for more than 24 hrs...\n" W
fi

rm $$.tmp

# Avoid pile ups
if [ -f /tmp/TrueConnectBackEnd_Proc.flg ]; then
   echo "Another instance of program $0 is still running! Aborting execution ..."
   exit 1
else
   touch /tmp/TrueConnectBackEnd_Proc.flg
   if [ $? -ne 0 ]; then
      error_message "Unable to create flag file /tmp/TrueConnectBackEnd_Proc.flg"
   fi
fi

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
CLASSPATH=$CLASSPATH:$PROJECT/resources/hibernate.cfg.xml
CLASSPATH=$CLASSPATH:$PROJECT/resources/log4j.properties
CLASSPATH=$CLASSPATH:$HOME/SUNWappserver/lib/activation.jar

#echo 'Classpath:'
#echo $CLASSPATH
logfile=/tmp/TruConnectBackend_Proc.log
errfile=/tmp/TruConnectBackend_Proc.err

echo "Starting @ $(date)" >> ${logfile}

java -cp $CLASSPATH com.tc.bu.TruConnectBackend 1>>${logfile} 2>>${errfile}
if [ $? -ne 0 ]; then
   error_message ${errfile} W
fi

echo "Ending @ $(date)" >> ${logfile}

#sleep 30

# Keep the last 800 lines of logging purposes
for f in ${logfile} ${errfile}
do
   tail -800 ${f} > /tmp/$$.tmp && mv /tmp/$$.tmp ${f}
done


# Remove pile-up flag

if [ -f /tmp/TrueConnectBackEnd_Proc.flg ]; then
   rm /tmp/TrueConnectBackEnd_Proc.flg
   if [ $? -ne 0 ]; then
      error_message "Unable to remove flag file /tmp/TrueConnectBackEnd_Proc.flg"
   fi
fi

exit 0

