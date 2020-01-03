#!/bin/bash

OLD=$(pwd)
BASE=$( cd `dirname $0`/.. ; pwd )
cd $OLD
proj=com.brunobonacci/oneconfig
version=$(cat $BASE/ver/1config.version)


rm -rf /tmp/stage
mkdir -p /tmp/stage
mkdir -p /tmp/stage/1config/src
tar -xvf ~/.m2/repository/com/brunobonacci/oneconfig/$version/oneconfig-$version.jar -C /tmp/stage/1config/src/

cp $BASE/README.md $BASE/CHANGELOG.md $BASE/1config-core/pom.xml /tmp/stage/1config/
cp -R $BASE/doc /tmp/stage/1config/
BASE=/tmp/stage/1config/

cat > /tmp/stage/1config/project.clj <<EOF
(defproject com.brunobonacci/oneconfig "$version"
  :description "A Clojure library for managing configurations"

  :url "https://github.com/BrunoBonacci/1config"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/1config.git"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.brunobonacci/where "0.5.2"]
                 [com.brunobonacci/safely "0.5.0-alpha7"]
                 [amazonica "0.3.139" :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core     "1.11.513"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.513"]
                 [com.amazonaws/aws-java-sdk-kms      "1.11.513"]
                 [com.amazonaws/aws-java-sdk-sts      "1.11.513"]
                 [com.amazonaws/aws-encryption-sdk-java "1.3.6"]
                 [prismatic/schema "1.1.10"]
                 [cheshire "5.8.1"]]
  )
EOF

rm -rf /tmp/cljdoc
mkdir -p /tmp/cljdoc



echo 'building documentation...'
docker run --rm -v "$BASE:/project" \
       -v "$HOME/.m2:/root/.m2" -v /tmp/cljdoc:/app/data \
       --entrypoint "clojure" \
       cljdoc/cljdoc -A:cli ingest -p "$proj" -v "$version" \
       --git /project

echo "---- cljdoc preview: starting server on port 8000"
echo "wait then open http://localhost:8000/d/$proj/$version/"
docker run --rm -p 8000:8000 -v /tmp/cljdoc:/app/data cljdoc/cljdoc
