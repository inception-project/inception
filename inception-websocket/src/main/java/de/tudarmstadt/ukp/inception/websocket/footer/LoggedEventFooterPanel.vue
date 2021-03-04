<!--
  Licensed to the Technische Universität Darmstadt under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The Technische Universität Darmstadt 
  licenses this file to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.
   
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<template>
  <div class="float-left dropup" style="position: static; margin-right: 4px; margin-left: 4px">
    <a role="button" data-toggle="dropdown" id="eventsPanelDropupLink" aria-haspopup="true" aria-expanded="false" @click="toggleConnection">
      <i class="fas fa-rss"></i>
    </a>
    <div class="dropdown-menu" aria-labelledby="eventsPanelDropupLink">
      <div class="card-header">
      Recent logged events
      </div>
      <div class="card-body">
      <ul class="list-group list-group-flush">
        <li v-show="!events.length" class="list-group-item">No recent events</li>
        <li v-for="event in events" :key="event.id" class="list-group-item">{{event.creationDate.format("LLLL")}}: {{event.eventMsg}}</li>
      </ul>
      </div>
    </div>
  </div>
</template>

<script>
module.exports = {
  props: {
    wsEndpoint: { type: String, required: true },   // should this be full ws://... url
    topicChannel: { type: String, required: true }  // this should be /queue/loggedevents
  },
  data() {
    return {
      events: [],
      socket: null,
      stompClient: null,
      connected: false
    }
  },
  methods: {
    toggleConnection(){
      if (this.connected){
        this.disconnect();
        return;
      }
      this.connect();
    },
    connect(){
      this.socket = new WebSocket(this.wsEndpoint);
      this.stompClient = webstomp.over(this.socket);
      var that = this;
      this.stompClient.connect({}, 
        function (frame) {
          console.log('Connected: ' + frame);
          that.connected = true;
          that.stompClient.subscribe('/user' + that.topicChannel, function (msg) {
            console.log('Received: ' + msg);
            that.events.push(JSON.parse(msg.body));
          });
        },
        function(error){
          console.log("Websocket connection error: " + JSON.stringify(error));
        }
      );
    },
    disconnect(){
      console.log("Websocket disconnecting");
      if (this.stompClient !== null) {
        this.stompClient.disconnect();
      }
      this.connected = false;
      console.log("Disconnected");
    }
  },
  beforeUnmount(){
    disconnect();
  }
}
</script>

<style scoped>
</style>