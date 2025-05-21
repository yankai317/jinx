
<template>
  <div class="course-card" @click="handleCardClick">
    <div class="course-cover">
      <img v-if="item.cover" :src="item.cover" :alt="item.name" class="cover-image" />
      <div v-else class="cover-placeholder">
        <file-image-outlined />
      </div>
      <div v-if="item.type" class="course-type" :class="typeClass">
        {{ typeText }}
      </div>
    </div>
    <div class="course-content">
      <div v-if="showSource && item.source" class="course-source" :class="sourceClass">
        {{ sourceText }}
      </div>
      <h3 class="course-title" :title="item.rawName || item.name" v-html="item.name"></h3>
      <div class="course-info">
        <span v-if="item.credit" class="course-credit">{{ item.credit }} 学分</span>
        <span v-if="item.viewCount" class="course-view-count">{{ item.viewCount }} 次查看</span>
        <span v-if="item.completeCount" class="course-complete-count">{{ item.completeCount }} 人完成</span>
      </div>
      <div v-if="showProgress && item.progress !== undefined" class="course-progress">
        <a-progress
          :percent="item.progress"
          :status="item.status === 1 ? 'success' : 'active'"
          size="small"
        />
        <div class="progress-text">
          <span>进度 {{ item.progress }}%</span>
          <span v-if="item.deadline" class="deadline">
            截止: {{ formatDate(item.deadline) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, computed } from 'vue';
import { useRouter } from 'vue-router';
import { FileImageOutlined } from '@ant-design/icons-vue';

export default defineComponent({
  name: 'CourseCard',
  components: {
    FileImageOutlined
  },
  props: {
    // 课程/培训/学习地图数据
    item: {
      type: Object,
      required: true,
      default: () => ({})
    },
    // 是否显示进度条
    showProgress: {
      type: Boolean,
      default: true
    },
    // 是否显示来源标签
    showSource: {
      type: Boolean,
      default: true
    }
  },
  setup(props) {
    const router = useRouter();

    // 计算类型样式
    const typeClass = computed(() => {
      const typeMap = {
        'COURSE': 'type-course',
        'TRAIN': 'type-train',
        'MAP': 'type-map'
      };
      return typeMap[props.item.type] || 'type-course';
    });

    // 计算类型文本
    const typeText = computed(() => {
      const typeMap = {
        'COURSE': '课程',
        'TRAIN': '培训',
        'LEARNING_MAP': '地图'
      };
      return typeMap[props.item.type] || '课程';
    });

    // 计算来源样式
    const sourceClass = computed(() => {
      const sourceMap = {
        'ASSIGN': 'source-assign',
        'SELF': 'source-self'
      };
      return sourceMap[props.item.source] || 'source-self';
    });

    // 计算来源文本
    const sourceText = computed(() => {
      const sourceMap = {
        'ASSIGN': '指派',
        'SELF': '自学'
      };
      return sourceMap[props.item.source] || '自学';
    });

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    };

    // 点击卡片跳转到详情页
    const handleCardClick = () => {
      if (!props.item || !props.item.id) return;

      console.log(props.item);
      const typeRouteMap = {
        'COURSE': `/toc/course/detail/${props.item.id}`,
        'TRAIN': `/toc/train/detail/${props.item.id}`,
        'LEARNING_MAP': `/toc/map/detail/${props.item.id}`
      };

      const route = typeRouteMap[props.item.type];
      if (route) {
        router.push(route);
      }
    };

    return {
      typeClass,
      typeText,
      sourceClass,
      sourceText,
      formatDate,
      handleCardClick
    };
  }
});
</script>

<style scoped>
.course-card {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
}

.course-cover {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f3f4f6;
  color: #9ca3af;
  font-size: 48px;
}

.course-type {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
}

.type-course {
  background-color: #1890FF;
}

.type-train {
  background-color: #10b981;
}

.type-map {
  background-color: #f59e0b;
}

.course-content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.course-source {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
}

.source-assign {
  background-color: #fee2e2;
  color: #ef4444;
}

.source-self {
  background-color: #e0f2fe;
  color: #0ea5e9;
}

.course-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.course-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  color: #6b7280;
  font-size: 14px;
}

.course-credit {
  color: #1890FF;
}

.course-progress {
  margin-top: auto;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.deadline {
  color: #ef4444;
}
</style>
