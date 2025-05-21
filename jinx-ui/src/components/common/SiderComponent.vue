<template>
<a-layout-sider v-model:collapsed="localCollapsed" collapsible width="200" class="sider">
  <a-menu v-model:selectedKeys="localSelectedKeys" v-model:openKeys="localOpenKeys" mode="inline" theme="light">
    <!-- 顶级菜单项 -->
    <template v-for="item in menuItems" :key="item.key">
      <a-menu-item v-if="!item.children && (!item?.permissionCodes?.length || matchPermission(item?.permissionCodes))" :key="item.key" @click="navigateTo(item.path)">
        <component :is="iconComponents[item.icon]" />
        <span>{{ item.title }}</span>
      </a-menu-item>
    </template>

    <!-- 子菜单 -->
    <template v-for="item in menuItems" :key="`sub-${item.key}`">
      <a-sub-menu :key="item.key" v-if="item.children && (!item?.permissionCodes?.length || matchPermission(item?.permissionCodes))">
        <template #title>
          <span>
            <component :is="iconComponents[item.icon]" />
            <span>{{ item.title }}</span>
          </span>
        </template>
        <a-menu-item v-for="child in item.children" :key="child.key" @click="navigateTo(child.path)" v-if="!child?.permissionCodes?.length || matchPermission(child?.permissionCodes)">
          <component :is="iconComponents[child.icon]" />
          <span>{{ child.title }}</span>
        </a-menu-item>
      </a-sub-menu>
    </template>
  </a-menu>
</a-layout-sider>
</template>

<script>
import { ref, defineComponent, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  ContainerOutlined,
  DashboardOutlined,
  ReadOutlined,
  SolutionOutlined,
  ClusterOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  SafetyOutlined,
  SettingOutlined,
  ToolOutlined,
} from '@ant-design/icons-vue';
import { matchPermission } from '@/utils/permission';

export default defineComponent({
  name: 'SiderComponent',
  components: {
    ContainerOutlined,
    DashboardOutlined,
    ReadOutlined,
    SolutionOutlined,
    ClusterOutlined,
    TrophyOutlined,
    AppstoreOutlined,
    SafetyOutlined,
    SettingOutlined,
    ToolOutlined,
  },
  props: {
    collapsed: {
      type: Boolean,
      default: false
    },
    selectedKeys: {
      type: Array,
      default: () => []
    },
    openKeys: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:collapsed', 'update:selectedKeys', 'update:openKeys', 'category-modal-show'],
  setup(props, { emit }) {
    const router = useRouter();
    const route = useRoute();

    // 图标映射
    const iconComponents = {
      DashboardOutlined,
      ContainerOutlined,
      ReadOutlined,
      SolutionOutlined,
      ClusterOutlined,
      TrophyOutlined,
      AppstoreOutlined,
      SafetyOutlined,
      SettingOutlined,
      ToolOutlined
    };

    // 菜单数据
    const menuItems = [
      {
        key: 'dashboard',
        title: '首页',
        icon: 'DashboardOutlined',
        path: '/home'
      },
      {
        key: 'learning',
        title: '学习中心',
        icon: 'ContainerOutlined',
        permissionCodes: ['course', 'train', 'map'],
        children: [
          {
            key: 'courseList',
            title: '课程列表',
            icon: 'ReadOutlined',
            path: '/course/list',
            permissionCodes: ['course']
          },
          {
            key: 'trainList',
            title: '培训列表',
            icon: 'SolutionOutlined',
            path: '/train/list',
            permissionCodes: ['train']
          },
          {
            key: 'mapList',
            title: '地图列表',
            icon: 'ClusterOutlined',
            path: '/map/list',
            permissionCodes: ['map']
          }
        ]
      },
      {
        key: 'operation',
        title: '运营中心',
        icon: 'ToolOutlined',
        permissionCodes: ['certificate', 'category:manage'],
        children: [
          {
            key: 'certificate',
            title: '证书管理',
            icon: 'TrophyOutlined',
            path: '/certificate/list',
            permissionCodes: ['certificate']
          },
          {
            key: 'category',
            title: '类目管理',
            icon: 'AppstoreOutlined',
            path: '/category/management',
            permissionCodes: ['category:manage']
          }
        ]
      },
      {
        key: 'system',
        title: '系统管理',
        icon: 'SettingOutlined',
        permissionCodes: ['permission'],
        children: [
          {
            key: 'role',
            title: '权限管理',
            icon: 'SafetyOutlined',
            path: '/role/management',
            permissionCodes: ['permission']
          }
        ]
      }
    ];

    // Create computed properties for two-way binding
    const localCollapsed = computed({
      get() { return props.collapsed; },
      set(value) { emit('update:collapsed', value); }
    });

    const localOpenKeys = computed({
      get() { return props.openKeys; },
      set(value) { emit('update:openKeys', value); }
    });

    const localSelectedKeys = computed({
      get() { return props.selectedKeys; },
      set(value) { emit('update:selectedKeys', value); }
    });

    // 页面跳转
    const navigateTo = (path) => {
      router.push(path);
    };

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      emit('category-modal-show');
    };

    return {
      menuItems,
      iconComponents,
      navigateTo,
      showCategoryModal,
      localOpenKeys,
      localSelectedKeys,
      localCollapsed,
      matchPermission,
    };
  }
});
</script>

<style scoped>
.sider {
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.08);
}
</style>
