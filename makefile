define helpdoc
# +----------------------------------------------------------------------------+
# |                                                                            |
# |                       ----==| 1 C O N F I G |==----                        |
# |                                                                            |
# +----------------------------------------------------------------------------+
#
# Requires GNU make 3.82+
#
# Install with:
#   brew install make
#   echo "alias make=gmake" > ~/.profile
#
# Available targets:
#
# - clean:   removes compilation outputs
# - build:   compiles and run unit tests for each modules
# - test:    runs integration tests for cli
# - package: creates the distribution packages
# - deploy:  uploads the library into clojars
# - all:     same as `make clean build test package`
#
endef

#
# Recipe prefix requires GNU make 3.82+
#
.RECIPEPREFIX := -


#
# AWS profile name to use to run the end-2-end integration tests
#
AWS_PROFILE=testaccount


#
# Help
#
.PHONY: help
export helpdoc
help:
- echo "$$helpdoc"


#
# Preparing all
#
all: clean build test package

#
# Checking java version
#
check-ver:
ifneq ($(shell java -version 2>&1 | grep 1.8.0 >/dev/null; printf $$?),0)
- echo "please use JDK 1.8"
- exit 1
endif


#
# Build
#
build: check-ver build-core build-cli build-ui
- @printf "#\n# Building 1config Completed!\n#\n"


#
# Build Core
#
build-core: check-ver 1config-core/target/oneconfig*.jar
1config-core/target/oneconfig*.jar:
- @printf "#\n# Building 1config-core\n#\n"
- (cd 1config-core; lein do check, midje, install)


#
# Build CLI
#
build-cli: check-ver build-core 1config-cli/target/1cfg
1config-cli/target/1cfg:
- @printf "#\n# Building 1config-cli\n#\n"
- (cd 1config-cli; lein do check, bin)



#
# Build UI
#
build-ui: check-ver build-core 1config-ui/target/1cfg-ui
1config-ui/target/1cfg-ui:
- @printf "#\n# Building 1config-ui\n#\n"
- (cd 1config-ui; lein do check, bin)


#
# run the end-2-end integration test
#
test: build-core 1config-cli/target/1cfg
- AWS_PROFILE=${AWS_PROFILE} ./test/bin/end-2-end-test.sh


package: build
- 1config-cli/bin/package.sh


.PHONY: clean
clean:
- @printf "#\n# Cleaning \n#\n"
- (cd 1config-core; rm -fr target)
- (cd 1config-cli;  rm -fr target)
- (cd 1config-ui;   rm -fr target)
