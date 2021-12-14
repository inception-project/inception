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
  <div class="flex-content flex-v-container">
    <div class="scrolling flex-content">
      {{data.text}}
    </div>
    <div class="scrolling flex-content">
      <table>
        <tr>
          <th>VID</th>
          <th>color</th>
          <th>label</th>
          <th>begin</th>
          <th>end</th>
        </tr>
        <tr v-for="span in data.spans" :key="span.vid">
          <td>{{span.vid}}</td>
          <td>{{span.color}}</td>
          <td>{{span.label}}</td>
          <td>{{span.begin}}</td>
          <td>{{span.end}}</td>
        </tr>
      </table>
      <!-- {{data.spans}} -->
    </div>
    <div v-if="data.arcs" class="scrolling flex-content">
      {{data.arcs}}
    </div>
    <div v-if="diff" class="scrolling flex-content">
      {{diff}}
    </div>
  </div>
</template>

<script>
module.exports = {
  props: {
    ajaxCommandEndpoint: { type: String, required: true },
    wsEndpoint: { type: String, required: true },   // should this be full ws://... url
    topicChannel: { type: String, required: true }
  },
  data() {
    return {
      data: [],
      diff: null,
      socket: null,
      client: null,
      connected: false
    }
  },
  methods: {
  },
    mounted() {
      this.client = new AnnotationEditing.AnnotationEditing();
      this.client.onConnect = () => {
        this.client.subscribeToViewport(this.topicChannel, data => this.data = data);
      };
      this.client.connect(this.wsEndpoint, this.ajaxCommandEndpoint);
    },
    beforeUnmount() {
      this.client.unsubscribeFromViewport();
    }
}
</script>

<style scoped>
</style>