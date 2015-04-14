#!/bin/sh

#  If IAM_MODE not set, use PROD
if [ -z "$IAM_MODE" ]
  IAM_MODE=PROD
fi

# If IAM_CONFOG not set, use PROD properties from local directory
if [ -z "$IAM_CONFIG" ]
  IAM_CONFIG=useridentitybackend.PROD.properties
fi

# If Version is from source, find the artifact
if [ $Version = "FROM_SOURCE" ]; then 
    # Find the bult artifact
    Version=$(find target/* -name *.jar | grep SNAPSHOT | grep -v original | gre
p -v lib)
else
    Version=UserIdentityBackend.jar
fi


nohup /usr/bin/java -DIAM_MODE=$IAM_MODE -DIAM_CONFIG=$IAM_CONFIG -jar  $Version

