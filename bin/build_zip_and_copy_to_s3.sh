#! /bin/bash

ENV=$1
OUTPUT_ZIP_NAME="confluentinc-kafka-connect-s3-UdxStreamPartitioner.zip"
S3_PATH="s3://udx-$ENV-msk-connectors/$OUTPUT_ZIP_NAME"

mvn clean package -DskipTests && \
echo "Copying ZIP archived build to S3 to path: $S3_PATH" && \
aws s3 cp kafka-connect-s3/target/components/packages/confluentinc-kafka-connect-s3-10.1.0-SNAPSHOT.zip s3://udx-$ENV-msk-connectors/$OUTPUT_ZIP_NAME --profile udx-$ENV