#!/bin/sh
#exec tmp/bin/fmvn -Dfre.depmap.file=fmvn-depmap.xml $*
exec tmp/bin/fmvn -Dfre.depmap.file=/usr/share/maven-fragments/maven $*
