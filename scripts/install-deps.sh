#!/bin/sh

apt install --no-install-recommends -y gettext git cmake

CURRDIR=`pwd`
DEPS=${1:-$CURRDIR/deps}

if [ -e "$DEPS" ]; then
  rm $DEPS -rf
fi

cd $CURRDIR

sh ./scripts/install-dbus-java.sh $DEPS
sh ./scripts/install-agile-interfaces.sh $DEPS

cd iot.agile.protocol.DummyProtocol
mvn clean install -U
