#
# Copyright (C) 2025 Objectos Software LTDA.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Objectos Start
#

## Coordinates
GROUP_ID := br.com.objectos
ARTIFACT_ID := objectos.start
VERSION := 0.2.6-SNAPSHOT
MODULE := $(ARTIFACT_ID)

## javac --release option
JAVA_RELEASE := 21

## Maven interop
REMOTE_REPOS := https://repo.maven.apache.org/maven2

## Dependencies
H2 := com.h2database/h2/2.3.232
WAY := $(GROUP_ID)/objectos.way/$(VERSION)
SLF4J_NOP := org.slf4j/slf4j-nop/2.0.17
TESTNG := org.testng/testng/7.11.0

# Delete the default suffixes
.SUFFIXES:

#
# start
#

.PHONY: all
all: test

include make/java-core.mk

#
# start@clean
#

include make/common-clean.mk

#
# start@compile
#

## Compile deps
COMPILE_DEPS := $(WAY)

include make/java-compile.mk

#
# start@jar
#

## where we'll find the classes for our JAR file
JAR_DIRECTORY := $(WORK)/jar

## the java files
JAR_JAVA_FILES := $(shell find ${MAIN} -type f ! -name 'Way.java' -print)

## class files to trigger JAR update
JAR_CLASS_FILES = $($(JAR_JAVA_FILES):$(MAIN)/%.java=$(CLASS_OUTPUT)/%.class)

## META-INF directory
JAR_META_INF := $(CLASS_OUTPUT)/META-INF

## license 'artifact'
JAR_LICENSE := $(JAR_META_INF)/LICENSE

## jar file name
JAR_FILE_NAME := $(ARTIFACT_ID)-$(VERSION).jar

## jar file path
JAR_FILE := $(WORK)/$(JAR_FILE_NAME)

## jar command
JARX := $(JAR)
JARX += --create
JARX += --file $(JAR_FILE)
JARX += --module-version $(VERSION)
JARX += -C $(JAR_DIRECTORY)
JARX += .

## requirements of the JAR_FILE target
JAR_FILE_REQS  = $(COMPILE_MARKER)
JAR_FILE_REQS += $(JAR_LICENSE)
JAR_FILE_REQS += $(JAR_DIRECTORY)

#
# jar targets
#

.PHONY: jar
jar: $(JAR_FILE)

.PHONY: jar-clean
jar-clean:
	rm -rf $(JAR_DIRECTORY) $(JAR_FILE)

$(JAR_DIRECTORY): $(JAR_CLASS_FILES)
	@mkdir --parents $@
	rsync -a --exclude='Way*.class' $(CLASS_OUTPUT)/ $(JAR_DIRECTORY)/

$(JAR_FILE): $(JAR_FILE_REQS)
	$(JARX)

$(JAR_META_INF):
	mkdir --parents $@

$(JAR_LICENSE): LICENSE | $(JAR_META_INF)
	cp LICENSE $(@D)

#
# start@test-repo
#

## Way.java src
WAY_JAVA := $(MAIN)/objectos/start/Way.java

## Y.java src
Y_JAVA := test/objectos/start/Y.java

## test repo
TEST_REPO := $(WORK)/test-repo

## test repo dep (self)
TEST_REPO_DEP_SELF := $(TEST_REPO)/$(call mk-resolved-jar,$(GROUP_ID)/$(ARTIFACT_ID)/$(VERSION))

## test repo dep (way)
TEST_REPO_DEP_WAY := $(TEST_REPO)/$(call mk-resolved-jar,$(WAY))
TEST_REPO_SRC_WAY := $(call gav-to-local,$(WAY))

## test repo requirements
TEST_REPO_REQS := $(WAY_JAVA)
TEST_REPO_REQS += $(TEST_REPO_DEP_SELF)
TEST_REPO_REQS += $(TEST_REPO_DEP_WAY)

## test repo marker
TEST_REPO_MARKER := $(WORK)/test-repo-marker

.PHONY: test-repo
test-repo: $(TEST_REPO_MARKER)

.PHONY: test-repo-clean
test-repo-clean:
	rm -rf $(TEST_REPO) $(TEST_REPO_MARKER)

$(TEST_REPO):
	mkdir --parents $@
	
$(TEST_REPO_DEP_SELF): $(JAR_FILE)
	mkdir --parents $(@D)
	cp $< $@

$(TEST_REPO_DEP_WAY): $(TEST_REPO_SRC_WAY)
	mkdir --parents $(@D)
	cp $< $@
	
$(TEST_REPO_MARKER): $(TEST_REPO_REQS) | $(TEST_REPO)
	sed -i \
		-e '/sed:SHA1_SELF/s/"[^"]*"/"$(shell sha1sum $(TEST_REPO_DEP_SELF) | cut -d' ' -f1 -z)"/' \
		-e '/sed:SHA1_WAY/s/"[^"]*"/"$(shell sha1sum $(TEST_REPO_DEP_WAY) | cut -d' ' -f1 -z)"/' \
		-e '/sed:VERSION/s/"[^"]*"/"$(VERSION)"/' \
		$(WAY_JAVA)	$(Y_JAVA)
	touch $@

#
# start@test-compile
#

## test compile deps
TEST_COMPILE_DEPS := $(TESTNG)

include make/java-test-compile.mk

#
# start@test
#

## test main class
TEST_MAIN := objectos.start.StartTest

## www test runtime dependencies
TEST_RUNTIME_DEPS := $(SLF4J_NOP)

## test --add-modules
TEST_ADD_MODULES := org.testng
TEST_ADD_MODULES += org.slf4j

## test --add-reads
TEST_ADD_READS := objectos.start=org.testng

## additional requirements
TEST_RUNTIME_REQS_MORE := $(TEST_REPO_MARKER)

include make/java-test.mk

#
# start@install
#

include make/java-install.mk

#
# start@way
#

## Way.java dest
WAY_SCRIPT := Way.java

.PHONY: way
way: $(WAY_SCRIPT) $(INSTALL)
	$(JAVA) $< --repo-remote $(LOCAL_REPO)/

$(WAY_SCRIPT): $(WAY_JAVA)
	sed 's/package objectos.start;//' $< > $@
