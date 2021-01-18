#!/bin/bash
git remote add upstream https://github.com/altran/Whydah-UserIdentityBackend.git
git fetch upstream
git merge upstream/master
git push
