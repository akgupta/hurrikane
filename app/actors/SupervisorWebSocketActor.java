/*
 * Copyright 2016 LinkedIn Corp. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package actors;

import actors.SupervisorWebSocketActorProtocol.AddClients;
import actors.SupervisorWebSocketActorProtocol.ClientEvent;
import actors.SupervisorWebSocketActorProtocol.ConnectionError;
import actors.SupervisorWebSocketActorProtocol.Publish;
import actors.SupervisorWebSocketActorProtocol.Received;
import actors.SupervisorWebSocketActorProtocol.RemoveAllClients;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import play.libs.Akka;
import play.libs.Json;
import play.libs.ws.WSClient;
import scala.concurrent.duration.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;


public class SupervisorWebSocketActor extends UntypedActor {
  private long bytesReceivedTotal = 0L;
  private long bytesReceived = 0L;
  private long chunksTotal = 0L;
  private long chunks = 0L;
  private long connectionErrors = 0L; // number of clients that could not open a connection to the server and thus, not added to the pool of clients
  private long lastReset = System.currentTimeMillis();
  private HashSet<ActorRef> activeClients = new HashSet<>();
  private final WSClient ws;
  private final Materializer materializer;
  private final ActorRef upstream;

  public static Props props(WSClient ws, Materializer materializer, ActorRef upstream) {
    return Props.create(SupervisorWebSocketActor.class, () -> new SupervisorWebSocketActor(ws, materializer, upstream));
  }

  public SupervisorWebSocketActor(WSClient ws, Materializer materializer, ActorRef upstream) {
    this.ws = ws;
    this.materializer = materializer;
    this.upstream = upstream;
  }

  @Override
  public void preStart() throws Exception {
    super.preStart();

    Akka.system()
        .scheduler()
        .schedule(Duration.create(3, SECONDS), Duration.create(3, SECONDS), self(), new Publish(),
            context().dispatcher(), null);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    if (message instanceof JsonNode) {
      ClientEvent event = Json.fromJson((JsonNode) message, ClientEvent.class);

      if (event instanceof AddClients) {
        AddClients addClients = (AddClients) event;
        for (int i = 0; i < addClients.getN(); i++) {
          context().actorOf(WSClientActor.props(addClients.getUrl(), ws, materializer));
        }
      }
      if (event instanceof RemoveAllClients) {
        Iterable<ActorRef> children = scala.collection.JavaConversions.asJavaIterable(context().children());
        for (ActorRef child : children) {
          context().stop(child);
        }
      }
    }

    if (message instanceof ConnectionError) {
      connectionErrors++;
    }

    if (message instanceof Received) {
      Received received = (Received) message;
      bytesReceived += received.getBytes();
      bytesReceivedTotal += received.getBytes();
      activeClients.add(sender());
      chunksTotal++;
      chunks++;
    }

    if (message instanceof Publish) {
      ObjectNode result = Json.newObject();
      result.put("bytesReceivedTotal", bytesReceivedTotal);
      result.put("bytesReceived", bytesReceived);
      result.put("msSinceLastReset", System.currentTimeMillis() - lastReset);
      result.put("chunksTotal", chunksTotal);
      result.put("chunks", chunks);
      result.put("activeClients", activeClients.size());
      result.put("clients", getContext().children().size());
      result.put("connectionErrors", connectionErrors);

      // push to event stream
      upstream.tell(result, self());

      lastReset = System.currentTimeMillis();
      bytesReceived = 0L;
      activeClients = new HashSet<>();
      chunks = 0L;
    }
  }
}
