define helpdoc
# +----------------------------------------------------------------------------+
# |                                                                            |
# |                       ----==| 1 C O N F I G |==----                        |
# |                                                                            |
# +----------------------------------------------------------------------------+
#
# It requires GNU make 3.82+
#
# Install with:
#   brew install make
#   echo "alias make=gmake" >> ~/.profile
#
# Run with:
#   $ make <target> ... <target>
#
# Examples:
#   $ make all
#   $ make clean build test package
#
# Available targets:
#
# - clean:   removes compilation outputs
# - build:   compiles and run unit tests for each modules
# - test:    runs integration tests for cli
# - package: creates the distribution packages
# - deploy:  uploads the library into clojars
# - all:     same as `make clean build test`
# - release: same as `clean update-version build test package`
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
# Preparing a release
#
release: clean update-version build test package


#
# Build
#
build: build-core build-cli build-ui
- @printf "#\n# Building 1config Completed!\n#\n"


#
# Build Core
#
core_src = $(shell find 1config-core/src 1config-core/resources 1config-shared/src -type f)
build-core: 1config-core/target/oneconfig*.jar
1config-core/target/oneconfig*.jar: $(core_src)
- @printf "#\n# Building 1config-core\n#\n"
- (cd 1config-core; lein do check, midje, install)


#
# Build CLI
#
cli_src = $(shell find 1config-cli/src 1config-cli/resources 1config-shared/src -type f)
build-cli: build-core 1config-cli/target/1cfg
1config-cli/target/1cfg: $(cli_src)
- @printf "#\n# Building 1config-cli\n#\n"
- (cd 1config-cli; lein do check, bin)



#
# Build UI
#
ui_src = $(shell find 1config-ui/src 1config-ui/resources 1config-shared/src -type f)
build-ui: build-core 1config-ui/target/1cfg-ui-beta
1config-ui/target/1cfg-ui-beta: $(ui_src)
- @printf "#\n# Building 1config-ui\n#\n"
- (cd 1config-ui; lein do check, bin)


#
# run the end-2-end integration test
#
test: build-core
- AWS_PROFILE=${AWS_PROFILE} ./test/bin/end-2-end-test.sh uberjar

#
# Package artifacts for homebrew and git release
#
PACKAGE := 1cfg
PKDIR   := /tmp/$(PACKAGE)
.PHONY: package
package:
- rm -fr $(PKDIR)
- mkdir -p $(PKDIR)/hb/bin
- @printf "\n(-) preparing copying artifact\n"
- cp 1config-cli/target/1cfg $(PKDIR)/1cfg-Darwin
- cp 1config-cli/target/1cfg-Linux $(PKDIR)/1cfg-Linux
- cp 1config-cli/target/1cfgx $(PKDIR)
- cp 1config-ui/target/1cfg-ui-beta $(PKDIR)
- @printf "\n(-) preparing Homebrew package for Linux\n"
- cp 1config-cli/bin/1cfg $(PKDIR)/hb/bin
- cp 1config-cli/target/1cfg $(PKDIR)/hb/bin/1cfg.jar
- cp 1config-ui/bin/1cfg-ui-beta $(PKDIR)/hb/bin
- cp 1config-ui/target/1cfg-ui-beta $(PKDIR)/hb/bin/1cfg-ui-beta.jar
- tar -zcvf $(PKDIR)/$(PACKAGE)-homebrew.tar.gz -C /tmp/$(PACKAGE)/hb .
- rm -fr $(PKDIR)/hb
- @printf "\n(-) writing checksums\n"
- shasum -a 256 $(PKDIR)/* > $(PKDIR)/$(PACKAGE).sha
- @printf "\n(-) packages ready in /tmp/$(PACKAGE)\n"
- @printf "#------------------------------------------------------------------#\n"
- ls -halp $(PKDIR)
- @printf "#------------------------------------------------------------------#\n"
- cat $(PKDIR)/1cfg.sha
- @printf "#------------------------------------------------------------------#\n"


#
# Update version in doc
#
.PHONY: update-version
update-version:
- LAST=$$(curl -s https://api.github.com/repos/BrunoBonacci/1config/releases | jq -r '.[0].tag_name') ; \
  CURR=$$(cat ver/1config.version) ; \
  echo Updating $${LAST} to $${CURR} ; \
  find . -name \*.md | grep -v CHANGELOG.md | xargs -I{} grep -l "$$LAST" {} | xargs -I{} sed -i '' s/$$LAST/$$CURR/g {}


#
# Clean target directories
#
.PHONY: clean
clean:
- @printf "#\n# Cleaning \n#\n"
- (cd 1config-core; rm -fr target)
- (cd 1config-cli;  rm -fr target)
- (cd 1config-ui;   rm -fr target)
