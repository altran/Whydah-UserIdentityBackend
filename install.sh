#/bin/bash!
#Erik - find one of the more advanced scripts :)

export IAM_MODE=TEST

A=UserIdentityBackend
V=1.0-SNAPSHOT
JARFILE=$A-$V.jar

pkill -f $A

wget --user=altran --password=passHere -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=altran-snapshots&g=net.whydah.sso.service&a=$A&v=$V&p=jar"
java -jar -DIAM_CONFIG=/var/whydah/config/useridentitybackend.TEST.properties $JARFILE &
