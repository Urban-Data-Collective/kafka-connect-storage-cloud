#!/usr/bin/env bash

CONFLUENT_KAFKA_VERSION="v7.2.0-108-ccs"
CONFLUENT_COMMON_VERSION="v7.2.0-949"
CONFLUENT_REST_UTILS_VERSION="v7.2.0-987"
CONFLUENT_SCHEMA_REGISTRY_VERSION="v7.2.0-1021"
CONFLUENT_KAFKA_CONNECT_STORAGE_COMMON_VERSION="v11.0.4"

echo "------ INSTALLING UPSTREAM DEPENDENCIES FOR kafka-connect-storage-cloud ------"

# [confluentinc/kafka](https://github.com/confluentinc/kafka)
git clone https://github.com/confluentinc/kafka.git && cd kafka && git checkout tags/$CONFLUENT_KAFKA_VERSION -b $CONFLUENT_KAFKA_VERSION && ./gradlewAll -x test publishToMavenLocal

# [confluentinc/common](https://github.com/confluentinc/common)
git clone https://github.com/confluentinc/common.git && cd common && git checkout tags/$CONFLUENT_COMMON_VERSION -b $CONFLUENT_COMMON_VERSION && mvn clean install -DskipTests
cd -

# [confluentinc/rest-utils](https://github.com/confluentinc/rest-utils)
git clone https://github.com/confluentinc/rest-utils.git && cd rest-utils && git checkout tags/$CONFLUENT_REST_UTILS_VERSION -b $CONFLUENT_REST_UTILS_VERSION && mvn clean install -DskipTests
cd -

# [confluentinc/schema-registry](https://github.com/confluentinc/schema-registry)
git clone https://github.com/confluentinc/schema-registry.git && cd schema-registry && git checkout tags/$CONFLUENT_SCHEMA_REGISTRY_VERSION -b $CONFLUENT_SCHEMA_REGISTRY_VERSION && mvn clean install -DskipTests
cd -

# [confluentinc/kafka-connect-storage-common](https://github.com/confluentinc/kafka-connect-storage-common)
# The kafka-connect-storage-hive dependency was causing problems because of a broken mirror,
# the '-pl \!:kafka-connect-storage-hive' part tells maven not to build this module
# There don't seem to be any runtime ramifications for this
git clone https://github.com/confluentinc/kafka-connect-storage-common.git && cd kafka-connect-storage-common && git checkout tags/$CONFLUENT_KAFKA_CONNECT_STORAGE_COMMON_VERSION -b $CONFLUENT_KAFKA_CONNECT_STORAGE_COMMON_VERSION && mvn -U clean install -DskipTests -pl \!:kafka-connect-storage-hive
cd -

echo "------ UPSTREAM DEPENDENCIES INSTALLED: CHECK FOR ERRORS ABOVE ------"