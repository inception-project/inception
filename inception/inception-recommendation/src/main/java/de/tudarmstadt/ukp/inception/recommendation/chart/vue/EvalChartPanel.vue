/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
  <template>
  <div class="card">
    <div class="card-header">
      Evaluation Scores
    </div>
    <div id="chartContainer" class="card-body">
    </div>
  </div>
</template>

<script>
module.exports = {
  props: {
    wsEndpoint: { type: String, required: true },   // should this be full ws://... url
    topicChannel: { type: String, required: true },  // this should be /queue/recEvents
    projectId: { type: Number }
  },
  data() {
    return {
      socket: null,
      stompClient: null,
      connected: false,
      evalChart: null
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
          if (that.projectId == -1){
            return;
          }
          that.stompClient.subscribe('/user/queue' + that.topicChannel + '/' + that.projectId, function (msg) {
            let msgBody = JSON.parse(msg.body);
            // FIXME might need to find a solution for this being an error msg 
            // and not an evaluation message?
            if (evalChart === null){
              that.evalChart = new EvalChart("chartContainer", msgBody);
            } else{
              that.evalChart.update(msgBody.eventMsg);
            }
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
    }
  },
  mounted(){
    this.connect();
  },
  beforeUnmount(){
    this.disconnect();
  }
}
</script>

<style scoped>
</style>