UserIdentityBackend
===================

Stores UserIdentities and their relation to Roles, Applications and Organizations.
Requires SecurityTokenService if authorization is turned on. 

![Whydha Bird](./whydah-bird.jpg)
Celebrating the Upcoming 2.0 release.
Photo curtesy of Mike's Birds (https://flic.kr/p/o9p1vw) Creative Commons 3 license.

![Architectural Overview](https://raw2.github.com/altran/Whydah-SSOLoginWebApp/master/Whydah%20infrastructure.png)


Installation
============



* create a user for the service
* run ./start_service_SNAPSHOT.sh

* when release is available -> create start_service.sh

```
#!/bin/sh

export IAM_MODE=TEST

A=UserIdentityBackend
V=LATEST
JARFILE=$A-$V.jar

pkill -f $A

wget --user=altran  -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=releases&g=net.whydah.identity&a=$A&v=$V&p=jar"
nohup java -jar -DIAM_CONFIG=useridentitybackend.TEST.properties $JARFILE &

tail -f nohup.out
```

* create useradmin.TEST.properties

```
prop.type=DEV
#prop.type=TEST
ldap.embedded=enabled
ldap.embedded.directory=bootstrapdata/ldap
ldap.embedded.port=10389

ldap.primary.url=ldap://localhost:10389/dc=external,dc=WHYDAH,dc=no
ldap.primary.admin.principal=uid=admin,ou=system
ldap.primary.admin.credentials=secret
ldap.primary.uid.attribute=uid
ldap.primary.username.attribute=initials
#For AD
#ldap.primary.uid.attribute=userprincipalname
#ldap.primary.username.attribute=sAMAccountName

roledb.directory=bootstrapdata/hsqldb
roledb.jdbc.driver=org.hsqldb.jdbc.JDBCDriver
roledb.jdbc.url=jdbc:hsqldb:file:bootstrapdata/hsqldb/roles
roledb.jdbc.user=sa
roledb.jdbc.password=

import.usersource=users.csv
import.rolemappingsource=rolemappings.csv
import.applicationssource=applications.csv
import.organizationssource=organizations.csv

useradmin.requiredrolename=WhydahUserAdmin

adduser.defaultrole.facebook.name=FBData

adduser.defaultrole.name=WhydahDefaultUser
adduser.defaultrole.value=1
adduser.defaultapplication.name=Whydah
adduser.defaultapplication.id=3
adduser.defaultorganization.name=Whydah
adduser.defaultorganization.value=1

#ssologinservice=http://myservice.net/sso/
ssologinservice=http://localhost:9997/sso/
# securitytokenservice=http://myservice.net/tokenservice/
securitytokenservice=http://localhost:9998/tokenservice/
# myuri=http://myservice.net/uib
myuri=http://localhost:9995/
service.port=9995


lucene.directory=bootstrapdata/lucene


gmail.username=mysystem@gmail.com
gmail.password=pw
```

Typical apache setup
====================

```
<VirtualHost *:80>
        ServerName myserver.net
        ServerAlias myserver
        ProxyRequests Off
        <Proxy *>
                Order deny,allow
                Allow from all
        </Proxy>
        ProxyPreserveHost on
                ProxyPass /sso http://localhost:9997/sso
                ProxyPass /uib http://localhost:9995/uib
                ProxyPass /tokenservice http://localhost:9998/tokenservice
                ProxyPass /useradmin http://localhost:9996/useradmin
                ProxyPass /test http://localhost:9990/test/
</VirtualHost>
```


TODO:
Better configuration of temporary lucene/hsqldb-paths. They are stored in different folders for different tests & usage modes. 



Server overview
===============


Development
===========

http://myApp.net - App using Whydah
http://myserver.net - Whydah SSO

Webproxy CNAME	 					CNAME/direct	
http://myserver.net/huntevaluationbackend/		server-x:8080/huntevaluationbackend
http://myserver.net					http://localhost:8983/solr	
http://myserver.net/sso					http://localhost:9997/sso	
http://myserver/tokenservice				http://localhost:9998/tokenservice/	
http://myserver.net/uib					http://localhost:9995/uib/	
http://myserver.cloudapp.net/useradmin			http://localhost:9996/useradmin/ 		 loop with ssologinservice.


Test/Production
===============
http://myApp.net - App using Whydah
http://myserver.net - Whydah SSO


Webproxy CNAME	 					CNAME/direct	
http://myserver.net/huntevaluationbackend/		server-x:8080/huntevaluationbackend
http://myserver.net					http://server-a:8983/solr	
http://myserver.net/sso					http://server-b:9997/sso	
http://myserver/tokenservice				http://server-c:9998/tokenservice/	
http://myserver.net/uib					http://server-d:9995/uib/	
http://myserver.cloudapp.net/useradmin			http://server-e:9996/useradmin/ 		 loop with ssologinservice.


Development Infrastructure
==========================

Webproxy CNAME	 		CNAME/direct	 	 		Comment
http://mvnrepo.cantara.no	http://nexus.cantara.no:8081		Ask Erik if it doesn't work.
http://ci.cantara.no		http://217.77.36.146:8080/jenkins/		 

