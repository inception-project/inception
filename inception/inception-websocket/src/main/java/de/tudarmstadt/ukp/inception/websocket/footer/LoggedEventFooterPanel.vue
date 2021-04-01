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
  <div class="float-left">
    <div class="btn-group dropup">
      <a role="button" class="p-0 m-0 btn btn-secondary dropdown-toggle" data-boundary="viewport" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" @click="toggleConnection">
        <i class="fas fa-rss"></i>
      </a>
      <div class="dropdown-menu">
        <div class="card-header">
        Recent logged events
        </div>
        <div class="card-body">
        <ul class="list-group list-group-flush">
          <li v-show="!events.length" class="list-group-item">No recent events</li>
          <li v-for="event in events" class="list-group-item">{{formatTime(event.timestamp)}}: {{event.eventMsg}}</li>
        </ul>
        </div>
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
          that.stompClient.subscribe('/user/queue/errors', function (msg) {
            console.error('Websocket server error: ' + JSON.stringify(msg));
          });
          that.stompClient.subscribe('/app' + that.topicChannel, function (msg) {
            console.log('Received initial data: ' + JSON.stringify(msg));
              that.events = JSON.parse(msg.body);
          });
          that.stompClient.subscribe('/topic' + that.topicChannel, function (msg) {
            console.log('Received: ' + JSON.stringify(msg));
            that.events.unshift(JSON.parse(msg.body));
            that.events.pop();
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
    },
    formatTime(timestamp) {
      return dayjs(timestamp).format("LLLL")
    }
  },
  beforeUnmount(){
    disconnect();
  }
}
</script>

<style scoped>
</style>