/*
 * Copyright 2016 LinkedIn Corp. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

var itemsReceived = 0;

if ("WebSocket" in window) {
  var websocketurl = document.getElementById('websocketurl').getAttribute("content");
  var ws = new WebSocket(websocketurl);

  ws.onmessage = function (event) {
    var data = JSON.parse(event.data);
    addMetric(data);

    var div = document.getElementById('messages');
    div.innerHTML = "Received Message: " + event.data + "<br/>";
  }
}

function addClientsHandler() {
  var number = document.getElementById("number").value;
  addClients(number);
}

function addClients(n) {
  var url = document.getElementById("url").value;
  ws.send(JSON.stringify({
    event: 'addClients',
    n: n,
    url: url
  }));

  var div = document.getElementById('messages');
  div.innerHTML = div.innerHTML + "Clients Added: " + n + ", with URL:" + url + "<br/>";
}

function removeAllClients() {
  stopRamp();

  ws.send(JSON.stringify({
    event: 'removeAllClients'
  }));

  var div = document.getElementById('messages');
  div.innerHTML = div.innerHTML + "All Clients Removed <br/>";
}

function addMetric(item) {
  itemsReceived++;
  item.index = itemsReceived;
  item.chunksTotal = +item.chunksTotal;
  item.chunks = +item.chunks;
  item.msSinceLastReset = +item.msSinceLastReset;
  item.bytesReceivedTotal = +item.bytesReceivedTotal;
  item.bytesReceived = +item.bytesReceived;
  item.activeClients = +item.activeClients;
  item.clients = +item.clients;
  item.connectionErrors = +item.connectionErrors;
  item.kbPerSec = (item.bytesReceived / 1024 / item.msSinceLastReset * 1000).toFixed(1);
  item.chunksPerSec = (item.chunks / item.msSinceLastReset * 1000).toFixed(0);
  item.totalMB = (item.bytesReceivedTotal / 1024 / 1024).toFixed(3);
  updateUI(item);
}

function numberWithCommas(x) {
  return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function updateUI(item) {
  $("#activeClients").html(numberWithCommas(item.activeClients));
  $("#chunksPerSec").html(numberWithCommas(item.chunksPerSec));
  $("#kbPerSec").html(numberWithCommas(item.kbPerSec));

  $("#totalClients").html(numberWithCommas(item.clients));
  $("#connectionErrors").html(numberWithCommas(item.connectionErrors));
  $("#totalChunks").html(numberWithCommas(item.chunksTotal));
  $("#totalMB").html(numberWithCommas(item.totalMB));
}

function ramp() {
  timer = setInterval(function () {
    addClients($("#rampCount").val());
  }, $("#rampInterval").val() * 1000);
}

function stopRamp() {
  if(timer) {
    clearInterval(timer);
  }
}