<template>
  <visibility-setting-component
    :biz-id="bizId"
    :biz-type="bizType"
    :title="title"
    @success="onSuccess"
    @cancel="onCancel"
  />
</template>

<script>
import { defineComponent } from 'vue';
import VisibilitySettingComponent from './VisibilitySettingComponent.vue';

export default defineComponent({
  name: 'VisibilitySettingPage',
  components: {
    VisibilitySettingComponent
  },
  props: {
    bizId: {
      type: Number,
      required: true
    },
    bizType: {
      type: String,
      default: 'course'
    },
    title: {
      type: String,
      default: '可见范围设置'
    },
    onSuccess: {
      type: Function,
      default: () => {}
    },
    onCancel: {
      type: Function,
      default: () => {}
    }
  }
});
</script>
