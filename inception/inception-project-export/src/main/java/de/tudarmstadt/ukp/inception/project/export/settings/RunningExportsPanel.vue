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
  <ul class="list-group list-group-flush">
    <li v-show="!runningExports.length" class="list-group-item p-1">
      No exports.
    </li>
    <li v-for="item in runningExports" :key="item.handle.runId" class="list-group-item p-1">
      <div class="d-flex w-100 justify-content-between"> 
        <h5 class="mb-1">{{item.title}}</h5>
        <button type="button" class="btn-close" aria-label="Close" v-on:click="cancel(item)"/>
      </div>
      <div v-show="item.state != 'COMPLETED'">
        <progress max="100" :value="item.progress" class="w-100"/>
      </div>
      <div v-show="item.state == 'COMPLETED'" class="text-center animated pulse">
        <a :href="item.downloadUrl"><b>Download</b></a>
      </div>
      <ul class="list-group" style="max-height: 5em; overflow: auto;">
        <li v-for="msg in item.messages" class="p0 m0">
          <small class="text-muted">{{msg.level}}: {{msg.message}}</small>
        </li>
      </ul>
    </li>
  </ul>
</template>

<script>
module.exports = {
  props: {
    wsEndpoint: { type: String, required: true },   // should this be full ws://... url
    topicChannel: { type: String, required: true }
  },
  data() {
    return {
      runningExports: [],
      socket: null,
      stompClient: null,
      connected: false
    }
  },
  methods: {
    connect() {
      if (this.connected){
        return;
      }

      this.socket = new WebSocket(this.wsEndpoint);
      this.stompClient = webstomp.over(this.socket);
      var that = this;
      this.stompClient.connect({}, 
        function (frame) {
          that.connected = true;
          that.stompClient.subscribe('/user/queue/errors', function (msg) {
            console.error('Websocket server error: ' + JSON.stringify(msg.body));
          });
          that.stompClient.subscribe('/app' + that.topicChannel, function (msg) {
            that.runningExports = JSON.parse(msg.body);
          });
          that.stompClient.subscribe('/topic' + that.topicChannel, function (msg) {
            var msgBody = JSON.parse(msg.body);
            var i = that.runningExports.findIndex(item => item.handle.runId === msgBody.handle.runId);
            if (i === -1) {
              that.runningExports.push(msgBody);
            }
            else {
              that.runningExports = that.runningExports.map((item, index) => { 
                if (i !== index) {
                  return item
                }
                
                if (!msgBody.messages) {
                  msgBody.messages = item.messages
                }
                else if (item.messages) {
                  msgBody.messages = msgBody.messages.concat(item.messages);
                }
                
                return msgBody;
              });
            }
          });
        },
        function(error){
          console.log("Websocket connection error: " + JSON.stringify(error));
        }
      );
    },

    disconnect() {
      if (this.stompClient !== null) {
        this.stompClient.disconnect();
      }
      this.connected = false;
    },
    
    cancel(runningExport) {
      this.runningExports = this.runningExports.filter(i => i !== runningExport);
      this.stompClient.send('/app' + this.topicChannel + "/cancel", {}, runningExport);
    }
  },
  mounted() {
    this.connect();
  },
  beforeUnmount() {
    this.disconnect();
  }
}
</script>

<style scoped>
</style>