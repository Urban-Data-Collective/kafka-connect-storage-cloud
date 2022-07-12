package io.confluent.connect.s3.extensions;

public class AvroPayload {
  private String payload;

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}
