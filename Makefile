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
WAY := br.com.objectos/objectos.way/$(VERSION)
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
