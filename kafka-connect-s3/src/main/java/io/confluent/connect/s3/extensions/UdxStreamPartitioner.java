/*
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.connect.s3.extensions;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.confluent.connect.storage.errors.PartitionException;
import io.confluent.connect.storage.partitioner.DefaultPartitioner;

import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class UdxStreamPartitioner<T> extends DefaultPartitioner<T> {
  private static final Logger log = LoggerFactory.getLogger(UdxStreamPartitioner.class);
  private static final String PARTITION_FORMAT =
          "stream_uuid=%s/entity_id=%s/year_month=%d-%02d/day=%02d/hour=%02d";

  private static final String CORRUPT_AVRO_PAYLOAD_PARTITION_FORMAT =
          "invalid_payloads/corrupt_payloads";
  private static final String INVALID_PAYLOAD_PARTITION_FORMAT =
          "invalid_payloads/stream_uuid=%s";
  private static final String INVALID_TIMESTAMP_PARTITION_FORMAT =
          "invalid_payloads/stream_uuid=%s/entity_id=%s/%s";

  @Override
  public void configure(Map<String, Object> config) {
    log.info("Configuring UdxStreamPartitioner...");
    super.configure(config);
  }

  private String getStreamUuidFromHeaders(SinkRecord sinkRecord) {
    log.info("Getting offering_uuid value from headers...");
    String streamUuid = null;
    for (Header header : sinkRecord.headers()) {
      System.out.println("header key => "
              + header.key()
              + "header value => "
              + header.value().toString());
      if (header.key().equals("offering_uuid")) {
        log.info("setting streamUuid to: " + header.value().toString());
        streamUuid = header.value().toString();
      }
    }
    return streamUuid == null ? "noStreamIdFound" : streamUuid;
  }

  private boolean timestampCanParseToLong(String timestamp) {
    try {
      Long.parseLong(timestamp);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private DateTime parseTimestampFromPayload(String timestamp) {
    log.info("parsing timestamp : " + timestamp);
    // Here the timestamp could be a long as a string or an ISO timestamp
    // We must therefore see if a long can be parsed from the string...
    if (timestampCanParseToLong(timestamp)) {
      long timestampAsLong = Long.parseLong(timestamp);
      log.info("Parsed timestamp as : " + timestampAsLong);
      return new DateTime(timestampAsLong).withZone(DateTimeZone.UTC);
    }

    try {
      return new DateTime(timestamp).withZone(DateTimeZone.UTC);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new PartitionException("Could not parse timestamp from payload");
    }

  }

  private String generateCompletePartition(String streamUuid, String entityId, DateTime timestamp) {
    int year = timestamp.getYear();
    int month = timestamp.getMonthOfYear();
    int day = timestamp.getDayOfMonth();
    int hour = timestamp.getHourOfDay();
    return String.format(PARTITION_FORMAT, streamUuid, entityId, year, month, day, hour);
  }

  private String generateCorruptAvroPayloadPartition() {
    return CORRUPT_AVRO_PAYLOAD_PARTITION_FORMAT;
  }

  private String generateInvalidPayloadPartition(String streamUuid) {
    return String.format(INVALID_PAYLOAD_PARTITION_FORMAT, streamUuid);
  }

  private String generateInvalidTimestampPartition(
          String streamUuid,
          String entityUuid,
          String timestamp
  ) {
    return String.format(INVALID_TIMESTAMP_PARTITION_FORMAT, streamUuid, entityUuid, timestamp);
  }

  private UdxPayload parseJsonStringToKnownClass(String jsonStringValue) {
    ObjectMapper mapper = new ObjectMapper();

    // We don't want to have to extend our POJO with every single
    // field that appears in a payload, we only care about the fields
    // that make up the partitions in S3, hence this FAIL_ON_UNKNOWN_PROPERTIES
    // setting
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // the value of the timestamp can either be:
    // { timestamp: string } (in an OCPI session payload)
    // or
    // { timestamp: { type: 'Property', value: string } } (in an OCPI location payload)
    // We account for both with some sort of generic function...

    UdxPayload udxPayload = null;

    // An alternative approach to this could be to use a custom JsonDeserializer:
    // https://www.baeldung.com/jackson-nested-values#mapping-with-custom-jsondeserializer
    // The use of concrete *Payload classes does make things very explicit, though
    try {
      // try to parse from { timestamp: string }
      udxPayload = mapper.readValue(jsonStringValue, FlatTimestampPayload.class);
    } catch (JacksonException e) {
      log.warn("Could not parse payload into FlatTimestampPayload POJO");
    }

    try {
      // try to parse from { timestamp: { type: 'Property', value: string } }
      udxPayload = mapper.readValue(jsonStringValue, NestedTimestampPayload.class);
    } catch (JacksonException e) {
      log.warn("Could not parse payload into NestedTimestampPayload POJO");
    }

    try {
      log.info("Mapped to: " + udxPayload.getClass().toString());
    } catch (Exception e) {
      log.warn("Could not map udxPayload to a known class");
    }

    return udxPayload;
  }

  @Override
  public String encodePartition(SinkRecord sinkRecord) {
    // NOTE that any Exceptions thrown from this class
    // will (most helpfully) crash the entire connector. This means it will not
    // start up correctly. This means there is no data lake

    // for original inspiration, see:
    // https://stackoverflow.com/questions/57499274/implementing-a-kafka-connect-custom-partitioner
    log.info("encoding partition with UdxStreamPartitioner...");
    log.info("Parsing value...");

    String payload = sinkRecord.value().toString();
    ObjectMapper mapper = new ObjectMapper();
    AvroPayload avroPayload;
    try {
      avroPayload = mapper.readValue(payload,AvroPayload.class);
    } catch (JsonProcessingException e) {
      return generateCorruptAvroPayloadPartition();
    }

    String jsonStringValue = avroPayload.getPayload();
    log.info("Value: " + jsonStringValue);
    String streamUuid = null;

    try {
      log.info("Assuming streamUuid is in the headers...");
      streamUuid = getStreamUuidFromHeaders(sinkRecord);
      UUID.fromString(streamUuid);
    } catch (IllegalArgumentException exception) {
      String msg =
              "stream uuid in header is not a valid uuid "
              + "it therefore probably not a valid stream id";
      log.warn(msg);
    }

    UdxPayload udxPayload = parseJsonStringToKnownClass(jsonStringValue);

    if (udxPayload == null || udxPayload.getId() == null || udxPayload.getTimestamp() == null) {
      log.warn("No UdxPayload mapping found, sending payload to non stream uuid partition...");
      String msg = "Could not map this payload to a defined UdxPayload class";
      // At this point, it might be a good idea to see if we can still parse the timestamp.
      // If not that, just use the timestamp of the message supplied by kakfa?
      return generateInvalidPayloadPartition(streamUuid);
    }

    log.info("Mapping record value into object...");
    log.info(udxPayload.toString());
    String entityIdRaw = udxPayload.getId();
    String timestamp = udxPayload.getTimestamp();

    // Replace all disallowed characters from entityId as these
    // special characters can't be part of partition path.
    String entityId = entityIdRaw.replaceAll("[^A-Za-z0-9!\\-_\\.*'()]", "_");
    log.info("Original entityId = " + entityIdRaw + ", Transformed entityId = " + entityId);

    try {
      DateTime parsedTimestamp = parseTimestampFromPayload(timestamp);
      return generateCompletePartition(streamUuid, entityId, parsedTimestamp);
    } catch (Exception e) {
      log.warn(e.getMessage());
      String msg = "Could not parse YYYY-MM/DD/HH values from timestamp: "
              + timestamp
              + " => Cannot build partition";
      log.warn(msg);
      return generateInvalidTimestampPartition(streamUuid, entityId, timestamp);
    }
  }
}
