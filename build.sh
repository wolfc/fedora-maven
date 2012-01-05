#!/bin/sh
#exec tmp/bin/fmvn -Dmaven.local.depmap.file=fmvn-depmap.xml $*
exec tmp/bin/fmvn -Dmaven.local.depmap.file=/usr/share/maven-fragments/maven $*
