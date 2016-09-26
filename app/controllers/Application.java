/*
 * Copyright 2016 LinkedIn Corp. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package controllers;

import actors.SupervisorWebSocketActor;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.JsonNode;
import javax.inject.Inject;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.LegacyWebSocket;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.index;


public class Application extends Controller {
  @Inject
  WSClient ws;

  @Inject
  Materializer materializer;

  public Result index() {
    return ok(index.render("Hurrikane"));
  }

  public LegacyWebSocket<JsonNode> metricsSocket() {
    return WebSocket.withActor(upstream -> SupervisorWebSocketActor.props(ws, materializer, upstream));
  }
}
