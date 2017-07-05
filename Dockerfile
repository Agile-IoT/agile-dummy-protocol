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
FROM agileiot/raspberry-pi3-zulujdk:8-jdk-maven

# Add packages
RUN apt-get update && apt-get install --no-install-recommends -y \
    build-essential \
    git\
    ca-certificates \
    apt \
    software-properties-common \
    unzip \
    cpp \
    binutils \
    maven \
    gettext \
    libc6-dev \
    make \
    cmake \
    cmake-data \
    pkg-config \
    clang \
    gcc-4.9 \
    g++-4.9 \
    qdbus \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# resin-sync will always sync to /usr/src/app, so code needs to be here.
WORKDIR /usr/src/app
ENV APATH /usr/src/app

COPY scripts scripts

RUN CC=clang CXX=clang++ CMAKE_C_COMPILER=clang CMAKE_CXX_COMPILER=clang++ \
scripts/install-dbus-java.sh $APATH/deps

RUN CC=clang CXX=clang++ CMAKE_C_COMPILER=clang CMAKE_CXX_COMPILER=clang++ \
scripts/install-agile-interfaces.sh $APATH/deps

# copy directories into WORKDIR
COPY iot.agile.protocol.DummyProtocol iot.agile.protocol.DummyProtocol

RUN cd iot.agile.protocol.DummyProtocol && mvn package

FROM agileiot/raspberry-pi3-zulujdk:8-jre
WORKDIR /usr/src/app
ENV APATH /usr/src/app

COPY --from=0 $APATH/scripts scripts
COPY --from=0 $APATH/deps deps
COPY --from=0 $APATH/iot.agile.protocol.DummyProtocol/target/agile-dummy-protocol-1.0.0-jar-with-dependencies.jar iot.agile.protocol.DummyProtocol/target/agile-dummy-protocol-1.0.0-jar-with-dependencies.jar

CMD [ "bash", "/usr/src/app/scripts/start.sh" ]
