#!/usr/bin/env bash

echo "------ INSTALLING UPSTREAM DEPENDENCIES FOR kafka-connect-storage-cloud ------"

# [confluentinc/kafka](https://github.com/confluentinc/kafka)
git clone https://github.com/confluentinc/kafka.git && cd kafka && git checkout tags/v7.2.0-108-ccs -b v7.2.0-108-ccs && ./gradlewAll -x test publishToMavenLocal

# [confluentinc/common](https://github.com/confluentinc/common)
git clone https://github.com/confluentinc/common.git && cd common && git checkout tags/v7.2.0-949 -b v7.2.0-949 && mvn clean install -DskipTests
cd -

# [confluentinc/rest-utils](https://github.com/confluentinc/rest-utils)
git clone https://github.com/confluentinc/rest-utils.git && cd rest-utils && git checkout tags/v7.2.0-987 -b v7.2.0-987 && mvn clean install -DskipTests
cd -

# [confluentinc/schema-registry](https://github.com/confluentinc/schema-registry)
git clone https://github.com/confluentinc/schema-registry.git && cd schema-registry && git checkout tags/v7.2.0-1021 -b v7.2.0-1021 && mvn clean install -DskipTests
cd -

# [confluentinc/kafka-connect-storage-common](https://github.com/confluentinc/kafka-connect-storage-common)
git clone https://github.com/confluentinc/kafka-connect-storage-common.git && cd kafka-connect-storage-common && git checkout tags/v11.0.4 -b 11.0.4 && mvn -U clean install -DskipTests -pl \!:kafka-connect-storage-hive
cd -

echo "------ UPSTREAM DEPENDENCIES INSTALLED: CHECK FOR ERRORS ABOVE ------"