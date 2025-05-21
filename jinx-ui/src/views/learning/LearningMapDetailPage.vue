<template>
  <div class="learning-map-detail">
    <a-list :data-source="taskList">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta
            :avatar="taskTypeIcon[item.type]"
            :description="`类型：${getTaskTypeName(item.type)}`"
          >
            <template #title>
              <a @click="viewTaskDetail(item)">
                {{ item.title }}
                <a-tag v-if="item.isRequired" color="red" style="margin-left: 8px;">必修</a-tag>
                <a-tag v-else color="blue" style="margin-left: 8px;">选修</a-tag>
              </a>
            </template>
          </a-list-item-meta>
          <template #actions>
            <a-button type="link" @click="viewTaskDetail(item)">
              查看
            </a-button>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script>
import { defineComponent, ref } from 'vue'

export default defineComponent({
  name: 'LearningMapDetailPage',
  setup() {
    const taskList = ref([])
    const taskTypeIcon = {
      // 在这里定义任务类型对应的图标
      COURSE: '/icons/course.png',
      EXAM: '/icons/exam.png',
      ASSIGNMENT: '/icons/assignment.png',
      SURVEY: '/icons/survey.png'
    }

    const getTaskTypeName = (type) => {
      const typeMap = {
        COURSE: '课程',
        EXAM: '考试',
        ASSIGNMENT: '作业',
        SURVEY: '调研'
      }
      return typeMap[type] || type
    }

    const viewTaskDetail = (item) => {
      // 查看任务详情的方法
      console.log('查看任务详情:', item)
    }

    return {
      taskList,
      taskTypeIcon,
      getTaskTypeName,
      viewTaskDetail
    }
  }
})
</script>

<style scoped>
.learning-map-detail {
  padding: 24px;
}
</style> 