<template>
  <div class="flex-tile" style="flex: 0.5; padding: 0px; min-width: 20rem;">
    <div class="flex-v-container">
      <div class="card-header">
        Recent activity (Vue)
      </div>
      <ul class="list-group list-group-flush">
        <li v-for="activity in activities" :key="activity.id" class="list-group-item" >
          <span class="badge badge-secondary float-right">
            {{ activity.type }}
          </span>
          <a :href="activity.link">{{ activity.documentName }}</a>
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