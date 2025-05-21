
<template>
  <div class="empty-state">
    <div class="empty-icon">
      <slot name="icon">
        <inbox-outlined />
      </slot>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p class="empty-description">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action"></slot>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import { InboxOutlined } from '@ant-design/icons-vue';

export default defineComponent({
  name: 'EmptyState',
  components: {
    InboxOutlined
  },
  props: {
    // 空状态标题
    title: {
      type: String,
      default: '暂无数据'
    },
    // 空状态描述
    description: {
      type: String,
      default: '没有找到相关数据'
    }
  }
});
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: #d1d5db;
  margin-bottom: 16px;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #374151;
}

.empty-description {
  margin: 0 0 24px;
  font-size: 14px;
  color: #6b7280;
  max-width: 400px;
}

.empty-action {
  margin-top: 16px;
}
</style>
