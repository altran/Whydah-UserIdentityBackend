UserIdentityBackend
===================

Stores UserIdentities and their relation to Roles, Applications and Organizations.
Requires SecurityTokenService if authorization is turned on. 

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

