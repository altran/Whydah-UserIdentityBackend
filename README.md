UserIdentityBackend
===================

Stores UserIdentities and their relation to Roles, Applications and Organizations.
Requires SecurityTokenService if authorization is turned on. 

TODO:
Better configuration of temporary lucene/hsqldb-paths. They are stored in different folders for different tests & usage modes. 