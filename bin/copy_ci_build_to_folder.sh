#! /bin/bash

DIR_TO_MAKE=$1
ZIP_FILENAME_PRODUCED_ON_CI="confluentinc-kafka-connect-s3-10.1.0-SNAPSHOT.zip"
ZIP_FILENAME_WE_WANT_IN_S3="confluentinc-kafka-connect-s3-UdxStreamPartitioner.zip"

mkdir $DIR_TO_MAKE && \
cp kafka-connect-s3/target/components/packages/$ZIP_FILENAME_PRODUCED_ON_CI $DIR_TO_MAKE/ &&  \
mv $DIR_TO_MAKE/$ZIP_FILENAME_PRODUCED_ON_CI $DIR_TO_MAKE/$ZIP_FILENAME_WE_WANT_IN_S3