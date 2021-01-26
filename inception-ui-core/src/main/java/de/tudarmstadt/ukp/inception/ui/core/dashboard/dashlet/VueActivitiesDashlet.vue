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
  <div class="flex-v-container">
    <div class="card-header">
      Recent activity
    </div>
    <ul class="list-group list-group-flush">
      <li v-if="!activities" class="list-group-item">Loading...</li>
      <li v-for="activity in activities" :key="activity.id" class="list-group-item" >
        <span class="badge badge-secondary float-right">
          {{ activity.type }}
        </span>
        <a :href="activity.link" :title="activity.documentName" style="display: block" class="text-truncate">{{ activity.documentName }}</a>
        <div class="flex-h-container">
          <small class="text-muted flex-content">
            <span v-if="displayUserInfo(activity)">
              <i class="far fa-user"></i>
              {{ activity.annotator }}
            </span>
          </small>
          <small class="text-muted">
            <i class="far fa-clock"/>&nbsp;
            <span :title="formatTime(activity.timestamp)">
              {{ formatRelativeTime(activity.timestamp) }}
            </span>
          </small>
        </div>
      </li>
    </ul>
  </div>
</template>

<script>
module.exports = {
  props: {
    dataUrl: { type: String, required: true }
  },
  data() {
    return {
      activities: null
    };
  },
  mounted() {
    axios
      .get(this.dataUrl)
      .then(response => (this.activities = response['data']))
  },
  methods: {
    formatRelativeTime(timestamp) {
      return dayjs().to(dayjs(timestamp))
    },
    formatTime(timestamp) {
      return dayjs(timestamp).format("LLLL")
    },
    displayUserInfo(activity) {
      return activity.user != activity.annotator && activity.annotator != "CURATION_USER"
    },
  }
};
</script>

<style scoped>
</style>
