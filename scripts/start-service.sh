#!/bin/sh

/usr/bin/java  -DIAM_MODE=DEV -DCONSTRETTO_TAGS=DEV -Dlogback.configurationFile=/home/UserIdentityBackend/logback.xml -jar /home/UserIdentityBackend/UserIdentityBackend.jar
