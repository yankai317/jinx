import { createRouter, createWebHistory } from 'vue-router'
import Hello from '../views/Hello.vue'
import { fetchAndStorePermissions } from '@/utils/permission'
import { ddAuth } from '@/common/ddAuth'
import * as dd from 'dingtalk-jsapi'; // 此方式为整体加载，也可按需进行加载
const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/hello',
    name: 'Hello',
    component: Hello
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginPage.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/home/HomePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/home',
    name: 'TocHome',
    component: () => import('../views/toc/HomePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/home',
    name: 'TocHomeMobile',
    component: () => import('../views/toc/mobile/HomePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/learning/center',
    name: 'LearningCenter',
    component: () => import('../views/toc/learningCenterPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/learning/center',
    name: 'LearningCenterMobile',
    component: () => import('../views/toc/mobile/learningCenterPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/learning/tasks',
    name: 'LearningTasks',
    component: () => import('../views/toc/learningTasksPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/learning/tasks',
    name: 'LearningTasksMobile',
    component: () => import('../views/toc/mobile/learningTasksPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/course/detail/:id',
    name: 'TocCourseDetail',
    component: () => import('../views/toc/courseDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/course/:id',
    name: 'TocCourseDetailMobile',
    component: () => import('../views/toc/mobile/courseDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/train/detail/:id',
    name: 'TocTrainDetail',
    component: () => import('../views/toc/trainDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/train/:id',
    name: 'TocTrainDetailMobile',
    component: () => import('../views/toc/mobile/trainDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/map/stage/detail',
    name: 'MapStageDetail',
    component: () => import('../views/toc/mapStageDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/map/stage',
    name: 'MapStageDetailMobile',
    component: () => import('../views/toc/mobile/mapStageDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/map/detail/:id',
    name: 'MapDetail',
    component: () => import('../views/toc/mapDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/map/:id',
    name: 'MapDetailMobile',
    component: () => import('../views/toc/mobile/mapDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/personal/center',
    name: 'PersonalCenter',
    component: () => import('../views/toc/personalCenterPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/personal/center',
    name: 'PersonalCenterMobile',
    component: () => import('../views/toc/mobile/personalCenterPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/search',
    name: 'Search',
    component: () => import('../views/toc/searchPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/search',
    name: 'SearchMobile',
    component: () => import('../views/toc/mobile/searchPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/certificates',
    name: 'UserCertificates',
    component: () => import('../views/toc/CertificateListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/certificates',
    name: 'UserCertificatesMobile',
    component: () => import('../views/toc/mobile/CertificateListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/certificate/:id',
    name: 'CertificateDetail',
    component: () => import('../views/toc/certificateDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/certificate/:id',
    name: 'CertificateDetailMobile',
    component: () => import('../views/toc/mobile/certificateDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/rankings',
    name: 'Rankings',
    component: () => import('../views/toc/RankingsPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/rankings',
    name: 'RankingsMobile',
    component: () => import('../views/toc/mobile/rankingsPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/learning/records',
    name: 'LearningRecordList',
    component: () => import('../views/toc/learningRecordListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toc/mobile/learning/records',
    name: 'LearningRecordListMobile',
    component: () => import('../views/toc/mobile/learningRecordListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/course/list',
    name: 'CourseList',
    component: () => import('../views/course/CourseListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/train/list',
    name: 'TrainList',
    component: () => import('../views/train/TrainListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/train/detail/:id',
    name: 'TrainDetail',
    component: () => import('../views/train/TrainDetailPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/train/tracking/:id',
    name: 'TrainTracking',
    component: () => import('../views/train/TrainTrackingPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/map/list',
    name: 'LearningMapList',
    component: () => import('../views/map/LearningMapListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/certificate/list',
    name: 'CertificateList',
    component: () => import('../views/certificate/certificateListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/certificate/create',
    name: 'CertificateCreate',
    component: () => import('../views/certificate/certificateCreatePage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/certificate/detail/:id',
    name: 'CertificateDetail',
    component: () => import('../views/certificate/certificateDetailPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/certificate/edit/:id',
    name: 'CertificateEdit',
    component: () => import('../views/certificate/certificateEditPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/certificate/users/:id',
    name: 'CertificateUsers',
    component: () => import('../views/certificate/certificateUserPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/course/create',
    name: 'CourseCreate',
    component: () => import('../views/course/CourseCreatePage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/course/edit/:id',
    name: 'CourseEdit',
    component: () => import('../views/course/CourseEditPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/course/detail/:id',
    name: 'CourseDetail',
    component: () => import('../views/course/CourseDetailPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/course/analysis/:id',
    name: 'CourseAnalysis',
    component: () => import('../views/course/courseAnalysisPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/train/create',
    name: 'TrainCreate',
    component: () => import('../views/train/TrainCreatePage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/train/edit/:id',
    name: 'TrainEdit',
    component: () => import('../views/train/TrainEditPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/train/analysis/:id',
    name: 'TrainAnalysis',
    component: () => import('../views/train/TrainAnalysisPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/map/edit/:id',
    name: 'LearningMapEdit',
    component: () => import('../views/map/LearningMapEditPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/map/create',
    name: 'LearningMapCreate',
    component: () => import('../views/map/LearningMapCreatePage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/map/detail/:id',
    name: 'LearningMapDetail',
    component: () => import('../views/map/LearningMapDetailPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/map/tracking/:id',
    name: 'LearningMapTracking',
    component: () => import('../views/map/LearningMapTrackingPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  // 权限管理相关路由
  {
    path: '/role/management',
    name: 'RoleManagement',
    component: () => import('../views/role/RoleManagementPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/role/create',
    name: 'RoleCreate',
    component: () => import('../views/role/RoleCreatePage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/role/edit/:id',
    name: 'RoleEdit',
    component: () => import('../views/role/RoleEditPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/category/management',
    name: 'CategoryManagement',
    component: () => import('../views/category/CategoryManagePage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
  // 指派记录相关路由
  {
    path: '/assignment/records',
    name: 'AssignRecordsList',
    component: () => import('../views/assignment/AssignRecordsListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/assignment/detail',
    name: 'AssignRecordDetail',
    component: () => import('../views/assignment/AssignRecordDetailPage.vue'),
    meta: { requiresAuth: true, hideInMenu: true }
  },
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 路由守卫，检查用户是否已登录
router.beforeEach(async (to, from, next) => {
  const token = sessionStorage.getItem('token')

  console.log('token', token, from, to)
  if (to.matched.some(record => record.meta.requiresAuth)) {
    console.log('未登录', from, to)
    // 需要登录权限的路由
    if (!token) {
      // 未登录，跳转到登录页
      if(dd.env.platform === 'notInDingTalk') {
        next({ path: '/login' })
      } else {
        const authRes = await ddAuth.auth(to.query);
        try {
          await ddAuth.login(authRes)
          next()
        } catch (error) {
          console.error('登录异常:', error);
          next({ path: '/login' })
        }
      }
    } else {
      // 已登录，获取权限并允许访问
      await fetchAndStorePermissions()
      next()
    }
  } else {
    // 不需要登录权限的路由
    if (token && to.path === '/login') {
      // 已登录，访问登录页时跳转到首页
      next({ path: '/home' })
    } else {
      // 允许访问
      next()
    }
  }
})

export default router
