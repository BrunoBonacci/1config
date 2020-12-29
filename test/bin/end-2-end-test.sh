#!/bin/bash
#
# from: 1config-cli
# $ ../test/bin/end-2-end-test.sh                  # to test the binary same as `1cfg`
# $ ../test/bin/end-2-end-test.sh 1cfg             # to test the binary
# $ ../test/bin/end-2-end-test.sh 1cfgx            # to test the executable jar
# $ ../test/bin/end-2-end-test.sh graalvm-config   # to run the test and generate the config

export CURR=$(pwd)


if [ "$1" == "graalvm-config" ] ; then

   export CFG1="java -agentlib:native-image-agent=config-merge-dir=$(dirname $0)/../../1config-cli/graalvm-config/ -jar $(dirname $0)/../../1config-cli/target/oneconfig-cli-*-standalone.jar"

   if [ ! -f "$(dirname $0)/../../1config-cli/target/oneconfig-cli-*-standalone.jar" ] ; then
       echo "ERROR: uberjar not present."
       echo "  please run:"
       echo "    cd 1config-cli"
       echo "    lein do clean, uberjar"
       exit 1
   fi

elif [ "$1" == "1cfg" -o "$1" == "" ] ; then

    export CFG1=$(dirname $0)/../../1config-cli/target/1cfg
    if [ ! -f $CFG1 ] ; then
        echo "ERROR: 1cfg binary executable not present."
        echo "  please run:"
        echo "    cd 1config-cli"
        echo "    lein do clean, uberjar, native-config, native"
        exit 1
    fi

elif [ "$1" == "1cfgx" ] ; then

    export CFG1=$(dirname $0)/../../1config-cli/target/1cfgx
    if [ ! -f $CFG1 ] ; then
        echo "ERROR: 1cfg binary executable not present."
        echo "  please run:"
        echo "    cd 1config-cli"
        echo "    lein do clean, bin"
        exit 1
    fi

else

    echo "unknown executiable: $1"
    exit 1

fi


# defining test table
export ONECONFIG_DYNAMO_TABLE=1ConfigTest2

if [ "$(aws dynamodb list-tables --output=text | grep -q $ONECONFIG_DYNAMO_TABLE || echo 1)" == "1" ] ; then
    echo "(*) To initialise a given backend"
    $CFG1 INIT -b dynamo
    echo "waiting for table to be available..."
    sleep 10
fi

## test help and various listings
$CFG1 -h
$CFG1 list
$CFG1 list -X
$CFG1 list -C
$CFG1 list-keys


# setup user-profiles for testing
rm -fr /tmp/end2end-test/
mkdir /tmp/end2end-test/
export ONECONFIG_HOME=/tmp/end2end-test/
export USER_PROFILE=$ONECONFIG_HOME/user-profiles.edn

if [ ! -f "$USER_PROFILE" ] ; then
    echo "User Profile file doesn't exists. creating one."
    cat > "$USER_PROFILE" <<\EOF
;; restriction file used for 1config automated test
{:restrictions
 [;; guard   -> restriction :message "Display message"
  [:account :matches? ".*"] :->  [:env :is-not? "restricted"]
  :message "Invalid env name. RESTRICTION TEST"
 ]
}
EOF
fi


# fail on error
set -xe


echo '(*) List KMS encryption keys managed by 1Config'
$CFG1 LIST-KEYS

if [ "$($CFG1 LIST-KEYS | grep -q 'test/service1' || echo 1)" = "1" ] ; then
  echo '(*) Create a master encryption key, the key name must be the same'
  echo '    and the configuration key to be used automatically.'
  $CFG1 CREATE-KEY -m 'test/service1'
  $CFG1 LIST-KEYS | grep 'test/service1' || exit 1
fi



echo "(*) To set the configuration value of a service called 'service1' use:"
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '1.6.0' -t txt 'value-1'

echo "(*) To read last configuration value for a service called 'service1'"
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/service1' -v '1.6.0')" = "value-1" ] || exit 2

echo '(*) To list configuration with optional filters and ordering'
$CFG1 SET -b dynamo -e int1 -k 'test/service1' -v '1.6.0' -t txt 'int-value-1'
$CFG1 LIST -b dynamo -e test -k ser -v 1. -o env,key | grep int1 && exit 3

echo "(*) environment are islated"
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/service1' -v '1.6.0')" = "value-1" ] || exit 4
[ "$($CFG1 GET -b dynamo -e int1  -k 'test/service1' -v '1.6.0')" = "int-value-1" ] || exit 5


echo "(*) configuration lookup are based on sem-ver ordering"
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '1.7.3' -t txt 'value-173'
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '1.12.0' -t txt 'value-1120'
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/service1' -v '1.7.0')" = "value-1" ] || exit 6
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/service1' -v '1.8.0')" = "value-173" ] || exit 7
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/service1' -v '2.8.0')" = "value-1120" ] || exit 8
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/service1')" = "value-1120" ] || exit 9


echo "(*) can use specific encryption key"
$CFG1 SET -b dynamo -e test1 -k 'test/super-service' -v '1.1.3' -t txt 'super-113' -m 'test/service1'
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/super-service' -v '2.8.0')" = 'super-113' ] || exit 10


echo "(*) To read a specific changeset for a service called 'service1'"
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '1.6.0' -t txt 'value-a'
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '1.6.0' -t txt 'value-b'
output=$($CFG1 GET -b dynamo -e test1 -k 'test/service1' -v '1.6.0' --with-meta)
changenum=$(echo "$output" | grep -q value-b && echo $output | grep -Eo ':change-num ([0-9]+)' | grep -Eo '[0-9+]+')
[ "$changenum" == '' ] && exit 11
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '1.6.0' -t txt 'value-c'
[ "$($CFG1 GET -b dynamo -e test1 -k 'test/service1' -v '1.6.0' -c $changenum)" = "value-b" ] || exit 12



echo "(*) verify if the value is a valid value for the given content-type"

echo "(*) testing json"
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t json '{"foo":1}'   2>/dev/null || (echo "good json not accepted" ; exit 31)
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t json '{"foo:1}'    2>/dev/null && (echo "bad  json accepted" ; exit 32)

echo "(*) testing edn"
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t edn '{:foo 1}'     2>/dev/null || (echo "good edn not accepted" ; exit 35)
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t edn '{foo: 1}'     2>/dev/null && (echo "bad  edn accepted" ; exit 36)

echo "(*) testing properties"
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t props 'foo=1'      2>/dev/null || (echo "good edn not accepted" ; exit 38)
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t props 'foo=\uHHHH' 2>/dev/null && (echo "bad  edn accepted" ; exit 39)

echo "(*) testing yaml"
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t yaml '{foo: bar baz}'  2>/dev/null || (echo "good yaml not accepted" ; exit 40)
$CFG1 SET -b dynamo -e test1 -k 'test/service1' -v '0.0.1' -t yaml '{foo: bar: baz}' 2>/dev/null && (echo "bad  yaml accepted" ; exit 41)


echo "(*) verify if the restrictions are taken into account"
$CFG1 -k mysql-database -e restricted -v '1.1.1' -t txt SET 'impossible' && exit 50

echo "(*) testing diffs"
$CFG1 SET -b dynamo -e diff1 -k 'test/service1' -v '1.2.0' -t txt 'Hello World, This is cool!'
$CFG1 SET -b dynamo -e diff1 -k 'test/service1' -v '1.5.0' -t txt 'Hello 1Config, This is cool!'
[ "$($CFG1 DIFF -b dynamo -e diff1 -k 'test/service1' -v '1.2.0' -v '1.2.0' --DC)" == "No differences found." ] || exit 60
[ "$($CFG1 DIFF -b dynamo -e diff1 -k 'test/service1' -v '1.2.0' -v '1.5.0' --DC | tr -d '\33')" == '[0mHello [41mWorld[0m[42m1Config[0m[0m, This is cool![0m' ] || exit 61

echo "ALL OK."
cd $CURR
