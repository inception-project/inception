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
  <div v-if="connectionError" class="flex-content flex-h-container no-data-notice">
    {{ connectionError }}
  </div>
  <div v-if="!connected" class="flex-content flex-h-container no-data-notice">
    Connecting...
  </div>
  <div v-if="connected && !exports.length" class="flex-content flex-h-container no-data-notice">
    No exports.
  </div>
  <ul v-if="exports.length" class="list-group list-group-flush">
    <li v-for="item in exports" :key="item.id" class="list-group-item p-2">
      <div class="d-flex w-100 justify-content-between"> 
        <h5 class="mb-1">{{item.title}}</h5>
        <div v-if="!cancelPending.has(item.id)">
          <button type="button" class="btn-close" aria-label="Close" v-on:click="cancel(item)"/>
        </div>
        <div v-if="cancelPending.has(item.id)">
          Aborted!
        </div>
      </div>
      <div v-if="item.state === 'RUNNING'">
        <progress max="100" :value="item.progress" class="w-100"/>
      </div>
      <div v-if="item.state === 'COMPLETED'" class="text-center">
        <button type="button" class="animated pulse btn btn-primary" v-on:click="download(item)">
          <i class="fas fa-download"></i> Download
        </button>
      </div>
      <div v-if="item.messages" class="card">
        <div class="card-header small">
          Messages
          <button type="button" class="btn-close float-end" aria-label="Close" v-on:click="closeMessages(item)"/>
        </div>
        <div class="card-body" style="max-height: 10em; min-height: 3em; overflow: auto;">
          <div v-for="message in item.messages">
            <small>
              <i v-if="message.level === 'ERROR'" class="text-danger fas fa-exclamation-triangle"></i>
              <i v-else-if="message.level === 'WARN'" class="text-warning fas fa-exclamation-triangle"></i>
              <i v-else class="text-muted fas fa-info-circle"></i>
              {{message.message}}
            </small>
          </div>
        </div>
      </div>
      <div v-else-if="item.latestMessage" class="p-2">
        <small>
          <i v-if="item.latestMessage.level === 'ERROR'" class="text-danger fas fa-exclamation-triangle"></i>
          <i v-else-if="item.latestMessage.level === 'WARN'" class="text-warning fas fa-exclamation-triangle"></i>
          <i v-else class="text-muted fas fa-info-circle"></i>
          {{item.latestMessage.message}}
          <span v-if="item.messageCount > 1 && item.state !== 'RUNNING'" v-on:click="loadMessages(item)" class="float-end badge rounded-pill bg-light text-dark" style="cursor: pointer;">
            Show all {{item.messageCount}} messages...
          </span>
        </small>
      </div>
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
      exports: [],
      cancelPending: new Set(),
      socket: null,
      stompClient: null,
      connected: false,
      connectionError: false
    }
  },
  methods: {
    connect() {
      if (this.connected){
        return;
      }

      let protocol = (window.location.protocol === 'https:' ? 'wss:' : 'ws:');
      let wsEndpoint = new URL(this.wsEndpoint)
      wsEndpoint.protocol = protocol;

      this.socket = new WebSocket(wsEndpoint.toString());
      this.stompClient = webstomp.over(this.socket);
      var that = this;
      this.stompClient.connect({}, 
        function (frame) {
          that.connected = true;
          that.stompClient.subscribe('/user/queue/errors', function (msg) {
            console.error('Websocket server error: ' + JSON.stringify(msg.body));
          });
          that.stompClient.subscribe('/app' + that.topicChannel, function (msg) {
            that.exports = JSON.parse(msg.body);
          });
          that.stompClient.subscribe('/topic' + that.topicChannel, function (msg) {
            var msgBody = JSON.parse(msg.body);
            var i = that.exports.findIndex(item => item.id === msgBody.id);
            if (i === -1) {
              if (!msgBody.removed) {
                that.exports.push(msgBody);
              }
              else {
                that.cancelPending.delete(msgBody.id);
              }
            }
            else {
              if (!msgBody.removed) {
                that.exports = that.exports.map((item, index) => i !== index ? item : msgBody);
              }
              else {
                that.exports.splice(i, 1);
                that.cancelPending.delete(msgBody.id);
              }
            }
          });
        },
        function(error){
          console.log("WebSocket connection error: " + JSON.stringify(error));
          this.connectionError = "Unable to establish WebSocket connection!";
        }
      );
    },

    disconnect() {
      if (this.stompClient !== null) {
        this.stompClient.disconnect();
      }
      this.connected = false;
    },
    
    download(item) {
      window.location=item.url + '/data';
    },
    
    cancel(task) {
      this.cancelPending.add(task.id);
      this.stompClient.send('/app/export/' + task.id + '/cancel', {}, {});
    },

    loadMessages(item) {
      axios
        .get(item.url + '/log')
        .then(response => (item.messages = response['data']))
    },
    
    closeMessages(item) {
      item.messages = null;
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