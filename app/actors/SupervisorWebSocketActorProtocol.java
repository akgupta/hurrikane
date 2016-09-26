/*
 * Copyright 2016 LinkedIn Corp. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package actors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


public abstract class SupervisorWebSocketActorProtocol {

  public static class Received {
    private final long bytes;

    public Received(long bytes) {
      this.bytes = bytes;
    }

    public long getBytes() {
      return bytes;
    }
  }

  public static class Publish { }

  public static class ConnectionError { }

  /**
   * Events to/from the client side
   */
  @JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.NAME, property = "event")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = AddClients.class, name = "addClients"),
      @JsonSubTypes.Type(value = RemoveAllClients.class, name = "removeAllClients")
  })
  public static abstract class ClientEvent {
    private ClientEvent() {
    }
  }

  public static class AddClients extends ClientEvent {
    private final long n;
    private final String url;

    public AddClients(@JsonProperty("n") long n, @JsonProperty("url") String url) {
      this.n = n;
      this.url = url;
    }

    public long getN() {
      return n;
    }

    public String getUrl() {
      return url;
    }
  }

  public static class RemoveAllClients extends ClientEvent {
  }
}
