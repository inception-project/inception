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
  <div class="float-start">
    <div class="mx-2">
      <a role="button" class="log-toggle ml-1 mr-1" data-bs-toggle="popover" aria-haspopup="true" aria-expanded="false">
        <i class="fas fa-scroll"></i>
      </a>
      <div class="d-none">
        <div class="log-popup">
          <div class="scrolling card-body small p-0">
            <ul class="list-group list-group-flush">
              <li v-show="!events.length" class="list-group-item p-1">No recent events</li>
              <li v-for="event in events" class="list-group-item p-1">{{formatTime(event.timestamp)}}: {{event.eventType}}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
module.exports = {
  props: {
    wsEndpoint: { type: String, required: true },   // should this be full ws://... url
    topicChannel: { type: String, required: true },  // this should be /queue/loggedevents
    feedbackPanelId: { type: String, required: true }
  },
  data() {
    return {
      events: [],
      socket: null,
      stompClient: null,
      connected: false,
      feedbackPanelExtension: new FeedbackPanelExtension(this.feedbackPanelId)
    }
  },
  methods: {
    connect(){
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
            that.events = JSON.parse(msg.body);
          });
          that.stompClient.subscribe('/topic' + that.topicChannel, function (msg) {
            var msgBody = JSON.parse(msg.body);
            that.events.unshift(msgBody);
            that.events.pop();
            var msg = that.formatTime(msgBody.timestamp) + ': ' + msgBody.eventType;
            that.feedbackPanelExtension.addInfoToFeedbackPanel(msg);
          });
        },
        function(error){
          console.log("Websocket connection error: " + JSON.stringify(error));
        }
      );
    },
    disconnect(){
      if (this.stompClient !== null) {
        this.stompClient.disconnect();
      }
      this.connected = false;
    },
    formatTime(timestamp) {
      return dayjs(timestamp).format("LLLL")
    }
  },
  mounted(){
    this.connect();
    new bootstrap.Popover(this.$el.querySelector('.log-toggle'), { 
      title: 'Recently logged events',
      container: 'body',
      offset: [0, 12],
      html: true,
      placement: 'top',
      fallbackPlacements: ['top'],
      customClass: 'shadow-lg above-modal w-auto mw-100',
      content: this.$el.querySelector('.log-popup'),
      template: '<div class="popover" role="tooltip"><div class="popover-arrow"></div><h3 class="popover-header"></h3><div class="popover-body p-0"></div></div>'
    })
  },
  beforeUnmount(){
    this.disconnect();
  }
}
</script>

<style scoped>
</style>