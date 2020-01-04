#!/bin/bash

OLD=$(pwd)
BASE=$( cd `dirname $0`/.. ; pwd )
cd $OLD
version=$(cat $BASE/ver/1config.version)


function clean(){
    rm -rf /tmp/cljdoc
    mkdir -p /tmp/cljdoc
    rm -rf /tmp/stage
    mkdir -p /tmp/stage
    cd /tmp/stage
    lein new project
    cd $OLD
}

function build(){
    rm -fr /tmp/stage/project/src/* /tmp/stage/project/doc/*
    cp -r $BASE/1config-core/src/* /tmp/stage/project/src/
    find /tmp/stage/project/src/ ! -name \*.clj -type f -delete
    rm -fr /tmp/stage/project/src/META-INF/
    cp $BASE/README.md $BASE/CHANGELOG.md /tmp/stage/project
    cp -R $BASE/doc /tmp/stage/project/

    cat > /tmp/stage/project/project.clj <<EOF
(defproject project "0.1.0-SNAPSHOT"
  :scm {:name "git" :url "/project"}

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

    cd /tmp/stage/project/
    lein pom
    lein install
    git init
    git add .
    git commit -m 'import'
    cd $OLD

    echo "--------------------------------------------------------------------------"
    echo "------------------------- building documentation  ------------------------"
    echo "--------------------------------------------------------------------------"
    docker run --rm -v "/tmp/stage/project:/project" \
           -v "$HOME/.m2:/root/.m2" -v /tmp/cljdoc:/app/data \
           --entrypoint "clojure" \
           cljdoc/cljdoc -A:cli ingest \
           -p "project/project" -v "0.1.0-SNAPSHOT" \
           --jar /project/target/project-0.1.0-SNAPSHOT.jar \
           --pom /project/pom.xml \
           --git /project --rev "master"
}


function show(){
   echo "--------------------------------------------------------------------------"
   echo "-------------- cljdoc preview: starting server on port 8000 --------------"
   echo "- wait then open http://localhost:8000/d/project/project/0.1.0-SNAPSHOT/ -"
   echo "--------------------------------------------------------------------------"
   docker run --rm -p 8000:8000 -v /tmp/cljdoc:/app/data cljdoc/cljdoc
}


if [ "$1" == "refresh" ] ; then
    build
    echo "--------------------------------------------------------------------------"
    echo "--------------------------- documentation READY --------------------------"
    echo "--------------------------------------------------------------------------"
else
    clean
    build
    show
fi
