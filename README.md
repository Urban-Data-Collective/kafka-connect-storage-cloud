# Kafka Connect Connector for S3 (with UdxStreamPartitioner)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fconfluentinc%2Fkafka-connect-storage-cloud.svg?type=shield)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fconfluentinc%2Fkafka-connect-storage-cloud?ref=badge_shield)


*kafka-connect-storage-cloud* is the repository for Confluent's [Kafka Connectors](http://kafka.apache.org/documentation.html#connect)
designed to be used to copy data from Kafka into Amazon S3. 

## Kafka Connect Sink Connector for Amazon Simple Storage Service (S3)

Documentation for this connector can be found [here](http://docs.confluent.io/current/connect/connect-storage-cloud/kafka-connect-s3/docs/index.html).

Blogpost for this connector can be found [here](https://www.confluent.io/blog/apache-kafka-to-amazon-s3-exactly-once).

# Development

NOTE: this is an exact fork of Kafka Connect Connector for S3 v10.0.8:

```shell
 git checkout tags/v10.0.8 -b v10.0.8 # a branch was made from this exact commit
```

## IDE setup

I've only tried using IntelliJ Community edition for development so far. I'm sure VSCode would work fine too, but I couldn't bear dealing with random Java extensions.

If using IntelliJ in combination with sdkman, remember to set the IntelliJ Maven path to the sdkman maven path!
Otherwise, the various `mvn installs`s on the dependent packages won't be linked properly and there will be PAIN.

Find this setting in IntelliJ 2022.1.2 Community edition in the preferences:

- Preferences > Build, Execution, Deployment > Build Tools > Maven
    - option: "Maven home path"

Finally, **make sure the indent level in IntelliJ is set to 2 spaces. Checkstyle (which seems to be a linter) will
complain otherwise**

## Installing Java 11 and maven with sdkman

Why Java 11? [These Confluent docs](https://docs.confluent.io/platform/current/installation/versions-interoperability.html#java) suggest that Java 11
is used in the Confluent platform - I've inferred that Java 11 is probably used to write the Confluent connectors. 

```sh
$ curl -s "https://get.sdkman.io" | bash
$ sdk install java 11.0.2-open
$ sdk install maven
```

## Installing upstream dependencies

For various reasons that are particular to Confluent Inc's FOSS workflow, 
a few of the dependencies of the kafka-connect-storage-cloud module
must be downloaded, built from source and then added to the local Maven repository. These dependencies are:

- [confluentinc/kafka](https://github.com/confluentinc/kafka)
- [confluentinc/common](https://github.com/confluentinc/common)
- [confluentinc/rest-utils](https://github.com/confluentinc/rest-utils)
- [confluentinc/schema-registry](https://github.com/confluentinc/schema-registry)
- [confluentinc/kafka-connect-storage-common](https://github.com/confluentinc/kafka-connect-storage-common)

Run this script to install the dependencies (it will take a while...)
```
./install_upstream_dependencies.sh
```

The dependencies in this script are exact. They were derived from the various pom.xml files that are included in each project.
The script should continue to work as long as the various tags for the various repos still exist on GitHub.

Then link all of these dependencies to the kafka-connect-s3 module with:

```
mvn clean install -DskipTests
```

## Running tests

Thusfar, I've only used IntelliJ for development. You may run unit tests via IntelliJ in the usual way. 

To run unit tests for the custom partitioner with this command:

```shell
mvn -Dtest=UdxStreamPartitionerTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

## Building a .zip snapshot of this forked connector

The 'deployable artifact' of this connector is a `.zip` archive built by Maven. 

1. `mvn clean install -DskipTests` (skips the integration tests, since these take a while)
2. The `.zip` bundle will appear in `kafka-connect-s3/target/components/packages/confluentinc-kafka-connect-s3-10.0.8.zip`
3. As a sanity check, it's worth running `ls -l kafka-connect-s3/target/components/packages/confluentinc-kafka-connect-s3-10.0.8.zip`, to make sure the last updated time makes sense

## Updating the connector ZIP in S3

### Via CI

There are workflows described in `.github/workflows/deploy-{ENV}.yml` that automatically push an artifact to the correct S3 bucket on merging to the `develop`, `stage` and `prod` branches.

### Via shell script

If for any reason the GitHub actions workflow is broken, or it's not desirable to use it, you can build the artifact with the script in the root of this repo:

```shell
# for dev
$ ./build_zip_and_copy_to_s3.sh dev 
# for stage
$ ./build_zip_and_copy_to_s3.sh stage
# for prod
$ ./build_zip_and_copy_to_s3.sh prod
```

### Once the updates have been pushed to S3

Once the ZIP has been uploaded to the S3 bucket, you'll need to reapply some terraform config.
See the readme [here](https://github.com/Urban-Data-Collective/udx-infra/tree/main/terraform/modules/data-lake/README.md) for what needs to be done.

## Original docs

To build a development version you'll need a recent version of Kafka 
as well as a set of upstream Confluent projects, which you'll have to build from their appropriate snapshot branch.
See [the kafka-connect-storage-common FAQ](https://github.com/confluentinc/kafka-connect-storage-common/wiki/FAQ)
for guidance on this process.

You can build *kafka-connect-storage-cloud* with Maven using the standard lifecycle phases.


# Contribute

- Source Code: https://github.com/confluentinc/kafka-connect-storage-cloud
- Issue Tracker: https://github.com/confluentinc/kafka-connect-storage-cloud/issues


# License

This project is licensed under the [Confluent Community License](LICENSE).


[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fconfluentinc%2Fkafka-connect-storage-cloud.svg?type=large)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fconfluentinc%2Fkafka-connect-storage-cloud?ref=badge_large)
