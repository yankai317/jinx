<template>
  <div class="page-breadcrumb">
    <a-breadcrumb>
      <a-breadcrumb-item>
        首页</a-breadcrumb-item>
      <a-breadcrumb-item v-for="item in breadcrumbData" :key="item.key">
        <a href="javascript:void(0);" @click="routerTo(item)" v-if="item.path && !item.current">
          {{ item.title }}
        </a>
        <template v-else>{{ item.title }}</template>
      </a-breadcrumb-item>
    </a-breadcrumb>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter();

const props = defineProps({
  selectedKeys: {
    type: Array,
    default: () => []
  },
  openKeys: {
    type: Array,
    default: () => []
  }
})

/** 当前路径 */
const routePath = useRoute().path;

/** 面包屑数据 */
const breadcrumbData = computed(() => {
  const path = findPath(menuData, []);

  return path;
})

/**
 * 获取全路径
 * @param nodes 当前节点所有数据
 * @param path 已存储路径
 */
const findPath = (nodes, path) => {
  for (const node of nodes) {
    // 当前路径
    let currentPath = [];


    // 如果当前节点匹配目标key，返回路径
    if (routePath.includes(node.path)) {
      currentPath = [...path, { ...node, current: true, children: undefined }]
      return currentPath;
    }
    // 最后一条数据要打标数据
    currentPath = [...path, { ...node, children: undefined }];


    // 如果有子节点，递归查找
    if (!!node.children?.length) {
      const foundPath = findPath(node.children, currentPath);
      if (foundPath) {
        return foundPath;
      }
    }
  }
  return null;
};

/** 路由跳转 */
const routerTo = (node) => {
  console.log(node)
  router.replace(node.path);
}


/** 菜单数据 */
const menuData = [
  {
    key: 'learning',
    title: '学习中心',
    icon: 'container-outlined',
    children: [
      {
        key: 'courseList',
        title: '课程列表',
        icon: 'read-outlined',
        path: '/course/list',
        children: [
          {
            key: 'courseCreate',
            title: '创建课程',
            path: '/course/create',
            meta: { hideInMenu: true }
          },
          {
            key: 'courseDetail',
            title: '课程详情',
            path: '/course/detail',
            meta: { hideInMenu: true }
          },
          {
            key: 'courseEdit',
            title: '编辑课程',
            path: '/course/edit',
            meta: { hideInMenu: true }
          },
        ]
      },
      {
        key: 'trainList',
        title: '培训列表',
        icon: 'solution-outlined',
        path: '/train/list',
        children: [
          {
            key: 'trainCreate',
            title: '创建培训',
            path: '/train/create',
            meta: { hideInMenu: true }
          },
          {
            key: 'trainDetail',
            title: '培训详情',
            path: '/train/detail',
            meta: { hideInMenu: true }
          },
          {
            key: 'trainEdit',
            title: '编辑培训',
            path: '/train/edit',
            meta: { hideInMenu: true }
          },
        ]
      },
      {
        key: 'mapList',
        title: '地图列表',
        icon: 'cluster-outlined',
        path: '/map/list',
        children: [
          {
            key: 'mapCreate',
            title: '创建地图',
            path: '/map/create',
            meta: { hideInMenu: true }
          },
          {
            key: 'mapDetail',
            title: '地图详情',
            path: '/map/detail',
            meta: { hideInMenu: true }
          },
          {
            key: 'mapEdit',
            title: '编辑地图',
            path: '/map/edit',
            meta: { hideInMenu: true }
          },
        ]
      }
    ]
  },
  {
    key: 'operation',
    title: '运营中心',
    icon: 'tool-outlined',
    children: [
      {
        key: 'certificate',
        title: '证书管理',
        icon: 'trophy-outlined',
        path: '/certificate/list',
        children: [
          {
            key: 'certificateCreate',
            title: '创建证书',
            path: '/certificate/create',
            meta: { hideInMenu: true }
          },
          {
            key: 'certificateDetail',
            title: '证书详情',
            path: '/certificate/detail',
            meta: { hideInMenu: true }
          },
          {
            key: 'certificateEdit',
            title: '证书编辑',
            path: '/certificate/edit',
            meta: { hideInMenu: true }
          },
        ]
      },
      {
        key: 'category',
        title: '类目管理',
        icon: 'appstore-outlined',
        path: '/category/management'
      }
    ]
  },
  {
    key: 'system',
    title: '系统管理',
    icon: 'setting-outlined',
    children: [
      {
        key: 'role',
        title: '权限管理',
        icon: 'safety-outlined',
        path: '/role/management'
      }
    ]
  }
]
</script>

<style scoped>
.page-breadcrumb {}
</style>