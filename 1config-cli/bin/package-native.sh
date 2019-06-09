#!/bin/bash

export PACKAGE=1cfg
export BASE=$(dirname $0)/..

echo '(-) '"creating /tmp/$PACKAGE"
rm -fr  /tmp/$PACKAGE
mkdir -p /tmp/$PACKAGE/hb/bin

echo '(-) '"preparing copying artifact"
chmod +x $BASE/target/$PACKAGE
cp $BASE/target/$PACKAGE /tmp/$PACKAGE/

echo '(-) '"preparing Homebrew package for Linux"
mkdir -p  /tmp/$PACKAGE/hb/bin
cp $BASE/bin/1cfg /tmp/$PACKAGE/hb/bin/
cp $BASE/target/$PACKAGE /tmp/$PACKAGE/hb/bin/$PACKAGE.jar
tar -zcvf /tmp/$PACKAGE/$PACKAGE-homebrew.tar.gz -C /tmp/$PACKAGE/hb .
rm    -fr /tmp/$PACKAGE/hb

echo '(-) '"writing checksums"
shasum -a 256 /tmp/$PACKAGE/* > /tmp/$PACKAGE/$PACKAGE.sha

echo '(-) '"packages ready in /tmp/$PACKAGE"
ls -halp /tmp/$PACKAGE
