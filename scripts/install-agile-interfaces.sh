#!/bin/sh
#-------------------------------------------------------------------------------
# Copyright (C) 2017 Create-Net / FBK.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     Create-Net / FBK - initial API and implementation
#-------------------------------------------------------------------------------

set -e

CURRDIR=`pwd`
DEPS=${1:-$CURRDIR/deps}
BUILD=$DEPS/build

#clean up first
if [ -e "$DEPS/agile-interfaces" ] ; then
  rm -r $DEPS/agile-interfaces
  rm $DEPS/agile-interfaces*
  # drop from local repo eventually
  if [ -e ~/.m2/repository/org/eclipse/agail/agile-interfaces ] ; then
    rm -r  ~/.m2/repository/org/eclipse/agail/agile-interfaces
  fi
fi

if [ ! -e "$BUILD" ] ; then
  mkdir -p $BUILD
fi

if [ ! -e "$BUILD/agile-api-spec" ] ; then
  cd $BUILD
  git clone https://github.com/Agile-IoT/agile-api-spec.git
  cd agile-api-spec
  git checkout v0.1.7
  cd ..
else
  cd $BUILD/agile-api-spec
  git checkout v0.1.7
#  git pull
  cd ..
fi

cd agile-api-spec/agile-dbus-java-interface
./scripts/install-dependencies.sh
mvn package
cp target/agile-interfaces-1.0.jar $DEPS
cd ..

cd $DEPS

mvn install:install-file -Dfile=$DEPS/agile-interfaces-1.0.jar \
                         -DgroupId=org.eclipse.agail \
                         -DartifactId=agile-interfaces \
                         -Dversion=1.0 \
                         -Dpackaging=jar \
                         -DgeneratePom=true \
                         -DlocalRepositoryPath=$DEPS
