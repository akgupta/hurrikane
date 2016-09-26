/*
 * Copyright 2016 LinkedIn Corp. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package actors;

import actors.SupervisorWebSocketActorProtocol.ConnectionError;
import actors.SupervisorWebSocketActorProtocol.Received;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.util.concurrent.CompletionStage;
import play.Logger;
import play.libs.ws.StreamedResponse;
import play.libs.ws.WSClient;


public class WSClientActor extends UntypedActor {

  public static Props props(String url, WSClient ws, Materializer materializer) {
    return Props.create(WSClientActor.class, () -> new WSClientActor(url, ws, materializer));
  }

  private WSClientActor(String url, WSClient ws, Materializer materializer) {
    CompletionStage<StreamedResponse> futureResponse = ws.url(url).setRequestTimeout(-1).setMethod("GET").stream();

    futureResponse.handle((res, exception) -> {
      if (exception != null) {
        Logger.error("Error while opening connection, killing WSClient Actor: ", exception);
        context().parent().tell(new ConnectionError(), self());
        context().stop(self());
        return null;
      }

      Source<ByteString, ?> responseBody = res.getBody();

      // The sink that writes to the output stream
      Sink<ByteString, CompletionStage<akka.Done>> outputHandler = Sink.<ByteString>foreach(bytes -> {
        Logger.debug(bytes.utf8String());
        self().tell(new Received(bytes.length()), null);
      });

      // materialize and run the stream
      responseBody.runWith(outputHandler, materializer).whenComplete((value, error) -> {
        if (error != null) {
          Logger.error(error.getMessage());
        }
        Logger.debug("The server closed the connection, killing the WSClient Actor.");
        context().stop(self());
      });
      return null;
    });
  }

  @Override
  public void onReceive(Object message)
      throws Exception {
    if (message instanceof Received) {
      context().parent().tell(message, self());
    }
  }

  @Override
  public void postStop()
      throws Exception {
    super.postStop();
  }
}
