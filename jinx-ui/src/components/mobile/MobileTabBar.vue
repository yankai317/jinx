
<template>
  <div class="mobile-tab-bar">
    <div
      v-for="(item, index) in tabs"
      :key="index"
      class="tab-item"
      :class="{ active: activeTab === item.key }"
      @click="handleTabClick(item.key)"
    >
      <component :is="item.icon" class="tab-icon" />
      <span class="tab-text">{{ item.text }}</span>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  HomeOutlined,
  ReadOutlined,
  UserOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'MobileTabBar',
  components: {
    HomeOutlined,
    ReadOutlined,
    UserOutlined
  },
  props: {
    // 当前激活的标签
    active: {
      type: String,
      default: 'home'
    }
  },
  emits: ['change'],
  setup(props, { emit }) {
    const router = useRouter();
    const route = useRoute();
    const activeTab = ref(props.active);

    // 底部导航栏配置
    const tabs = [
      {
        key: 'home',
        text: '首页',
        icon: HomeOutlined,
        route: '/toc/mobile/home'
      },
      {
        key: 'learning',
        text: '学习中心',
        icon: ReadOutlined,
        route: '/toc/mobile/learning/center'
      },
      {
        key: 'profile',
        text: '我的',
        icon: UserOutlined,
        route: '/toc/mobile/personal/center'
      }
    ];

    // 处理标签点击
    const handleTabClick = (key) => {

      activeTab.value = key;
      emit('change', key);

      // 查找对应的路由并导航
      const tab = tabs.find(item => item.key === key);
      if (tab && tab.route && route.path !== tab.route) {
        router.push(tab.route);
      }
    };

    return {
      activeTab,
      tabs,
      handleTabClick
    };
  }
});
</script>

<style scoped>
.mobile-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 65px;
  background-color: #FFFFFF;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 24px;
  box-shadow: 0 -1px 0 0 #F3F4F6;
  z-index: 100;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  cursor: pointer;
}

.tab-icon {
  font-size: 20px;
  color: #9CA3AF;
  margin-bottom: 4px;
}

.tab-text {
  font-size: 12px;
  color: #9CA3AF;
}

.tab-item.active .tab-icon,
.tab-item.active .tab-text {
  color: #1890FF;
}
</style>
